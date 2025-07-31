package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.entity.place.PlaceReview;

import java.time.LocalDateTime;

public record PlaceReviewDetail(
        Long id,
        Byte rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime embeddedAt
) {
    public static PlaceReviewDetail from(PlaceReview placeReview) {
        return new PlaceReviewDetail(
                placeReview.getId(),
                placeReview.getRating(),
                placeReview.getComment(),
                placeReview.getCreatedAt(),
                placeReview.getUpdatedAt(),
                placeReview.getEmbeddedAt()
        );
    }
}
