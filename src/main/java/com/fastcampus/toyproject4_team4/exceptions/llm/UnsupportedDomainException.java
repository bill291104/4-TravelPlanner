package com.fastcampus.toyproject4_team4.exceptions.llm;

import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import org.springframework.http.HttpStatus;

/*
 * LLM이 분류한 도메인이 현재 비즈니스 로직에서 처리할 수 없을 때 발생하는 예외입니다.
 * Python의 UnsupportedDomainClassificationError (HTTP 422 Unprocessable Entity)에 대응
 */

public class UnsupportedDomainException extends PythonServiceException {
    public UnsupportedDomainException(String message){
        super(message, "UnsupportedDomainClassificationError", HttpStatus.UNPROCESSABLE_ENTITY.value());
    }
}
