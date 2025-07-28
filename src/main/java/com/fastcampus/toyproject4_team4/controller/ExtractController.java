package com.fastcampus.toyproject4_team4.controller;

import com.fastcampus.toyproject4_team4.dto.AnswerRequest;
import com.fastcampus.toyproject4_team4.service.ExtractService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ExtractController {
    private final ExtractService extractService;

    public ExtractController(ExtractService extractService) {
        this.extractService = extractService;
    }

    @PostMapping("/answer")
    public ResponseEntity<List<String>> handleAnswer(AnswerRequest request) {
        List<String> response = extractService.service(request.context());
        return ResponseEntity.ok(response);
    }
}
