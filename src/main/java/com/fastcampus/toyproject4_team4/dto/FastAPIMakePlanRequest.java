package com.fastcampus.toyproject4_team4.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FastAPIMakePlanRequest(
        @JsonProperty("supervisor_prompt") String supervisorPrompt,
        @JsonProperty("user_requests") List<String> userRequest,
        @JsonProperty("conversation_history") List<ChatMessage> conversationHistory,
        @JsonProperty("candidate_places") List<PlaceDetail> candidatePlaces,
        @JsonProperty("candidate_restaurants") List<RestaurantDetail> candidateRestaurants,
        @JsonProperty("candidate_accommodations") List<AccommodationDetail> candidateAccommodations
) {
}
