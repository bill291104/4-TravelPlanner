package com.fastcampus.toyproject4_team4.dto;
import com.fastcampus.toyproject4_team4.entity.restaurant.RestaurantReview;

import java.time.LocalDateTime;

public record RestaurantReviewDetail(
        Long id,
        Byte rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime embeddedAt
) {
    public static RestaurantReviewDetail from(RestaurantReview restaurantReview) {
        return new RestaurantReviewDetail(
                restaurantReview.getId(),
                restaurantReview.getRating(),
                restaurantReview.getComment(),
                restaurantReview.getCreatedAt(),
                restaurantReview.getUpdatedAt(),
                restaurantReview.getEmbeddedAt()
        );
    }
}
