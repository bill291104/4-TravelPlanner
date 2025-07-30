package com.fastcampus.toyproject4_team4.exceptions.llm;

import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import org.springframework.http.HttpStatus;

/*
 * LLM 프롬프트 템플릿의 형식이 유효하지 않거나 필수 변수가 누락되었을 때 발생
 * Python의 PromptTemplateError (HTTP 400 Bad Request)에 대응
 */

public class PromptTemplateException extends PythonServiceException {
    public PromptTemplateException(String message){
        super(message, "PromptTemplateError", HttpStatus.BAD_REQUEST.value());
    }
}
