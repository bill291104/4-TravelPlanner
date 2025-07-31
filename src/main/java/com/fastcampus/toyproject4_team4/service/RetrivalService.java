package com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.dto.AccommodationDetail;
import com.fastcampus.toyproject4_team4.dto.PlaceDetail;
import com.fastcampus.toyproject4_team4.dto.RestaurantDetail;
import com.fastcampus.toyproject4_team4.repository.AccommodationRepository;
import com.fastcampus.toyproject4_team4.repository.PlaceRepository;
import com.fastcampus.toyproject4_team4.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RetrivalService {
    
    private final PlaceRepository placeRepository;
    private final RestaurantRepository restaurantRepository;
    private final AccommodationRepository accommodationRepository;

    public List<?> getDetail(Domains domain, List<Long> pks) {
        return switch (domain) {
            case PLACE -> placeRepository.findAllById(pks).stream().map(PlaceDetail::from).toList();
            case RESTAURANT -> restaurantRepository.findAllById(pks).stream().map(RestaurantDetail::from).toList();
            case ACCOM -> accommodationRepository.findAllById(pks).stream().map(AccommodationDetail::from).toList();
            default -> throw new IllegalArgumentException("Unsupported domain: " + domain.getName() +
                ". Supported domains: place, restaurant, accom");
        };
    }
}