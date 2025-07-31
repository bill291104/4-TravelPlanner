package com.fastcampus.toyproject4_team4.googlePlace;

import com.google.gson.*;
import okhttp3.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Properties;

public class PlaceGoogleAPI {

    private static final OkHttpClient client = new OkHttpClient();

    public static void main(String[] args) {

        Properties properties = new Properties();

        try (InputStream input = new FileInputStream("src/main/resources/application.properties")) {
            properties.load(input);
        } catch (IOException e) {
            System.err.println("⚠️ 설정 파일 로드 실패: " + e.getMessage());
            return;
        }

        String query = "부산 커플 여행지";

        String API_KEY = properties.getProperty("google.api.key");
        String DB_URL = properties.getProperty("db.url") +
                "?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul";
        String DB_USER = properties.getProperty("db.user");
        String DB_PASSWORD = properties.getProperty("db.password");

        System.out.println("🔗 연결 정보:");
        System.out.println("   URL: " + DB_URL);
        System.out.println("   User: " + DB_USER);

        // 테이블 존재 여부 먼저 확인
        try (Connection testConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            Statement stmt = testConn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'place'");
            if (!rs.next()) {
                System.err.println("❌ place 테이블이 존재하지 않습니다!");
                System.err.println("💡 Spring Boot 애플리케이션을 먼저 실행하세요.");
                return;
            }
            System.out.println("✅ place 테이블 확인완료");
        } catch (Exception e) {
            System.err.println("❌ 데이터베이스 연결 실패: " + e.getMessage());
            return;
        }

        int maxPages = 5;
        int pageCount = 0;
        String nextPageToken = null;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String insertPlaceSQL = """
                INSERT INTO place (place_name, description, address, latitude, longitude, 
                                 created_at, updated_at, embedded_at, google_place_id)
                SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?
                FROM DUAL
                WHERE NOT EXISTS (
                    SELECT 1 FROM place WHERE google_place_id = ?
                )
            """;

            do {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                        "?query=" + encodedQuery +
                        "&language=ko" +
                        "&key=" + API_KEY +
                        (nextPageToken != null ? "&pagetoken=" + nextPageToken : "");

                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    System.err.println("API 요청 실패: " + response.code());
                    break;
                }

                JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
                JsonArray results = root.getAsJsonArray("results");

                for (JsonElement element : results) {
                    JsonObject place = element.getAsJsonObject();

                    String placeId = place.get("place_id").getAsString();

                    // 상세 정보 가져오기 (더 많은 필드 포함)
                    JsonObject details = fetchPlaceDetails(placeId, API_KEY);

                    // 기본 정보 추출
                    String name = getStringValue(details, "name");
                    String address = getStringValue(details, "formatted_address");

                    // 위도, 경도 추출
                    double lat = 0.0, lon = 0.0;
                    if (details.has("geometry") && details.getAsJsonObject("geometry").has("location")) {
                        JsonObject location = details.getAsJsonObject("geometry").getAsJsonObject("location");
                        lat = location.get("lat").getAsDouble();
                        lon = location.get("lng").getAsDouble();
                    }

                    // 설명 생성 (editorial_summary나 types 기반)
                    String description = generateDescription(details);

                    LocalDateTime now = LocalDateTime.now();

                    // 장소 저장
                    try (PreparedStatement stmt = conn.prepareStatement(insertPlaceSQL)) {
                        stmt.setString(1, name);
                        stmt.setString(2, description);
                        stmt.setString(3, address);
                        stmt.setBigDecimal(4, BigDecimal.valueOf(lat));
                        stmt.setBigDecimal(5, BigDecimal.valueOf(lon));
                        stmt.setObject(6, now);
                        stmt.setObject(7, now);
                        stmt.setObject(8, now);
                        stmt.setString(9, placeId);
                        stmt.setString(10, placeId); // WHERE 조건용

                        int rowsAffected = stmt.executeUpdate();
                        if (rowsAffected > 0) {
                            System.out.println("✅ 새 장소 저장: " + name);
                        } else {
                            System.out.println("⚠️ 이미 존재하는 장소: " + name);
                        }
                    }

                    // 리뷰 저장
                    Long placeDbId = getPlaceIdByPlaceId(conn, placeId);
                    if (placeDbId != null && details.has("reviews")) {
                        JsonArray reviews = details.getAsJsonArray("reviews");
                        for (JsonElement revElem : reviews) {
                            JsonObject rev = revElem.getAsJsonObject();
                            String comment = getStringValue(rev, "text");
                            double rating = rev.has("rating") ? rev.get("rating").getAsDouble() : 0.0;

                            if (comment != null && !comment.isBlank()) {
                                insertPlaceReview(conn, placeDbId, comment, rating, now);
                            }
                        }
                    }
                }

                pageCount++;
                if (root.has("next_page_token")) {
                    nextPageToken = root.get("next_page_token").getAsString();
                    System.out.println("다음 페이지 토큰 발견. 2초 대기 중...");
                    Thread.sleep(2000);
                } else {
                    nextPageToken = null;
                }

            } while (nextPageToken != null && pageCount < maxPages);

