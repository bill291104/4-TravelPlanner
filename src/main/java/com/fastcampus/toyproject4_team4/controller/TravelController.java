package com.fastcampus.toyproject4_team4.controller;

import com.fastcampus.toyproject4_team4.dto.*;
import com.fastcampus.toyproject4_team4.service.APIUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TravelController {
    @Value("${fastapi.url}")
    private String fastApiUrl;

    @PostMapping("/plan")
    public ResponseEntity<TravelPlanResponse> createTravelPlan(@RequestBody TravelData travelData) {
        TravelPlanResponse response = APIUtil.sendPostRequest(fastApiUrl + "/make_plan", travelData, new TypeReference<>() {});
        return ResponseEntity.ok(response);
    }
}
