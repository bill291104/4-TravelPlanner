package com.fastcampus.toyproject4_team4.dto;

import java.util.List;

public record FastAPIMakePlanRequest(
        String supervisorPrompt,
        List<String> userRequest,
        List<ChatMessage> conversationHistory,
        List<PlaceDetail> candidatePlaces,
        List<RestaurantDetail> candidateRestaurants,
        List<AccommodationDetail> candidateAccommodations
) {
}
