package com.fastcampus.toyproject4_team4.exceptions.llm;

import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import org.springframework.http.HttpStatus;

/*
 * LLM의 도메인 분류가 모호하거나 확신하기 어려울 때 발생하는 예외입니다.
 * Python의 AmbiguousClassificationError (HTTP 422 Unprocessable Entity)에 대응
 */
public class AmbiguousClassificationException extends PythonServiceException {
    public AmbiguousClassificationException(String message){
        super(message, "AmbiguousClassificationError", HttpStatus.UNPROCESSABLE_ENTITY.value());
    }
}
