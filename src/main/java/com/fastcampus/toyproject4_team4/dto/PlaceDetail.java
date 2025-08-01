package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.entity.place.Place;
import com.fasterxml.jackson.annotation.JsonFormat;

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
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updatedAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime embeddedAt,
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

