package com.fastcampus.toyproject4_team4.exceptions.common;

/*
 * 요청한 리소스(문서, 데이터)를 찾을 수 없을 때 발생하는 예외
 * Python의 TravelDocumentNotFoundError, NoRelevantDocumentsFoundError (HTTP 404 NOT Found)에 대응
 */

import com.fastcampus.toyproject4_team4.exceptions.PythonServiceException;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends PythonServiceException {
    // Python에서 넘어오는 구체적인 errorCode를 함께 저장할 수 있도록 생성자 오버로드
    public ResourceNotFoundException(String message, String pythonErrorCode) {
        super(message, pythonErrorCode, HttpStatus.NOT_FOUND.value());
    }
}
