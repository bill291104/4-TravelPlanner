package com.fastcampus.toyproject4_team4.dto;

import java.util.List;

public record TravelData(
        List<PlaceDetail> places,
        List<AccommodationDetail> accommodations,
        List<RestaurantDetail> restaurants,
        List<ChatMessage> conversationHistory
) {
}
