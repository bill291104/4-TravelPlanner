package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.entity.TravelStyle;

import java.time.LocalDateTime;

public record TravelStyleDetail(
        Long id,
        String name,
        String description,
        String recSeason,
        String ageGroup,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime embeddedAt
) {
    public static TravelStyleDetail from(TravelStyle travelStyle) {
        if (travelStyle == null) return null;
        return new TravelStyleDetail(
                travelStyle.getId(),
                travelStyle.getName(),
                travelStyle.getDescription(),
                travelStyle.getRecSeason(),
                travelStyle.getAgeGroup(),
                travelStyle.getCreatedAt(),
                travelStyle.getUpdatedAt(),
                travelStyle.getEmbeddedAt()
        );
    }
}
