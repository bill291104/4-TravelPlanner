package com.fastcampus.toyproject4_team4.dto;
import com.fastcampus.toyproject4_team4.entity.accomodation.AccommodationReview;

import java.time.LocalDateTime;

public record AccommodationReviewDetail(
        Long id,
        Byte rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime embeddedAt
) {
    public static AccommodationReviewDetail from(AccommodationReview accommodationReview) {
        return new AccommodationReviewDetail(
                accommodationReview.getId(),
                accommodationReview.getRating(),
                accommodationReview.getComment(),
                accommodationReview.getCreatedAt(),
                accommodationReview.getUpdatedAt(),
                accommodationReview.getEmbeddedAt()
        );
    }
}
