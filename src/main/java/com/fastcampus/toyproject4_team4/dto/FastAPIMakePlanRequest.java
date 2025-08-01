package com.fastcampus.toyproject4_team4.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public record FastAPIMakePlanRequest(
        @JsonProperty("supervisor_prompt") String supervisorPrompt,
        @JsonProperty("user_requests") List<String> userRequest,
        @JsonProperty("conversation_history") List<ChatMessage> conversationHistory,
        @JsonProperty("candidate_places") List<FastAPIPlaceDetail> candidatePlaces,
        @JsonProperty("candidate_restaurants") List<FastAPIRestaurantDetail> candidateRestaurants,
        @JsonProperty("candidate_accommodations") List<FastAPIAccommodationDetail> candidateAccommodations
) {
    public static FastAPIMakePlanRequest from(String supervisorPrompt, TravelData travelData) {
        return new FastAPIMakePlanRequest(
                supervisorPrompt,
                new ArrayList<>(),
                travelData.conversationHistory(),
                travelData.places().stream().map(FastAPIPlaceDetail::from).toList(),
                travelData.restaurants().stream().map(FastAPIRestaurantDetail::from).toList(),
                travelData.accommodations().stream().map(FastAPIAccommodationDetail::from).toList()
        );
    }
}