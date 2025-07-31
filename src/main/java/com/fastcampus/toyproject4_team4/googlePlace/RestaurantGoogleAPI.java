package com.fastcampus.toyproject4_team4.googlePlace;

import com.fastcampus.toyproject4_team4.config.ConfigLoader;
import com.google.gson.*;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestaurantGoogleAPI {

    private static final OkHttpClient client = new OkHttpClient();

    public static void main(String[] args) {

        Properties properties = new Properties();

        try (InputStream input = new FileInputStream("src/main/resources/application.properties")) {
            properties.load(input);
        } catch (IOException e) {
            System.err.println("⚠️ 설정 파일 로드 실패: " + e.getMessage());
            return;
        }

        String query = "부산 회 맛집";

        String API_KEY = properties.getProperty("google.api.key");
        String DB_URL = properties.getProperty("db.url") + "?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul";
        String DB_USER = properties.getProperty("db.user");
        String DB_PASSWORD = properties.getProperty("db.password");

        System.out.println("🔗 DB URL: " + DB_URL);
        System.out.println("📊 DB User: " + DB_USER);

        int maxPages = 5;
        int pageCount = 0;
        String nextPageToken = null;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

//            // 연결 설정 최적화
//            conn.setAutoCommit(false); // 트랜잭션 수동 관리
//            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            // 테이블 존재 확인
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "restaurant", null);
            if (!tables.next()) {
                System.err.println("❌ restaurant 테이블이 존재하지 않습니다!");
                System.err.println("💡 Spring Boot 애플리케이션을 먼저 실행해서 테이블을 생성하세요.");
                return;
            }
            System.out.println("✅ restaurant 테이블 확인됨");

            String insertRestaurantSQL = """
                INSERT INTO restaurant (name, description, address, latitude, longitude, op_time, tel, url, 
                                      created_at, updated_at, embedded_at, google_place_id)
                SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                FROM DUAL
                WHERE NOT EXISTS (
                    SELECT 1 FROM restaurant WHERE google_place_id = ?
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
                    String mapUrl = "https://www.google.com/maps/place/?q=place_id:" + placeId;

                    // 위도, 경도 추출
                    double lat = 0.0, lon = 0.0;
                    if (details.has("geometry") && details.getAsJsonObject("geometry").has("location")) {
                        JsonObject location = details.getAsJsonObject("geometry").getAsJsonObject("location");
                        lat = location.get("lat").getAsDouble();
                        lon = location.get("lng").getAsDouble();
                    }

                    // 설명 생성 (editorial_summary나 types 기반)
                    String description = generateDescription(details);

                    // 영업시간 추출
                    String opTime = extractOperatingHours(details);

                    // 전화번호 추출
                    String tel = getStringValue(details, "formatted_phone_number");

                    LocalDateTime now = LocalDateTime.now();

                    try (PreparedStatement stmt = conn.prepareStatement(insertRestaurantSQL)) {
                        stmt.setString(1, name);
                        stmt.setString(2, description);
                        stmt.setString(3, address);
                        stmt.setBigDecimal(4, BigDecimal.valueOf(lat));
                        stmt.setBigDecimal(5, BigDecimal.valueOf(lon));
                        stmt.setString(6, opTime);
                        stmt.setString(7, tel);
                        stmt.setString(8, mapUrl);
                        stmt.setObject(9, now);
                        stmt.setObject(10, now);
                        stmt.setObject(11, now);
                        stmt.setString(12, placeId);
                        stmt.setString(13, placeId); // WHERE 조건용

                        int rowsAffected = stmt.executeUpdate();
                        if (rowsAffected > 0) {
                            System.out.println("✅ 새 음식점 저장: " + name);
                        } else {
                            System.out.println("⚠️ 이미 존재하는 음식점: " + name);
                        }
                    }

                    // 리뷰 저장
                    Long restaurantId = getRestaurantIdByPlaceId(conn, placeId);
                    if (restaurantId != null && details.has("reviews")) {
                        JsonArray reviews = details.getAsJsonArray("reviews");
                        for (JsonElement revElem : reviews) {
                            JsonObject rev = revElem.getAsJsonObject();
                            String comment = getStringValue(rev, "text");
                            double rating = rev.has("rating") ? rev.get("rating").getAsDouble() : 0.0;

                            if (comment != null && !comment.isBlank()) {
                                insertReview(conn, restaurantId, comment, rating, now);
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
     * 음식점 설명을 생성하는 메서드
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
                String koreanType = translateType(typeStr);
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

        return description.length() > 0 ? description.toString() : "Google Places에서 가져온 음식점입니다.";
    }

    /**
     * Google Places type을 한국어로 번역 (확장된 음식점 타입들)
     */
    private static String translateType(String type) {
        return switch (type) {
            // 기본 음식점
            case "restaurant" -> "음식점";
            case "food" -> "음식";
            case "establishment" -> "시설";
            case "point_of_interest" -> "관심지점";

            // 배달/테이크아웃
            case "meal_takeaway" -> "테이크아웃";
            case "meal_delivery" -> "배달";

            // 카페 & 음료
            case "cafe" -> "카페";
            case "coffee_shop" -> "커피숍";
            case "tea_house" -> "찻집";
            case "juice_shop" -> "주스바";
            case "cat_cafe" -> "고양이카페";
            case "dog_cafe" -> "강아지카페";

            // 베이커리 & 디저트
            case "bakery" -> "베이커리";
            case "donut_shop" -> "도넛가게";
            case "ice_cream_shop" -> "아이스크림가게";
            case "chocolate_shop" -> "초콜릿가게";
            case "chocolate_factory" -> "초콜릿공장";
            case "dessert_restaurant" -> "디저트전문점";
            case "dessert_shop" -> "디저트가게";
            case "candy_store" -> "사탕가게";
            case "confectionery" -> "제과점";

            // 국가별 레스토랑
            case "korean_restaurant" -> "한식당";
            case "chinese_restaurant" -> "중식당";
            case "japanese_restaurant" -> "일식당";
            case "italian_restaurant" -> "이탈리안";
            case "french_restaurant" -> "프렌치";
            case "american_restaurant" -> "양식당";
            case "mexican_restaurant" -> "멕시칸";
            case "indian_restaurant" -> "인도음식";
            case "thai_restaurant" -> "태국음식";
            case "vietnamese_restaurant" -> "베트남음식";
            case "greek_restaurant" -> "그리스음식";
            case "spanish_restaurant" -> "스페인음식";
            case "turkish_restaurant" -> "터키음식";
            case "brazilian_restaurant" -> "브라질음식";
            case "lebanese_restaurant" -> "레바논음식";
            case "afghani_restaurant" -> "아프간음식";
            case "african_restaurant" -> "아프리카음식";
            case "indonesian_restaurant" -> "인도네시아음식";
            case "asian_restaurant" -> "아시아음식";
            case "mediterranean_restaurant" -> "지중해음식";
            case "middle_eastern_restaurant" -> "중동음식";

            // 전문 음식점
            case "pizza_restaurant" -> "피자";
            case "sushi_restaurant" -> "스시";
            case "ramen_restaurant" -> "라멘";
            case "seafood_restaurant" -> "해산물";
            case "barbecue_restaurant" -> "바베큐";
            case "steak_house" -> "스테이크하우스";
            case "hamburger_restaurant" -> "햄버거";
            case "fast_food_restaurant" -> "패스트푸드";
            case "fine_dining_restaurant" -> "파인다이닝";
            case "buffet_restaurant" -> "뷔페";
            case "vegan_restaurant" -> "비건음식";
            case "vegetarian_restaurant" -> "채식음식";

            // 식사 시간별
            case "breakfast_restaurant" -> "조식전문점";
            case "brunch_restaurant" -> "브런치";
            case "diner" -> "다이너";

            // 술집 & 바
            case "bar" -> "바";
            case "wine_bar" -> "와인바";
            case "pub" -> "펍";
            case "bar_and_grill" -> "바앤그릴";
            case "night_club" -> "나이트클럽";

            // 기타
            case "deli" -> "델리";
            case "sandwich_shop" -> "샌드위치가게";
            case "bagel_shop" -> "베이글가게";
            case "cafeteria" -> "카페테리아";
            case "food_court" -> "푸드코트";
            case "acai_shop" -> "아사이볼";

            default -> null;
        };
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
     * 영업시간 정보를 추출하는 메서드
     */
    private static String extractOperatingHours(JsonObject details) {
        if (!details.has("opening_hours")) {
            return null;
        }

        JsonObject openingHours = details.getAsJsonObject("opening_hours");

        // weekday_text가 있으면 사용 (더 상세함)
        if (openingHours.has("weekday_text")) {
            JsonArray weekdayText = openingHours.getAsJsonArray("weekday_text");
            StringBuilder hours = new StringBuilder();

            for (JsonElement day : weekdayText) {
                if (hours.length() > 0) hours.append("\n");
                hours.append(day.getAsString());
            }

            return hours.toString();
        }

        // periods 정보로 간단하게 구성
        if (openingHours.has("periods")) {
            JsonArray periods = openingHours.getAsJsonArray("periods");
            if (periods.size() > 0) {
                JsonObject firstPeriod = periods.get(0).getAsJsonObject();

                if (firstPeriod.has("open") && firstPeriod.has("close")) {
                    String openTime = firstPeriod.getAsJsonObject("open").get("time").getAsString();
                    String closeTime = firstPeriod.getAsJsonObject("close").get("time").getAsString();

                    // 시간 포맷팅 (예: "0900" -> "09:00")
                    openTime = formatTime(openTime);
                    closeTime = formatTime(closeTime);

                    return String.format("영업시간: %s - %s", openTime, closeTime);
                }
            }
        }

        return null;
    }

    /**
     * 시간을 포맷팅하는 헬퍼 메서드 (예: "0900" -> "09:00")
     */
    private static String formatTime(String time) {
        if (time.length() == 4) {
            return time.substring(0, 2) + ":" + time.substring(2);
        }
        return time;
    }

    private static Long getRestaurantIdByPlaceId(Connection conn, String placeId) throws SQLException {
        String sql = "SELECT restaurant_id FROM restaurant WHERE google_place_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, placeId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("restaurant_id");
            }
        }
        return null;
    }

    private static void insertReview(Connection conn, Long restaurantId, String comment, double rating, LocalDateTime now) throws SQLException {
        // collation 문제를 피하기 위해 BINARY 비교 사용
        String checkSql = "SELECT COUNT(*) FROM restaurant_review WHERE restaurant_id = ? AND BINARY comment = BINARY ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setLong(1, restaurantId);
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
            INSERT INTO restaurant_review (restaurant_id, comment, rating, created_at, updated_at, embedded_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, restaurantId);
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