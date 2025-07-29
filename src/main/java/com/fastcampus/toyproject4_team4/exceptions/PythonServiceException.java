package com.fastcampus.toyproject4_team4.exceptions;

// RuntimeException을 상속 받아 Unchecked Exception으로 만듦.
// 호출 쪽은 try-catch 문을 강제로 사용하지 않아도 됨.

import lombok.Getter;

@Getter
public class PythonServiceException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public PythonServiceException(String message, String errorCode, int httpStatus){
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public PythonServiceException(String message, String errorCode, int httpStatus, Throwable cause){
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}