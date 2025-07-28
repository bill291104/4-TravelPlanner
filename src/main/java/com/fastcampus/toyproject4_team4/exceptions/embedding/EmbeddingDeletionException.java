package com.fastcampus.toyproject4_team4.exceptions.embedding;

import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import org.springframework.http.HttpStatus;

public class EmbeddingDeletionException extends PythonServiceException {
    public EmbeddingDeletionException(String message){
        super(message, "EmbeddingDeletionError", HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
