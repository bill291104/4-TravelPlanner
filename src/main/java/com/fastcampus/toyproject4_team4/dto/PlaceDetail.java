package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.entity.place.Place;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PlaceDetail(
        Long id,
        String placeName,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime embeddedAt,
        TravelStyleDetail travelStyle,
        List<HashtagDetail> hashtags
) {
    public static PlaceDetail from(Place place) {
        return new PlaceDetail(
                place.getId(),
                place.getPlaceName(),
                place.getDescription(),
                place.getLatitude(),
                place.getLongitude(),
                place.getAddress(),
                place.getCreatedAt(),
                place.getUpdatedAt(),
                place.getEmbeddedAt(),
                TravelStyleDetail.from(place.getTravelStyle()),
                place.getHashtags().stream().map(HashtagDetail::from).toList()
        );
    }
}

