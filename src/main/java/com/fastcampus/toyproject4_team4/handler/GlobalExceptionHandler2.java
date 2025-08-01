package com.fastcampus.toyproject4_team4.handler;

import com.fastcampus.toyproject4_team4.exceptions.FastAPIExceptionUtil;
import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import com.fastcampus.toyproject4_team4.exceptions.embedding.*;
import com.fastcampus.toyproject4_team4.exceptions.internal.PythonInternalServerError;
import com.fastcampus.toyproject4_team4.exceptions.llm.*;
import com.fastcampus.toyproject4_team4.exceptions.common.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * "Handling 2: 처리할 수 없는 오류"를 담당하는 예외 핸들러
 * 사용자에게는 일반적인 사죄 문구를 제공하고, 개발자를 위한 상세 로그를 남김
 */
@Log4j2
@ControllerAdvice // 모든 Controller 또는 RestController에 적용
@RestController // JSON 응답을 반환하도록 설정
public class GlobalExceptionHandler2 {
    /**
     * Python 서비스에서 발생하는 Handling2 오류들을 한 번에 처리
     * 해당 예외들은 복구 불가능한 오류로 분류
     * 포함 되는 예외
     * LLMAPICommunicationException
     * PromptTemplateException
     * LLMServiceException
     * EmbeddingCreationException
     * EmbeddingDeletionException
     */
    @ExceptionHandler({
            PythonServiceException.class,
            InvalidInputException.class,
            ResourceNotFoundException.class,
            ServiceUnavailableException.class,
            EmbeddingCreationException.class,
            EmbeddingDeletionException.class,
            EmbeddingServiceException.class,
            PythonInternalServerError.class,
            AmbiguousClassificationException.class,
            LLMAPICommunicationException.class,
            LLMParsingFailedException.class,
            LLMResponseException.class,
            PromptTemplateException.class,
            UnsupportedDomainException.class,
            FastAPIExceptionUtil.ServerErrorException.class
    })
    public ResponseEntity<Object> handlePythonServerErrors(PythonServiceException e, WebRequest request){
        // 개발자를 위한 상세 로그 기록(필수)
        log.error("Python 서비스 오류 발생 ({}): {}" ,e.getErrorCode(),e.getMessage());


        // 사용자에게 보여줄 사죄 문구
        String userMessage;

        // 재질문을 하는 예외
        if (e instanceof ResourceNotFoundException) { // 유사도 검색 결과 비어있을 때
            userMessage = "죄송해요, 말씀하신 내용과 딱 맞는 정보를 찾기가 어렵네요. 좀 더 구체적으로 설명해주실 수 있을까요?";
        }
        else if (e instanceof ServiceUnavailableException) {
            userMessage = "Python 서버 구동 실패."; // 사용자가 볼 메세지 아님.
        }
        else if (e instanceof InvalidInputException) {
            userMessage = "임베딩 요청 형식 오류."; // 사용자가 볼 메세지 아님.
        }
        else if (e instanceof LLMParsingFailedException) {
            userMessage = "정성스러운 답변 감사합니다! 말씀해주신 내용도 흥미롭지만, 이번 여행 계획과는 다소 관련이 적은 것 같아요. 여행에 더 직접적인 도움이 될 수 있는 정보가 있다면 알려주시면 정말 감사하겠습니다 \uD83D\uDE0A";
        }
        else if (e instanceof UnsupportedDomainException) {
            userMessage = "정성스러운 답변 감사합니다! 말씀해주신 내용도 흥미롭지만, 이번 여행 계획과는 다소 관련이 적은 것 같아요. 여행에 더 직접적인 도움이 될 수 있는 정보가 있다면 알려주시면 정말 감사하겠습니다 \uD83D\uDE0A";
        }
        else if (e instanceof AmbiguousClassificationException) {
            userMessage = "정성스러운 답변 감사합니다! 말씀해주신 내용도 흥미롭지만, 이번 여행 계획과는 다소 관련이 적은 것 같아요. 여행에 더 직접적인 도움이 될 수 있는 정보가 있다면 알려주시면 정말 감사하겠습니다 \uD83D\uDE0A";
        }

        // 사죄 문구 예외
        else if (e instanceof EmbeddingServiceException) {
            userMessage = "죄송합니다. 임베딩 과정 중에 알 수 없는 오류가 발생했습니다.";
        }
        else if (e instanceof PromptTemplateException) {
            userMessage = "죄송합니다. 서비스 내부 설정에 문제가 발생했습니다. 관리자에게 문의해주세요.";
        }
        else if (e instanceof LLMAPICommunicationException) {
            userMessage = "죄송합니다. LLM 서비스와의 통신에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
        else if (e instanceof LLMResponseException) {
            userMessage = "죄송합니다. LLM 응답 처리 중 알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요.";
        }
        else if (e instanceof EmbeddingCreationException) {
            userMessage = "죄송합니다. 데이터 생성 중 알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요.";
        }
        else if (e instanceof EmbeddingDeletionException) {
            userMessage = "죄송합니다. 데이터 삭제 중 알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요.";
        }
        else {
            userMessage = "죄송합니다. 서비스 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }

        // HTTP 상태 코드는 예외 객체에 저장된 값을 사용
        HttpStatus responseStatus = HttpStatus.valueOf(e.getHttpStatus());
        Map<String, Object> body = createErrorBody(e.getHttpStatus(), responseStatus.getReasonPhrase(), e.getErrorCode(), userMessage, request);
        return new ResponseEntity<>(body, responseStatus);
    }


    /**
     * 자바 애플리케이션 자체에서 발생하는 예상치 못한 모든 예외를 처리하는 최종 핸들러
     * 이 핸들러는 명시적으로 처리되지 않은 모든 Exception을 잡는 역할
     */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllException(Exception e, WebRequest request){
        // 개발자를 위한 상세 로그 기록
        e.printStackTrace();
        log.error("예상치 못한 서버 오류 발생: {}", e.getMessage());


        // 사용자 사죄 문구
        String userMessage = "죄송합니다. 서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";

        Map<String, Object> body = createErrorBody(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", "UNEXPECTED_JAVA_ERROR", userMessage, request);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 오류 응답 본문을 생성하는 헬퍼 메서드
     * 모든 예외 핸들러에서 일관된 JSON 응답 형식을 유지
     */
    public Map<String, Object> createErrorBody(int status, String error, String code, String message, WebRequest request){
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now()); // 오류 발생 시각
        body.put("status", status); // HTTP 상태 코드 (숫자)
        body.put("error", error); // HTTP 상태 코드 설명
        body.put("code", code); // 애플리케이션 내부 오류 코드
        body.put("message", message); // 사용자에게 보여줄 메세지
        body.put("path", request.getDescription(false).replace("uri", "")); // 요청 경로
        return body;
    }
}
