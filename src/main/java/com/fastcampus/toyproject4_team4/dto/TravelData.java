package com.fastcampus.toyproject4_team4.dto;

import java.util.List;

public record TravelData(
        List<Object> places,
        List<Object> accommodations,
        List<Object> restaurants,
        List<ChatMessage> history
        ){
}