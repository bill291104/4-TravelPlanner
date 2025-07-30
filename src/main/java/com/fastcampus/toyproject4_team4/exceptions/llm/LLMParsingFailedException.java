package com.fastcampus.toyproject4_team4.exceptions.llm;

import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import org.springframework.http.HttpStatus;

/*
 * LLM 응답을 파싱하는 데 실패했을 때 발생하는 오류입니다.
 * Python의 LLMParsingError (HTTP 500 Internal Server Error)에 대응합니다.
 */

public class LLMParsingFailedException extends PythonServiceException {
    public LLMParsingFailedException(String message){
        super(message, "LLMParsingError", HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
