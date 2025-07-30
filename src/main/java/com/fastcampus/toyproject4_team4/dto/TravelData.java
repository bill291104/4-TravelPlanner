package com.fastcampus.toyproject4_team4.dto;

import java.util.List;

public record TravelData(
        List<Object> places,
        List<Object> restaurants,
        List<Object> accommodations,
        List<ChatMessage> conversationHistory
) {
}
