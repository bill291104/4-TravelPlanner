package com.fastcampus.toyproject4_team4.googlePlace;

import com.fastcampus.toyproject4_team4.config.ConfigLoader;
import com.google.gson.*;
import okhttp3.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.sql.Connection;
import java.time.LocalDateTime;

public class RestaurantGoogleAPI {

    private static final OkHttpClient client = new OkHttpClient();

    public static void main(String[] args) {
        String query = "부산 술집";

        String API_KEY = ConfigLoader.get("google.api.key");
        String DB_URL = ConfigLoader.get("db.url");
        String DB_USER = ConfigLoader.get("db.user");
        String DB_PASSWORD = ConfigLoader.get("db.password");

        int maxPages = 5;
        int pageCount = 0;
        String nextPageToken = null;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String insertRestaurantSQL = """
                INSERT INTO restaurant (name, address, lat, lon, url, created_at, updated_at, embedded_at, google_place_id)
                SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?
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

                    String name = place.get("name").getAsString();
                    String address = place.has("formatted_address") ? place.get("formatted_address").getAsString() : null;
                    String placeId = place.get("place_id").getAsString();
                    String mapUrl = "https://www.google.com/maps/place/?q=place_id:" + placeId;

                    double lat = place.getAsJsonObject("geometry").getAsJsonObject("location").get("lat").getAsDouble();
                    double lon = place.getAsJsonObject("geometry").getAsJsonObject("location").get("lng").getAsDouble();

                    LocalDateTime now = LocalDateTime.now();

                    try (PreparedStatement stmt = conn.prepareStatement(insertRestaurantSQL)) {
                        stmt.setString(1, name);
                        stmt.setString(2, address);
                        stmt.setBigDecimal(3, BigDecimal.valueOf(lat));
                        stmt.setBigDecimal(4, BigDecimal.valueOf(lon));
                        stmt.setString(5, mapUrl);
                        stmt.setObject(6, now);
                        stmt.setObject(7, now);
                        stmt.setObject(8, now);
                        stmt.setString(9, placeId);
                        stmt.setString(10, placeId);
                        stmt.executeUpdate();
                    }

                    // 리뷰 저장
                    Long restaurantId = getRestaurantIdByPlaceId(conn, placeId);
                    if (restaurantId != null) {
                        try {
                            JsonObject details = fetchPlaceDetails(placeId, API_KEY);
                            if (details.has("reviews")) {
                                JsonArray reviews = details.getAsJsonArray("reviews");
                                for (JsonElement revElem : reviews) {
                                    JsonObject rev = revElem.getAsJsonObject();
                                    String comment = rev.has("text") ? rev.get("text").getAsString() : null;
                                    double rating = rev.has("rating") ? rev.get("rating").getAsDouble() : 0.0;

                                    if (comment != null && !comment.isBlank()) {
                                        insertReview(conn, restaurantId, comment, rating, now);
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            System.err.println("리뷰 저장 실패 (" + name + "): " + ex.getMessage());
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
        String sql = """
            INSERT INTO restaurant_review (restaurant_id, comment, rating, created_at, updated_at, embedded_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, restaurantId);
            stmt.setString(2, comment);
            stmt.setByte(3, (byte) rating);
            stmt.setObject(4, now);
            stmt.setObject(5, now);
            stmt.setObject(6, now);
            stmt.executeUpdate();
        }
    }

    private static JsonObject fetchPlaceDetails(String placeId, String apiKey) throws Exception {
        String url = "https://maps.googleapis.com/maps/api/place/details/json" +
                "?place_id=" + placeId +
                "&fields=reviews" +
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
}
