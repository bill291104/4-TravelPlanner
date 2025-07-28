package com.fastcampus.toyproject4_team4.dto;

public record RestaurantDto(
    Integer pk,
    String name,
    Double lat,
    Double lon,
    Double expCost,
    String description,
    String cuisineType,
    String signatureMenu,
    String operatingHours
) {
    // record class는 자동으로 생성자, getter, equals, hashCode, toString 제공
    // Python RestaurantDetail 스키마와 동일한 구조로 데이터 호환성 확보
}