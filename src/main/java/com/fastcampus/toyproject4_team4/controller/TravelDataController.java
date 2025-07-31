package com.fastcampus.toyproject4_team4.controller;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.dto.AnswerRequest;
import com.fastcampus.toyproject4_team4.dto.ChatResponse;
import com.fastcampus.toyproject4_team4.dto.DetailResponse;
import com.fastcampus.toyproject4_team4.dto.TravelData;
import com.fastcampus.toyproject4_team4.service.ExtractService;
import com.fastcampus.toyproject4_team4.service.RetrivalService;
import com.fastcampus.toyproject4_team4.service.TravelDataService;
import com.fastcampus.toyproject4_team4.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RestController
@RequestMapping
@RequiredArgsConstructor
public class TravelDataController {
    private final ExtractService extractService;
    private final VectorService vectorService;
    private final RetrivalService retrivalService;
    private final TravelDataService travelDataService;

    @GetMapping("/init")
    public ResponseEntity<TravelData> getEmptyTravelData(){
        log.info("비어 있는 TravelData 를 생성 합니다.");
        TravelData td = travelDataService.createTravelData();
        log.info("생성 완료");
        return ResponseEntity.ok(td);
    }

    @PostMapping("/check")
    public ResponseEntity<ChatResponse> checkDomain(@RequestBody TravelData td){
        log.info("TravelData 에서 비어있는 도메인을 확인합니다...");
        Domains emptyDomain = travelDataService.checkEmptyDomain(td);

        ChatResponse response;
        if (emptyDomain == null) {
            log.info("모든 데이터 준비 완료");
            response = new ChatResponse("여행 계획을 세우기 위한 모든 준비를 마쳤습니다.", true);
        } else {
            log.info("비어있는 도메인: {}", emptyDomain.getName());
            response = travelDataService.getQuestion(emptyDomain);
        }
        System.out.println(td);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/answer")
    public ResponseEntity<DetailResponse> handleAnswer(@RequestBody AnswerRequest request) { //<<애너테이션추가 by 먕
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
