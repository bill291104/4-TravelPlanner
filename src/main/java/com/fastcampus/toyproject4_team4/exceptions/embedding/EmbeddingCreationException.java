package com.fastcampus.toyproject4_team4.exceptions.embedding;

import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import org.springframework.http.HttpStatus;

public class EmbeddingCreationException extends PythonServiceException {
    public EmbeddingCreationException(String message){
        super(message, "EmbeddingCreationError", HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
