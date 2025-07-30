package com.fastcampus.toyproject4_team4.repository;

import com.fastcampus.toyproject4_team4.entity.accomodation.AccommodationReview;
import com.fastcampus.toyproject4_team4.entity.accomodation.Accommodation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccommodationReviewRepository extends JpaRepository<AccommodationReview, Long> {
    Page<AccommodationReview> findByAccommodation(Accommodation accommodation, Pageable pageable);
}
