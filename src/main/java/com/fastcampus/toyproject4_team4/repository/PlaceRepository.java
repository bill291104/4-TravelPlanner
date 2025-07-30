package com.fastcampus.toyproject4_team4.repository;

import com.fastcampus.toyproject4_team4.entity.place.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Integer> {
}
