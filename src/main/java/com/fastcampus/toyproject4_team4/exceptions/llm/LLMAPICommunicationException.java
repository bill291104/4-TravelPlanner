package com.fastcampus.toyproject4_team4.exceptions.llm;

import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import org.springframework.http.HttpStatus;

/*
 * LLM API 호출 중 발생하는 오류 (예: 키 문제, 인증 실패, Rate Limit 초과, 모델 오류)입니다.
 * Python의 LLMAPIError (HTTP 500 Internal Server Error)에 대응
 */

public class LLMAPICommunicationException extends PythonServiceException {
    public LLMAPICommunicationException(String message){
        super(message, "LLMAPIError", HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
