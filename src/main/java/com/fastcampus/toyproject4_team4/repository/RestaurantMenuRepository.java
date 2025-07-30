package com.fastcampus.toyproject4_team4.repository;

import com.fastcampus.toyproject4_team4.entity.restaurant.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantMenuRepository extends JpaRepository<Menu, Long> {}
