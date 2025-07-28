package com.fastcampus.toyproject4_team4.repository;

import com.fastcampus.toyproject4_team4.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Integer> {
    
    // JpaRepository가 제공하는 findAllById() 메서드를 사용
    // List<Integer> pks를 받아서 WHERE pk IN (1,2,3,...) 쿼리를 자동 생성
    // 반환 타입: List<Restaurant>
}