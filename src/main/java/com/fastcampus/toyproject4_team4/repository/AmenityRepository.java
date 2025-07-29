package com.fastcampus.toyproject4_team4.repository;

import com.fastcampus.toyproject4_team4.entity.accomodation.Accommodation;
import com.fastcampus.toyproject4_team4.entity.accomodation.Amenity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmenityRepository extends JpaRepository<Amenity, Integer> {
    Page<Amenity> findByAccommodation(Accommodation accommodation, Pageable pageable);
}
