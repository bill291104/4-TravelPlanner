package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.entity.accomodation.Accommodation;
import com.fastcampus.toyproject4_team4.entity.place.Place;
import com.fastcampus.toyproject4_team4.entity.restaurant.Restaurant;

import java.util.List;

public record TravelData(
        List<Place> places,
        List<Accommodation> accommodations,
        List<Restaurant> restaurants,
        List<ChatMessage> conversationHistory
) {
}
