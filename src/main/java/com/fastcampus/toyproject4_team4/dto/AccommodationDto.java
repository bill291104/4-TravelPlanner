package com.fastcampus.toyproject4_team4.dto;

import java.util.List;

public record AccommodationDto(
    Integer pk,
    String name,
    Double lat,
    Double lon,
    Double expCost,
    String description,
    String accomType,
    String grade,
    List<String> amenities,
    String checkInOutTime,
    String bookingUrl
) {
    // record class는 자동으로 생성자, getter, equals, hashCode, toString 제공
    // Python AccommodationDetail 스키마와 동일한 구조로 데이터 호환성 확보
}