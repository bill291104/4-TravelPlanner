package com.fastcampus.toyproject4_team4.exceptions;

import com.fastcampus.toyproject4_team4.exceptions.common.InvalidInputException;
import com.fastcampus.toyproject4_team4.exceptions.common.ResourceNotFoundException;
import com.fastcampus.toyproject4_team4.exceptions.common.ServiceUnavailableException;
import com.fastcampus.toyproject4_team4.exceptions.embedding.EmbeddingCreationException;
import com.fastcampus.toyproject4_team4.exceptions.embedding.EmbeddingDeletionException;
import com.fastcampus.toyproject4_team4.exceptions.embedding.EmbeddingServiceException;
import com.fastcampus.toyproject4_team4.exceptions.internal.PythonInternalServerError;
import com.fastcampus.toyproject4_team4.exceptions.llm.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;

/**
 * Python 서비스에서 발생한 HTTP 오류 응답을 파싱하여
 * 적절한 자바 커스텀 예외 (PythonServiceException의 하위 클래스)를 생성하는 팩토리 클래스
 * 해당 클래스는 예외 매핑 로직을 중앙 집중화하고 재사용 가능하게 설계
 */

public class PythonExceptionFactory {

    // ObjectMapper는 한 번만 생성하여 재사용함.
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * HttpClientErrorException (4xx 클라이언트 오류)를 파싱하여
     * 해당 오류에 매핑되는 PythonServiceException의 하위 클래스를 반환.
     *
     * @param e 발생한 HttpClientException
     * @return 매핑된 자바 커스텀 예외
     */
    public static PythonServiceException createClientException(HttpClientErrorException e) {
        String errorCode = "UNKNOWN_CLIENT_ERROR";
        String errorMessage = "클라이언트 요청 오류";

        try {
            JsonNode errorNode = objectMapper.readTree(e.getResponseBodyAsString());
            if (errorNode.has("name")){
                errorCode = errorNode.get("name").asText();
            }
            if (errorNode.has("message")){
                errorMessage = errorNode.get("message").asText();
            }
        }catch (IOException parseException){
            System.err.println("Error parsing Python client error response JSON: " + parseException.getMessage());
            errorMessage = "Python 서비스로부터 유효하지 않은 오류 응답 JSON을 받았습니다:" + e.getMessage();
        }

        switch (errorCode){
            case "InvalidInputError":
                return new InvalidInputException(errorMessage);
            case "PromptTemplateError":
                return new PromptTemplateException(errorMessage);
            case "TravelDocumentNotFoundError":
            case "NoRelevantDocumentsFoundError":
                return new ResourceNotFoundException(errorMessage, errorCode);
            case "UnsupportedDomainClassificationError":
                return new UnsupportedDomainException(errorMessage);
            case "AmbiguousClassificationError":
                return new AmbiguousClassificationException(errorMessage);
            // 여기에 다른 4xx 오류 매핑을 추가할 수 있음.
            default:
                return new PythonServiceException(errorMessage, errorCode, e.getStatusCode().value());
        }
    }

    /**
     * HttpServerErrorException (5xx 서버 오류)을 파싱하여
     * 해당 오류에 매핑되는 PythonServiceException의 하위 클래스를 반환합니다.
     *
     * @param e 발생한 HttpServerErrorException
     * @return 매핑된 자바 커스텀 예외
     */
    public static PythonServiceException createServerException(HttpServerErrorException e) {
        String errorCode = "UNKNOWN_SERVER_ERROR";
        String errorMessage = "서버 내부 오류";

        try {
            JsonNode errorNode = objectMapper.readTree(e.getResponseBodyAsString());
            if (errorNode.has("name")) {
                errorCode = errorNode.get("name").asText();
            }
            if (errorNode.has("message")) {
                errorMessage = errorNode.get("message").asText();
            }
        }catch (IOException parseException) {
            System.err.println("Error parsing Python server error response JSON:" + parseException.getMessage());
            errorMessage = "Python 서비스로부터 유효하지 않은 오류 응답 JSON을 받았습니다:" + e.getMessage();
        }

        // HTTP 상태 코드 503은 서비스 이용 불가로 매핑 (DatabaseConnectionError)
        if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
            return new ServiceUnavailableException(errorMessage);
        }

        // Python의 500 오류들을 자바 커스텀 예외로 매핑
        switch (errorCode){
            case "LLMAPIError":
                return new LLMAPICommunicationException(errorMessage);
            case "LLMParsingError":
                return new LLMParsingFailedException(errorMessage);
            case "LLMResponseError":
            case "LLMServiceError":
                return new LLMResponseException(errorMessage, errorCode);
            case "EmbeddingServiceError":
                return new EmbeddingServiceException(errorMessage);
            case "EmbeddingCreationError":
                return new EmbeddingCreationException(errorMessage);
            case "EmbeddingDeletionError":
                return new EmbeddingDeletionException(errorMessage);
            default:
                return new PythonInternalServerError(errorMessage, errorCode);
        }
    }
}