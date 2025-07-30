package com.fastcampus.toyproject4_team4.exceptions.common;

/*
* 서비스가 현재 요청을 처리할 수 없을 때 발생하는 예외
* Python의 DatabaseConnectionError (HTTP 503 Service Unavailable)에 해당
 */

import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends PythonServiceException {
    public ServiceUnavailableException(String message){
        super(message, "DatabaseConnectionError", HttpStatus.SERVICE_UNAVAILABLE.value());
    }
}
