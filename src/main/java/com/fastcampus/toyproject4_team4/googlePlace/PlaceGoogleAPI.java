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

public class PlaceGoogleAPI {

    private static final OkHttpClient client = new OkHttpClient();

    public static void main(String[] args) {
        String query = "부산에서 역사를 느낄 수 있는 곳";

        String API_KEY = ConfigLoader.get("google.api.key");
        String DB_URL = ConfigLoader.get("db.url");
        String DB_USER = ConfigLoader.get("db.user");
        String DB_PASSWORD = ConfigLoader.get("db.password");

        int maxPages = 3;
        int pageCount = 0;
        String nextPageToken = null;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String insertPlaceSQL = """
                INSERT INTO place (place_name, address, lat, lon, created_at, updated_at, embedded_at, google_place_id)
                SELECT ?, ?, ?, ?, ?, ?, ?, ?
                FROM DUAL
                WHERE NOT EXISTS (
                    SELECT 1 FROM place WHERE google_place_id = ?
                )
            """;

            String insertReviewSQL = """
                INSERT INTO place_review (rating, comment, created_at, updated_at, embedded_at, place_id)
                VALUES (?, ?, ?, ?, ?, ?)
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
                    double lat = place.getAsJsonObject("geometry").getAsJsonObject("location").get("lat").getAsDouble();
                    double lon = place.getAsJsonObject("geometry").getAsJsonObject("location").get("lng").getAsDouble();
                    LocalDateTime now = LocalDateTime.now();

                    // 장소 저장
                    long placeDbId = -1L;
                    try (PreparedStatement stmt = conn.prepareStatement(insertPlaceSQL, Statement.RETURN_GENERATED_KEYS)) {
                        stmt.setString(1, name);
                        stmt.setString(2, address);
                        stmt.setBigDecimal(3, BigDecimal.valueOf(lat));
                        stmt.setBigDecimal(4, BigDecimal.valueOf(lon));
                        stmt.setObject(5, now);
                        stmt.setObject(6, now);
                        stmt.setObject(7, now);
                        stmt.setString(8, placeId);
                        stmt.setString(9, placeId);
                        stmt.executeUpdate();

                        try (ResultSet keys = stmt.getGeneratedKeys()) {
                            if (keys.next()) placeDbId = keys.getLong(1);
                        }
                    }

                    if (placeDbId == -1L) {
                        try (PreparedStatement checkStmt = conn.prepareStatement("SELECT place_id FROM place WHERE google_place_id = ?")) {
                            checkStmt.setString(1, placeId);
                            try (ResultSet rs = checkStmt.executeQuery()) {
                                if (rs.next()) placeDbId = rs.getLong("place_id");
                            }
                        }
                    }

                    // 리뷰 저장 (details API 호출)
                    if (placeDbId != -1L) {
                        String detailUrl = "https://maps.googleapis.com/maps/api/place/details/json" +
                                "?place_id=" + placeId +
                                "&key=" + API_KEY +
                                "&language=ko" +
                                "&fields=review";

                        Request detailRequest = new Request.Builder().url(detailUrl).build();
                        Response detailResponse = client.newCall(detailRequest).execute();

                        if (!detailResponse.isSuccessful()) {
                            System.err.println("리뷰 API 요청 실패: " + detailResponse.code());
                            continue;
                        }

                        JsonObject detailRoot = JsonParser.parseString(detailResponse.body().string()).getAsJsonObject();
                        JsonObject resultObj = detailRoot.getAsJsonObject("result");

                        if (resultObj != null && resultObj.has("reviews")) {
                            JsonArray reviews = resultObj.getAsJsonArray("reviews");
                            for (JsonElement r : reviews) {
                                JsonObject review = r.getAsJsonObject();

                                try (PreparedStatement reviewStmt = conn.prepareStatement(insertReviewSQL)) {
                                    byte rating = (byte) review.get("rating").getAsInt();
                                    String comment = review.get("text").getAsString();

                                    reviewStmt.setByte(1, rating);
                                    reviewStmt.setString(2, comment);
                                    reviewStmt.setObject(3, now);
                                    reviewStmt.setObject(4, now);
                                    reviewStmt.setObject(5, now);
                                    reviewStmt.setLong(6, placeDbId);
                                    reviewStmt.executeUpdate();
                                } catch (Exception e) {
                                    System.err.println("리뷰 저장 실패 (" + name + "): " + e.getMessage());
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

            System.out.println("✅ Place 및 리뷰 저장 완료. 총 페이지: " + pageCount);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}