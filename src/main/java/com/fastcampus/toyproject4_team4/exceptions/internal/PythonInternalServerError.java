package com.fastcampus.toyproject4_team4.exceptions.internal;

import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import org.springframework.http.HttpStatus;

public class PythonInternalServerError extends PythonServiceException {
    public PythonInternalServerError(String message, String pythonErrorCode) {
        super(message, pythonErrorCode, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
