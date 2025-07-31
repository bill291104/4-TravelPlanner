package com.fastcampus.toyproject4_team4.controller;

import com.fastcampus.toyproject4_team4.dto.*;
import com.fastcampus.toyproject4_team4.service.PlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PlanningController {
    private final PlanningService planningService;

    @PostMapping("/plan")
    public ResponseEntity<TravelPlanResponse> createTravelPlan(@RequestBody TravelData travelData) {
        TravelPlanResponse travelPlan = planningService.createTravelPlan(travelData);

        return ResponseEntity.ok(travelPlan);
    }
}
