package com.fastcampus.toyproject4_team4.dto;

import java.math.BigDecimal;
import java.util.List;

public record FastAPIPlaceDetail(
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal exp_cost,
        String description,
        Long id,
        String address,
        String travel_style,
        List<String> hashtags
) {
    public static FastAPIPlaceDetail from(PlaceDetail placeDetail) {
        return new FastAPIPlaceDetail(
                placeDetail.placeName(),
                placeDetail.latitude(),
                placeDetail.longitude(),
                null,
                placeDetail.description(),
                placeDetail.id(),
                placeDetail.address(),
                placeDetail.travelStyle() == null ? "" : placeDetail.travelStyle().description(),
                placeDetail.hashtags().stream().map(HashtagDetail::content).toList()
        );
    }
}
