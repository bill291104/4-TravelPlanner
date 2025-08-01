package com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.dto.*;
import com.fastcampus.toyproject4_team4.entity.Prompt;
import com.fastcampus.toyproject4_team4.entity.Scenario;
import com.fastcampus.toyproject4_team4.repository.PromptRepository;
import com.fastcampus.toyproject4_team4.repository.ScenarioRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class PlanningService {
    @Value("${fastapi.url}")
    private String fastApiUrl;

    private final PromptRepository promptRepository;
    private final ScenarioRepository scenarioRepository;
    private final APIUtil apiUtil;

    public TravelPlanResponse createTravelPlan(TravelData travelData) {
        Scenario scenario = scenarioRepository.findByCode("PLN_001").orElseThrow(IllegalArgumentException::new);
        Prompt prompt = promptRepository.findByScenario(scenario).orElseThrow(IllegalArgumentException::new);
        FastAPIMakePlanRequest payload = new FastAPIMakePlanRequest(prompt.getPromptTemplate(),
                new ArrayList<>(), // userRequests
                travelData.conversationHistory(),
                travelData.places() == null ? new ArrayList<>() : travelData.places().stream().map(FastAPIPlaceDetail::from).toList(),
                travelData.restaurants() == null ? new ArrayList<>() : travelData.restaurants().stream().map(FastAPIRestaurantDetail::from).toList(),
                travelData.accommodations() == null ? new ArrayList<>() : travelData.accommodations().stream().map(FastAPIAccommodationDetail::from).toList()
        );
        TravelPlanResponse response = apiUtil.sendPostRequest(fastApiUrl + "/planning/make", payload, new TypeReference<>() {});
        return response;
    }
}
