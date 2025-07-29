package com.fastcampus.toyproject4_team4.repository;

import com.fastcampus.toyproject4_team4.entity.accomodation.AccomReview;
import com.fastcampus.toyproject4_team4.entity.accomodation.Accommodation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccomReviewRepository extends JpaRepository<AccomReview, Integer> {
    Page<AccomReview> findByAccommodation(Accommodation accommodation, Pageable pageable);
}
