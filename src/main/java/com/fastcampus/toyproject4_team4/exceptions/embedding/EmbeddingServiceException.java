package com.fastcampus.toyproject4_team4.exceptions.embedding;

import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import org.springframework.http.HttpStatus;

public class EmbeddingServiceException extends PythonServiceException {
    public EmbeddingServiceException(String message){
        super(message, "EmbeddingServiceError", HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
