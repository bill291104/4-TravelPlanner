package com.fastcampus.toyproject4_team4.controller;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.dto.AnswerRequest;
import com.fastcampus.toyproject4_team4.dto.DetailResponse;
import com.fastcampus.toyproject4_team4.service.ExtractService;
import com.fastcampus.toyproject4_team4.service.RetrivalService;
import com.fastcampus.toyproject4_team4.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Log4j2
@RestController
@RequiredArgsConstructor
public class PlanningController {
    private final ExtractService extractService;
    private final VectorService vectorService;
    private final RetrivalService retrivalService;

    @PostMapping("/answer")
    public ResponseEntity<DetailResponse> handleAnswer(AnswerRequest request) {
        log.info("Handle Answer Request\nRequest: {}", request);

        // 도메인 추출
        Domains targetDomain = extractService.extractDomain(request.context());
        log.info("Target Domain: {}", targetDomain);
        // 키워드 추출
        List<String> keywords = extractService.extractKeywords(request.context(), targetDomain);
        log.info("Keywords: {}", keywords);
        // 유사도 검색
        List<Long> pks = vectorService.service(request.context(), targetDomain, keywords);
        log.info("PK Extract Successfully\nResult\npks: {}", pks);
        // 상세 데이터 조회
        List<?> details = retrivalService.getDetail(targetDomain, pks);
        log.info("Retrival Successfully\nResult\ndetails: {}", details);

        return ResponseEntity.ok(DetailResponse.from(targetDomain, details));
    }
}
