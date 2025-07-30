package com.fastcampus.toyproject4_team4.controller;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.dto.AnswerRequest;
import com.fastcampus.toyproject4_team4.service.ExtractService;
import com.fastcampus.toyproject4_team4.service.VectorService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Log4j2
@RestController
public class ExtractController {
    private final ExtractService extractService;
    private final VectorService vectorService;

    public ExtractController(ExtractService extractService, VectorService vectorService) {
        this.extractService = extractService;
        this.vectorService = vectorService;
    }

    @PostMapping("/answer")
    public ResponseEntity<List<Integer>> handleAnswer(AnswerRequest request) {
        log.info("Handle Answer Request\nRequest: {}", request);
        Domains targetDomain = extractService.extractDomain(request.context());
        log.info("Target Domain: {}", targetDomain);
        List<String> keywords = extractService.extractKeywords(request.context(), targetDomain);
        log.info("Keywords: {}", keywords);

        List<Integer> pks = vectorService.service(request.context(), targetDomain, keywords);
        log.info("PK Extract Successfully\nResult\npks: {}", pks);
        return ResponseEntity.ok(pks);
    }
}
