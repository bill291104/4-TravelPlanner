package com.fastcampus.toyproject4_team4.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.fastcampus.toyproject4_team4.exceptions.FastAPIExceptionUtil;
import com.fastcampus.toyproject4_team4.exceptions.FastAPIExceptionUtil.ApiClientException;
import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class APIUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(100))
            .build();

    static <S, R> R sendPostRequest(String uri, S payload, TypeReference<R> responseTypeRef) {
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
        } catch (JsonProcessingException e) {
            // 요청 본문 직렬화 또는 성공 응답 본문 역직렬화 도중 발생
            log.error(e.getMessage());
            throw new FastAPIExceptionUtil.ServerErrorException("JSON 처리 중 오류 발생: " + e.getMessage()); // ✨ FastAPIExceptionUtil의 ServerErrorException 사용
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            throw new FastAPIExceptionUtil.ServerErrorException("Python 서비스와 통신 중 오류 발생: " + e.getMessage()); // ✨ FastAPIExceptionUtil의 ServerErrorException 사용
        }
    }
}
