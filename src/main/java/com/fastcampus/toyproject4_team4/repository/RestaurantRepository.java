package com.fastcampus.toyproject4_team4.repository;

import com.fastcampus.toyproject4_team4.entity.restaurant.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Integer> {
}
