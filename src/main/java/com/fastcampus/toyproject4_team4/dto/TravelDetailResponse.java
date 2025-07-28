package com.fastcampus.toyproject4_team4.dto;

import java.util.List;

public record TravelDetailResponse<T>(
    String domain,
    List<T> details
) {
    // 제네릭을 사용하여 PlaceDto, RestaurantDto, AccommodationDto 모두 처리 가능
    // Python에서 받을 때 domain으로 타입 구분
}