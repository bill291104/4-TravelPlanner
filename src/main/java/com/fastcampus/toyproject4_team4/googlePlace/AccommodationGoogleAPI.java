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
import java.time.LocalTime;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;

public class AccommodationGoogleAPI {

    private static final OkHttpClient client = new OkHttpClient();

    public static void main(String[] args) {

        Properties properties = new Properties();

        try (InputStream input = new FileInputStream("src/main/resources/application.properties")) {
            properties.load(input);
        } catch (IOException e) {
            System.err.println("⚠️ 설정 파일 로드 실패: " + e.getMessage());
            return;
        }

        String query = "부산 펜션";

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
            ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'accommodation'");
            if (!rs.next()) {
                System.err.println("❌ accommodation 테이블이 존재하지 않습니다!");
                System.err.println("💡 Spring Boot 애플리케이션을 먼저 실행하세요.");
                return;
            }
            System.out.println("✅ accommodation 테이블 확인완료");
        } catch (Exception e) {
            System.err.println("❌ 데이터베이스 연결 실패: " + e.getMessage());
            return;
        }

        int maxPages = 5;
        int pageCount = 0;
        String nextPageToken = null;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String insertAccomSQL = """
                INSERT INTO accommodation (name, type, description, address, latitude, longitude, 
                                         avg_rating, url, created_at, updated_at, embedded_at, google_place_id)
                SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                FROM DUAL
                WHERE NOT EXISTS (
                    SELECT 1 FROM accommodation WHERE google_place_id = ?
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

                    // 상세 정보 가져오기
                    JsonObject details = fetchAccommodationDetails(placeId, API_KEY);

                    // 기본 정보 추출
                    String name = getStringValue(details, "name");
                    String address = getStringValue(details, "formatted_address");
                    String mapUrl = "https://www.google.com/maps/place/?q=place_id:" + placeId;

                    // 위도, 경도 추출
                    double lat = 0.0, lon = 0.0;
                    if (details.has("geometry") && details.getAsJsonObject("geometry").has("location")) {
                        JsonObject location = details.getAsJsonObject("geometry").getAsJsonObject("location");
                        lat = location.get("lat").getAsDouble();
                        lon = location.get("lng").getAsDouble();
                    }

                    // 숙소 타입 추출
                    String type = extractAccommodationType(details);

                    // 설명 생성
                    String description = generateAccommodationDescription(details);

                    // 평점 추출
                    BigDecimal avgRating = null;
                    if (details.has("rating")) {
                        avgRating = BigDecimal.valueOf(details.get("rating").getAsDouble());
                    }

                    LocalDateTime now = LocalDateTime.now();

                    // 숙소 저장
                    Long accomDbId = null;
                    try (PreparedStatement stmt = conn.prepareStatement(insertAccomSQL)) {
                        stmt.setString(1, name);
                        stmt.setString(2, type);
                        stmt.setString(3, description);
                        stmt.setString(4, address);
                        stmt.setBigDecimal(5, BigDecimal.valueOf(lat));
                        stmt.setBigDecimal(6, BigDecimal.valueOf(lon));
                        stmt.setBigDecimal(7, avgRating);
                        stmt.setString(8, mapUrl);
                        stmt.setObject(9, now);
                        stmt.setObject(10, now);
                        stmt.setObject(11, now);
                        stmt.setString(12, placeId);
                        stmt.setString(13, placeId); // WHERE 조건용

                        int rowsAffected = stmt.executeUpdate();
                        if (rowsAffected > 0) {
                            System.out.println("✅ 새 숙소 저장: " + name);
                        } else {
                            System.out.println("⚠️ 이미 존재하는 숙소: " + name);
                        }
                    }

                    // 저장된 숙소 ID 가져오기
                    accomDbId = getAccommodationIdByPlaceId(conn, placeId);

                    if (accomDbId != null) {
                        // 편의시설 저장
                        saveAmenities(conn, accomDbId, details);

                        // 리뷰 저장
                        if (details.has("reviews")) {
                            JsonArray reviews = details.getAsJsonArray("reviews");
                            for (JsonElement revElem : reviews) {
                                JsonObject rev = revElem.getAsJsonObject();
                                String comment = getStringValue(rev, "text");
                                double rating = rev.has("rating") ? rev.get("rating").getAsDouble() : 0.0;

                                if (comment != null && !comment.isBlank()) {
                                    insertAccommodationReview(conn, accomDbId, comment, rating, now);
                                }
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
     * Google Places API에서 숙소 상세 정보를 가져오는 메서드
     */
    private static JsonObject fetchAccommodationDetails(String placeId, String apiKey) throws Exception {
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
     * 숙소 타입을 추출하는 메서드
     */
    private static String extractAccommodationType(JsonObject details) {
        if (details.has("types")) {
            JsonArray types = details.getAsJsonArray("types");
            for (JsonElement type : types) {
                String typeStr = type.getAsString();
                String koreanType = translateAccommodationType(typeStr);
                if (koreanType != null) {
                    return koreanType;
                }
            }
        }
        return "숙소"; // 기본값
    }

    /**
     * 숙소 타입을 한국어로 번역 (Google Places API 전체 숙소 타입 포함)
     */
    private static String translateAccommodationType(String type) {
        return switch (type) {
            // 기본 숙박시설
            case "lodging" -> "숙박시설";
            case "hotel" -> "호텔";
            case "motel" -> "모텔";
            case "resort_hotel" -> "리조트호텔";
            case "extended_stay_hotel" -> "장기체류호텔";

            // 게스트하우스 & 민박
            case "guest_house" -> "게스트하우스";
            case "bed_and_breakfast" -> "민박";
            case "private_guest_room" -> "개인게스트룸";
            case "inn" -> "여관";
            case "japanese_inn" -> "일본식여관";
            case "budget_japanese_inn" -> "저가일본식여관";

            // 호스텔 & 저가숙소
            case "hostel" -> "호스텔";

            // 캠핑 & 아웃도어
            case "campground" -> "캠핑장";
            case "camping_cabin" -> "캠핑캐빈";
            case "rv_park" -> "RV파크";
            case "mobile_home_park" -> "모바일홈파크";

            // 독채 & 별장
            case "cottage" -> "코티지";
            case "farmstay" -> "농장체험숙소";

            // 기타 타입들
            case "apartment" -> "아파트";
            case "vacation_rental" -> "휴가용임대";

            default -> null;
        };
    }

    /**
     * 숙소 설명을 생성하는 메서드
     */
    private static String generateAccommodationDescription(JsonObject details) {
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
                String koreanType = translateAccommodationType(typeStr);
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

        // 가격대 정보 추가
        if (details.has("price_level")) {
            int priceLevel = details.get("price_level").getAsInt();
            String priceDesc = getPriceDescription(priceLevel);
            if (description.length() > 0) description.append(" ");
            description.append("가격대: ").append(priceDesc);
        }

        return description.length() > 0 ? description.toString() : "Google Places에서 가져온 숙박시설입니다.";
    }

    /**
     * 가격대를 설명으로 변환
     */
    private static String getPriceDescription(int priceLevel) {
        return switch (priceLevel) {
            case 0 -> "무료";
            case 1 -> "저렴함 ($)";
            case 2 -> "보통 ($$)";
            case 3 -> "비쌈 ($$$)";
            case 4 -> "매우 비쌈 ($$$$)";
            default -> "정보없음";
        };
    }

    /**
     * 편의시설 저장 메서드
     */
    private static void saveAmenities(Connection conn, Long accommodationId, JsonObject details) throws SQLException {
        Set<String> amenities = extractAmenities(details);

        for (String amenityName : amenities) {
            // 편의시설이 존재하는지 확인하고 없으면 생성
            Long amenityId = getOrCreateAmenity(conn, amenityName);

            // 숙소-편의시설 연결
            linkAccommodationAmenity(conn, accommodationId, amenityId);
        }
    }

    /**
     * 편의시설 정보를 추출하는 메서드
     */
    private static Set<String> extractAmenities(JsonObject details) {
        Set<String> amenities = new HashSet<>();

        // types에서 편의시설 관련 정보 추출
        if (details.has("types")) {
            JsonArray types = details.getAsJsonArray("types");
            for (JsonElement type : types) {
                String typeStr = type.getAsString();
                String amenity = translateToAmenity(typeStr);
                if (amenity != null) {
                    amenities.add(amenity);
                }
            }
        }

        // 기본 편의시설들 추가 (숙소라면 보통 있는 것들)
        amenities.add("Wi-Fi");
        amenities.add("주차장");

        return amenities;
    }

    /**
     * Google Places type을 편의시설로 번역
     */
    private static String translateToAmenity(String type) {
        return switch (type) {
            case "spa" -> "스파";
            case "gym" -> "피트니스센터";
            case "swimming_pool" -> "수영장";
            case "restaurant" -> "레스토랑";
            case "bar" -> "바/라운지";
            case "parking" -> "주차장";
            case "wifi" -> "Wi-Fi";
            case "air_conditioning" -> "에어컨";
            case "laundry_service" -> "세탁서비스";
            case "room_service" -> "룸서비스";
            case "business_center" -> "비즈니스센터";
            case "conference_room" -> "회의실";
            case "elevator" -> "엘리베이터";
            case "wheelchair_accessible" -> "휠체어 접근 가능";
            default -> null;
        };
    }

    /**
     * 편의시설을 가져오거나 생성하는 메서드
     */
    private static Long getOrCreateAmenity(Connection conn, String amenityName) throws SQLException {
        // 먼저 존재하는지 확인
        String selectSql = "SELECT amenity_id FROM amenity WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, amenityName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("amenity_id");
            }
        }

        // 존재하지 않으면 생성
        String insertSql = "INSERT INTO amenity (name, description, is_free) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, amenityName);
            stmt.setString(2, amenityName + " 편의시설");
            stmt.setByte(3, (byte) 1); // 기본적으로 무료로 설정
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                System.out.println("✅ 새 편의시설 생성: " + amenityName);
                return keys.getLong(1);
            }
        }

        return null;
    }

    /**
     * 숙소와 편의시설을 연결하는 메서드
     */
    private static void linkAccommodationAmenity(Connection conn, Long accommodationId, Long amenityId) throws SQLException {
        // 이미 연결되어 있는지 확인
        String checkSql = "SELECT COUNT(*) FROM accommodation_amenity WHERE accommodation_id = ? AND amenity_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setLong(1, accommodationId);
            stmt.setLong(2, amenityId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return; // 이미 연결됨
            }
        }

        // 연결 생성
        String insertSql = "INSERT INTO accommodation_amenity (accommodation_id, amenity_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setLong(1, accommodationId);
            stmt.setLong(2, amenityId);
            stmt.executeUpdate();
        }
    }

    private static Long getAccommodationIdByPlaceId(Connection conn, String placeId) throws SQLException {
        String sql = "SELECT accommodation_id FROM accommodation WHERE google_place_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, placeId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("accommodation_id");
            }
        }
        return null;
    }

    private static void insertAccommodationReview(Connection conn, Long accommodationId, String comment, double rating, LocalDateTime now) throws SQLException {
        // 중복 리뷰 체크
        String checkSql = "SELECT COUNT(*) FROM accommodation_review WHERE accommodation_id = ? AND comment = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setLong(1, accommodationId);
            checkStmt.setString(2, comment);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("⚠️ 중복 리뷰 건너뜀: " + comment.substring(0, Math.min(30, comment.length())) + "...");
                return;
            }
        } catch (SQLException e) {
            System.out.println("⚠️ 중복 체크 실패, 바로 삽입 시도: " + e.getMessage());
        }

        String sql = """
            INSERT INTO accommodation_review (accommodation_id, comment, rating, created_at, updated_at, embedded_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, accommodationId);
            stmt.setString(2, comment);
            stmt.setByte(3, (byte) Math.round(rating));
            stmt.setObject(4, now);
            stmt.setObject(5, now);
            stmt.setObject(6, now);
            stmt.executeUpdate();
            System.out.println("✅ 리뷰 저장 완료: " + comment.substring(0, Math.min(50, comment.length())) + "...");
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("⚠️ 중복 리뷰 (키 제약): " + comment.substring(0, Math.min(30, comment.length())) + "...");
            } else {
                System.err.println("❌ 리뷰 저장 실패: " + e.getMessage());
            }
        }
    }
}