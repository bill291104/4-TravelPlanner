package com.fastcampus.toyproject4_team4.exceptions;

import com.fastcampus.toyproject4_team4.exceptions.common.*;
import com.fastcampus.toyproject4_team4.exceptions.embedding.*;
import com.fastcampus.toyproject4_team4.exceptions.llm.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;

/**
 * Python 서비스에서 발생한 HTTP 오류 응답을 파싱하여
 * 적절한 자바 커스텀 예외를 생성하는 팩토리 클래스
 * 해당 클래스는 예외 매핑 로직을 중앙 집중화하고 재사용 가능하게 설계
 */
@Service
public class FastAPIExceptionUtil {

    // FastAPI가 에러 발생 시 발생하는 JSON 응답 구조에 맞는 Record를 정의
    public record ErrorResponse(String name, String detail) {}
    
    // 클래스 멤버
    private static final ObjectMapper objectMapper = new ObjectMapper();


    // 커스텀 예외 정의 (FastAPIClient 내부 예외 계층 유지)
    // 이 예외들은 handleErrorResponse 메서드에서 직접 던져짐

    /**
     * API 클라이언트에서 발생하는 모든 커스텀 예외의 부모 클래스
     * RuntimeException을 상속받아 Unchecked Exception으로
     * 해당 클래스는 PythonServiceException과 별개로 FastAPIClient 자체의 최상위 예외로 사용
     */
    public static class ApiClientException extends RuntimeException {
        public ApiClientException(String message){
            super(message);
        }
    }

    /**
     * 잘못된 요청 값(4xx 에러)으로 인해 발생하는 예외
     * 클라이언트(요청을 보낸 측)의 문제
     */
    public static class InvalidInputException extends ApiClientException {
        public InvalidInputException(String message) {
            super(message);
        }
    }

    /**
     * 서버 측 문제(5xx 에러)로 인해 발생하는 예외
     * 호출 대상 서버(FastAPI)의 내부 문제
     */
    public static class ServerErrorException extends ApiClientException {
        public ServerErrorException(String message) {
            super(message);
        }
    }

    /**
     * HTTP 응답을 확인하고, 상태 코드가 2xx가 아닐 경우 적절한 커스텀 예외를 발생 시키는 팩토리 역할
     * 이 메서드는 FastAPI의 에러 응답 구조(ErrorResponse record)를 파싱하여 예외 메세지를 구성,
     *
     * @param response 서버로부터 받은 HttpResponse 객체
     */
    public static void handleErrorResponse(HttpResponse<String> response) throws ApiClientException, PythonServiceException {
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
            // 오류 응답 역직렬화 문제
            throw new ServerErrorException("Failed to parse error response body. Status: " + statusCode + ". Original error: " + e.getMessage());
        }

        // 파싱된 ErrorResponse에서 에러 코드와 메세지를 추출
        String errorCode = error.name();
        String errorMessage = error.detail();

        if (errorCode == null) {
            errorCode = "UnknownError";
        }
        if (errorMessage == null) {
            errorMessage = "No error message provided";
        }

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
                    // 매핑되지 않은 4xx 오류는 일반 InvalidInputException으로 처리
                    throw new InvalidInputException("클라이언트 요청 오류 (" + errorCode + "): " + errorMessage);
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
                    // 매핑되지 않은 5xx 오류는 일반 ServerErrorException로 처리
                    throw new ServerErrorException("서버 내부 오류 (" + errorCode + "): " + errorMessage);
            }
        } else {
            // 그 외 알 수 없는 오류 처리
            throw new ApiClientException("Unknown error with status code: " + statusCode + " - " + errorMessage);
        }
    }


}