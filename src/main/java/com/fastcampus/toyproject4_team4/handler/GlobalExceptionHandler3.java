package com.fastcampus.toyproject4_team4.handler;

import com.fastcampus.toyproject4_team4.exceptions.embedding.EmbeddingCreationException;
import com.fastcampus.toyproject4_team4.exceptions.embedding.EmbeddingDeletionException;
import com.fastcampus.toyproject4_team4.exceptions.llm.LLMAPICommunicationException;
import com.fastcampus.toyproject4_team4.exceptions.llm.LLMResponseException;
import com.fastcampus.toyproject4_team4.exceptions.llm.PromptTemplateException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Handling 3: 처리할 수 없는 오류"를 담당하는 예외 핸들러
 * 주로 5xx 서버 오류 및 예상치 못한 시스템 내부 오류를 처리함
 * 사용자에게는 일반적인 사죄 문구를 제공하고, 개발자를 위한 상세 로그를 남김
 */

@ControllerAdvice // 모든 Controller 또는 RestController에 적용
@RestController // JSON 응답을 반환하도록 설정
public class GlobalExceptionHandler3 {
    /**
     * Python 서비스에서 발생하는 Handling3 오류들을 한 번에 처리
     */
//    @ExceptionHandler({
//            LLMAPICommunicationException.class
//            PromptTemplateException.class,
//            LLMResponseException.class, // LLMResponseException 에서는 LLMServiceException만 포함
//            EmbeddingCreationException.class,
//            EmbeddingDeletionException.class
//    })
//

}
