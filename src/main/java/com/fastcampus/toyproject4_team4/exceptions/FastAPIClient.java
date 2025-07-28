package com.fastcampus.toyproject4_team4.exceptions;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.exceptions.common.InvalidInputException;
import com.fastcampus.toyproject4_team4.exceptions.common.ResourceNotFoundException;
import com.fastcampus.toyproject4_team4.exceptions.common.ServiceUnavailableException;
import com.fastcampus.toyproject4_team4.exceptions.embedding.EmbeddingCreationException;
import com.fastcampus.toyproject4_team4.exceptions.embedding.EmbeddingDeletionException;
import com.fastcampus.toyproject4_team4.exceptions.embedding.EmbeddingServiceException;
import com.fastcampus.toyproject4_team4.exceptions.internal.PythonInternalServerError;
import com.fastcampus.toyproject4_team4.exceptions.llm.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Python 서비스에서 발생한 HTTP 오류 응답을 파싱하여
 * 적절한 자바 커스텀 예외 (PythonServiceException의 하위 클래스)를 생성하는 팩토리 클래스
 * 해당 클래스는 예외 매핑 로직을 중앙 집중화하고 재사용 가능하게 설계
 */

public class FastAPIClient {
    // DTO 및 Enum 정의


    // FastAPI가 에러 발생 시 발생하는 JSON 응답 구조에 맞는 Record를 정의
    public record ErrorResponse(boolean error, String name, String massage, Object details) {}


    // 클래스 멤버
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * HTTP 응답을 확인하고, 상태 코드가 2xx가 아닐 경우 적절한 커스텀 예외를 발생 시키는 팩토리 역할
     * 이 메서드는 FastAPI의 에러 응답 구조(ErrorResponse record)를 파싱하여 예외 메세지를 구성,
     *
     * @param response 서버로부터 받은 HttpResponse 객체
     * @throws PythonServiceException 에러 상태 코드일 경우 발생 (PythonServiceException의 하위 클래스)
     */
    private static void handleErrorResponse(HttpResponse<String> response) throws PythonServiceException {
        int statusCode = response.statusCode();
        String responseBody = response.body();

        // 성공적인 상태 코드 (200 ~ 299)일 경우, 아무 작업도 하지 않고 메서드 종료
        if(statusCode >= 200 && statusCode < 300){
            return;
        }

        // 에러 응답 본문을 ErrorResponse 객체로 파싱함
        // FastAPI의 에러 응답이 JSON 형식이 아닐 경우 JsonProcessingException이 발생 할 수 있음
        ErrorResponse error;
        try {
            error = objectMapper.readValue(responseBody, ErrorResponse.class);
        } catch (JsonProcessingException e) {
            // 에러 응답 본문이 예상된 JSON 형식이 아닐 경우 PythonInternalServerError를 던짐
            throw new PythonInternalServerError("Failed to parse error response body. Status: " + statusCode + ". Original error: " + e.getMessage(), "JSON_PARSE_ERROR_PYTHON_RESPONSE");
        }

        // 파싱된 ErrorResponse에서 에러 코드와 메세지를 추출
        String errorCode = error.name();
        String errorMessage = error.massage();

        // HTTP 상태 코드에 따라 분기하여 적절한 예외를 생성하고 던짐.
        if(statusCode >= 400 && statusCode < 500) {
            // 4xx 클라이언트 오류 매핑
            switch (errorCode) {
                case "InvalidInputError":
                    throw new InvalidInputException(errorMessage);
                case "PromptTemplateError":
                    throw new PromptTemplateException(errorMessage);
                case "TravelDocumentNotFoundError":
                case "NoRelevantDocumentsFoundError":
                    throw new ResourceNotFoundException(errorMessage, errorCode);
                case "UnsupportedDomainClassificationError":
                    throw new UnsupportedDomainException(errorMessage);
                case "AmbiguousClassificationError":
                    throw new AmbiguousClassificationException(errorMessage);
                default:
                    // 매핑되지 않은 4xx 오류는 일반 PythonServiceException으로 처리
                    throw new PythonServiceException(errorMessage, errorCode, statusCode);
            }
        } else if (statusCode >= 500 && statusCode < 600) {
            // 5xx 서버 오류 매핑
            if (statusCode == HttpStatus.SERVICE_UNAVAILABLE.value()){ // 503
                throw new ServiceUnavailableException(errorMessage);
            }
            switch (errorCode) {
                case "LLMAPIError":
                    throw new LLMAPICommunicationException(errorMessage);
                case "LLMParsingError":
                    throw new LLMParsingFailedException(errorMessage);
                case "LLMResponseError":
                case "LLMServiceError":
                    throw new LLMResponseException(errorMessage, errorCode);
                case "EmbeddingServiceError":
                    throw new EmbeddingServiceException(errorMessage);
                case "EmbeddingCreationError":
                    throw new EmbeddingCreationException(errorMessage);
                case "EmbeddingDeletionError":
                    throw new EmbeddingDeletionException(errorMessage);
                default:
                    // 매핑되지 않은 5xx 오류는 일반 PythonServiceError로 처리
                    throw new PythonServiceException(errorMessage, errorCode, statusCode);
            }
        } else {
            // 그 외 알 수 없는 오류 처리
            throw new PythonServiceException("Unknown error with status code: " + statusCode + " - " + errorMessage, errorCode, statusCode);
        }
    }


}