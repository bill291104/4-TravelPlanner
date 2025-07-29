package com.fastcampus.toyproject4_team4.exceptions.llm;

/*
 * LLM이 유효하지 않거나 예상치 못한 응답을 반환했거나, LLM 서비스 전반에 문제가 발생했을 때의 예외
 * Python의 LLMResponseError, LLMServiceError (HTTP 500 Internal Server Error)에 대응
 */

import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import org.springframework.http.HttpStatus;

public class LLMResponseException extends PythonServiceException {
    // Python의 LLMResponseException 또는 LLMServiceError 중 어떤 것이 구체적으로 전달하기 위해 errorCode를 받음
    public LLMResponseException(String message, String pythonErrorCode) {
        super(message, pythonErrorCode, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
