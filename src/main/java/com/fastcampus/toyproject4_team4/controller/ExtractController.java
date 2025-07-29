package com.fastcampus.toyproject4_team4.controller;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.dto.AnswerRequest;
import com.fastcampus.toyproject4_team4.service.ExtractService;
import com.fastcampus.toyproject4_team4.service.VectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        Domains targetDomain = extractService.extractDomain(request.context());
        List<String> keywords = extractService.extractKeywords(request.context(), targetDomain);

        List<Integer> pks = vectorService.service(request.context(), targetDomain, keywords);
        return ResponseEntity.ok(pks);
    }
}
