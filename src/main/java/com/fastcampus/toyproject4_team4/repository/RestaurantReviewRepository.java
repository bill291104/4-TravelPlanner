package com.fastcampus.toyproject4_team4.repository;

import com.fastcampus.toyproject4_team4.entity.restaurant.Restaurant;
import com.fastcampus.toyproject4_team4.entity.restaurant.RestaurantReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantReviewRepository extends JpaRepository<RestaurantReview, Long> {
    Page<RestaurantReview> findByRestaurant(Restaurant restaurant, Pageable pageable);
}
