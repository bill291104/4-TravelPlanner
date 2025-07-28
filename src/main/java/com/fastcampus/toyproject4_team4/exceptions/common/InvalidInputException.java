package com.fastcampus.toyproject4_team4.exceptions.common;

import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import org.springframework.http.HttpStatus;

public class InvalidInputException extends PythonServiceException {
    public InvalidInputException(String message){
        super(message, "InvalidInputError", HttpStatus.BAD_REQUEST.value());
    }
}
