package com.fastcampus.toyproject4_team4.controller;

import com.fastcampus.toyproject4_team4.dto.*;
import com.fastcampus.toyproject4_team4.service.APIUtil;
import com.fastcampus.toyproject4_team4.service.TravelService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travel")
@RequiredArgsConstructor
public class TravelController {

        /**
     * 여행 계획 생성 API
     * POST /api/travel/plan
     * 완성된 TravelData를 Python 서비스로 전송하여 여행 계획 생성
     */
    @PostMapping("/plan")
    public ResponseEntity<TravelResponsedto> createTravelPlan(@RequestBody TravelData travelData) {
        TravelResponsedto response = APIUtil.sendPostRequest("http://localhost:8000/make_plan", travelData, new TypeReference<>() {
        });
        return ResponseEntity.ok(response);
    }

}
