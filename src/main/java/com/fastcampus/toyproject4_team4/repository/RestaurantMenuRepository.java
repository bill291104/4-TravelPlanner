package com.fastcampus.toyproject4_team4.repository;

import com.fastcampus.toyproject4_team4.entity.restaurant.Restaurant;
import com.fastcampus.toyproject4_team4.entity.restaurant.RestaurantMenu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantMenuRepository extends JpaRepository<RestaurantMenu, Integer> {
    Page<RestaurantMenu> findByRestaurant(Restaurant restaurant, Pageable pageable);
}
