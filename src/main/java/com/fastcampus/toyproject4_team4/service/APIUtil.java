package com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.exceptions.FastAPIExceptionUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Log4j2
public class APIUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(100))
            .build();

    public static <S, R> R sendPostRequest(String uri, S payload, TypeReference<R> responseTypeRef) {
        log.debug("Send Post Request\nPayload: {}", payload);
        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            FastAPIExceptionUtil.handleErrorResponse(response);
            return objectMapper.readValue(response.body(), responseTypeRef);
        } catch (IOException | InterruptedException e) {
            log.error("Send Post Request Failed");
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    static <S> void sendPostRequest(String uri, S payload) {
        log.debug("Send Post Request\nPayload: {}", payload);
        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            FastAPIExceptionUtil.handleErrorResponse(response);
        } catch (IOException | InterruptedException e) {
            log.error("Send Post Request Failed");
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    static void sendDeleteRequest(String uri) {
        log.debug("Send Delete Request");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Content-Type", "application/json")
                    .DELETE()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            FastAPIExceptionUtil.handleErrorResponse(response);
        } catch (IOException | InterruptedException e) {
            log.error("Send Delete Request Failed");
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}