            System.out.println("✅ 총 " + pageCount + " 페이지 처리 완료. 저장 완료!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Google Places API에서 더 상세한 정보를 가져오는 메서드
     */
    private static JsonObject fetchPlaceDetails(String placeId, String apiKey) throws Exception {
        String fields = "name,formatted_address,geometry,formatted_phone_number," +
                "opening_hours,editorial_summary,types,reviews,rating,user_ratings_total," +
                "price_level,website,photos";

        String url = "https://maps.googleapis.com/maps/api/place/details/json" +
                "?place_id=" + placeId +
                "&fields=" + fields +
                "&language=ko" +
                "&key=" + apiKey;

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("상세 정보 API 실패: " + response.code());
            }
            JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
            return root.getAsJsonObject("result");
        }
    }

    /**
     * JSON에서 문자열 값을 안전하게 추출하는 헬퍼 메서드
     */
    private static String getStringValue(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }

    /**
     * 장소 설명을 생성하는 메서드
     */
    private static String generateDescription(JsonObject details) {
        StringBuilder description = new StringBuilder();

        // editorial_summary가 있으면 우선 사용
        if (details.has("editorial_summary")) {
            JsonObject summary = details.getAsJsonObject("editorial_summary");
            if (summary.has("overview")) {
                description.append(summary.get("overview").getAsString());
            }
        }

        // types 정보로 카테고리 추가
        if (details.has("types")) {
            JsonArray types = details.getAsJsonArray("types");
            StringBuilder categories = new StringBuilder();

            for (JsonElement type : types) {
                String typeStr = type.getAsString();
                String koreanType = translatePlaceType(typeStr);
                if (koreanType != null) {
                    if (categories.length() > 0) categories.append(", ");
                    categories.append(koreanType);
                }
            }

            if (categories.length() > 0) {
                if (description.length() > 0) description.append(" ");
                description.append("카테고리: ").append(categories.toString());
            }
        }

        // 평점 정보 추가
        if (details.has("rating")) {
            double rating = details.get("rating").getAsDouble();
            int totalRatings = details.has("user_ratings_total") ?
                    details.get("user_ratings_total").getAsInt() : 0;

            if (description.length() > 0) description.append(" ");
            description.append(String.format("Google 평점: %.1f/5.0 (%d개 리뷰)", rating, totalRatings));
        }

        return description.length() > 0 ? description.toString() : "Google Places에서 가져온 장소입니다.";
    }

    /**
     * Google Places type을 한국어로 번역 (관광지 맞춤)
     */
    private static String translatePlaceType(String type) {
        return switch (type) {
            // 🎯 관광명소 & 랜드마크
            case "tourist_attraction" -> "관광명소";
            case "historical_landmark" -> "역사적 명소";
            case "monument" -> "기념물";
            case "landmark" -> "랜드마크";
            case "cultural_landmark" -> "문화유적";
            case "historical_place" -> "역사적 장소";
            case "sculpture" -> "조각상";
            case "observation_deck" -> "전망대";
            case "visitor_center" -> "방문자센터";

            // 🏛️ 문화 & 예술
            case "museum" -> "박물관";
            case "art_gallery" -> "미술관";
            case "art_studio" -> "아트스튜디오";
            case "cultural_center" -> "문화센터";
            case "opera_house" -> "오페라하우스";
            case "concert_hall" -> "콘서트홀";
            case "performing_arts_theater" -> "공연장";
            case "auditorium" -> "강당";
            case "philharmonic_hall" -> "음악당";

            // 🏞️ 자연 & 공원
            case "park" -> "공원";
            case "national_park" -> "국립공원";
            case "state_park" -> "도립공원";
            case "botanical_garden" -> "식물원";
            case "garden" -> "정원";
            case "zoo" -> "동물원";
            case "aquarium" -> "수족관";
            case "wildlife_park" -> "야생동물공원";
            case "wildlife_refuge" -> "야생동물보호구역";
            case "beach" -> "해변";
            case "natural_feature" -> "자연명소";
            case "hiking_area" -> "등산지역";
            case "cycling_park" -> "자전거공원";
            case "dog_park" -> "반려견공원";
            case "picnic_ground" -> "피크닉장";

            // ⛪ 종교시설
            case "church" -> "교회";
            case "buddhist_temple" -> "불교사원";
            case "hindu_temple" -> "힌두사원";
            case "mosque" -> "모스크";
            case "synagogue" -> "회당";
            case "place_of_worship" -> "종교시설";

            // 🎢 오락 & 엔터테인먼트
            case "amusement_park" -> "놀이공원";
            case "amusement_center" -> "오락센터";
            case "water_park" -> "워터파크";
            case "ferris_wheel" -> "관람차";
            case "roller_coaster" -> "롤러코스터";
            case "casino" -> "카지노";
            case "movie_theater" -> "영화관";
            case "bowling_alley" -> "볼링장";
            case "karaoke" -> "노래방";
            case "video_arcade" -> "오락실";
            case "night_club" -> "나이트클럽";
            case "comedy_club" -> "코미디클럽";
            case "dance_hall" -> "댄스홀";
            case "internet_cafe" -> "PC방";

            // 🏛️ 교육 & 학술
            case "library" -> "도서관";
            case "university" -> "대학교";
            case "school" -> "학교";
            case "planetarium" -> "천문관";

            // ⚽ 스포츠 & 레크리에이션
            case "stadium" -> "경기장";
            case "arena" -> "아레나";
            case "sports_complex" -> "체육복합시설";
            case "playground" -> "놀이터";
            case "athletic_field" -> "운동장";
            case "golf_course" -> "골프장";
            case "ski_resort" -> "스키장";
            case "ice_skating_rink" -> "아이스링크";
            case "skateboard_park" -> "스케이트보드장";
            case "swimming_pool" -> "수영장";
            case "fishing_pond" -> "낚시터";
            case "marina" -> "마리나";
            case "adventure_sports_center" -> "모험스포츠센터";
            case "off_roading_area" -> "오프로드지역";

            // 🛍️ 쇼핑 & 상업지역
            case "shopping_mall" -> "쇼핑몰";
            case "market" -> "시장";
            case "plaza" -> "광장";

            // 🏢 기타 시설
            case "convention_center" -> "컨벤션센터";
            case "community_center" -> "커뮤니티센터";
            case "banquet_hall" -> "연회장";
            case "wedding_venue" -> "웨딩홀";
            case "event_venue" -> "이벤트장";
            case "amphitheatre" -> "원형극장";

            // 💆 웰니스
            case "spa" -> "스파";
            case "massage" -> "마사지";
            case "sauna" -> "사우나";
            case "wellness_center" -> "웰니스센터";

            // 📍 기본 카테고리
            case "point_of_interest" -> "관심지점";
            case "establishment" -> "시설";
            case "locality" -> "지역";
            case "sublocality" -> "세부지역";
            case "neighborhood" -> "동네";
            case "cemetery" -> "묘지";

            default -> null;
        };
    }

    /**
     * 관광지와 관련된 타입인지 확인하는 메서드
     */
    private static boolean isRelevantPlaceType(String type) {
        return switch (type) {
            case "tourist_attraction", "museum", "art_gallery", "park",
                 "church", "hindu_temple", "buddhist_temple", "mosque", "synagogue", "place_of_worship",
                 "historical_landmark", "monument", "cemetery", "library", "university", "school",
                 "zoo", "aquarium", "amusement_park", "stadium", "shopping_mall", "market",
                 "beach", "natural_feature", "locality", "sublocality", "neighborhood",
                 "point_of_interest", "establishment", "landmark", "cultural_center", "convention_center" -> true;
            default -> false;
        };
    }

    private static Long getPlaceIdByPlaceId(Connection conn, String placeId) throws SQLException {
        String sql = "SELECT place_id FROM place WHERE google_place_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, placeId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("place_id");
            }
        }
        return null;
    }

    private static void insertPlaceReview(Connection conn, Long placeId, String comment, double rating, LocalDateTime now) throws SQLException {
        // 중복 리뷰 체크 (같은 장소, 같은 코멘트)
        String checkSql = "SELECT COUNT(*) FROM place_review WHERE place_id = ? AND comment = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setLong(1, placeId);
            checkStmt.setString(2, comment);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("⚠️ 중복 리뷰 건너뜀: " + comment.substring(0, Math.min(30, comment.length())) + "...");
                return; // 이미 존재하는 리뷰
            }
        } catch (SQLException e) {
            // collation 에러가 발생하면 중복 체크 없이 바로 삽입 시도
            System.out.println("⚠️ 중복 체크 실패, 바로 삽입 시도: " + e.getMessage());
        }

        String sql = """
            INSERT INTO place_review (place_id, comment, rating, created_at, updated_at, embedded_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, placeId);
            stmt.setString(2, comment);
            stmt.setByte(3, (byte) Math.round(rating));
            stmt.setObject(4, now);
            stmt.setObject(5, now);
            stmt.setObject(6, now);
            stmt.executeUpdate();
            System.out.println("✅ 리뷰 저장 완료: " + comment.substring(0, Math.min(50, comment.length())) + "...");
        } catch (SQLException e) {
            // 중복 키 오류나 기타 에러는 로그만 출력하고 계속 진행
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("⚠️ 중복 리뷰 (키 제약): " + comment.substring(0, Math.min(30, comment.length())) + "...");
            } else {
                System.err.println("❌ 리뷰 저장 실패: " + e.getMessage());
                System.err.println("   리뷰 내용: " + comment.substring(0, Math.min(50, comment.length())) + "...");
            }
        }
    }
}