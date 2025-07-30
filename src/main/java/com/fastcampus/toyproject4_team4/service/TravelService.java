package com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.dto.TravelDetailResponse;
import com.fastcampus.toyproject4_team4.entity.accomodation.Accommodation;
import com.fastcampus.toyproject4_team4.entity.place.Place;
import com.fastcampus.toyproject4_team4.entity.restaurant.Restaurant;
import com.fastcampus.toyproject4_team4.repository.AccomRepository;
import com.fastcampus.toyproject4_team4.repository.PlaceRepository;
import com.fastcampus.toyproject4_team4.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class TravelService {
    
    private final PlaceRepository placeRepository;
    private final RestaurantRepository restaurantRepository;
    private final AccomRepository accommodationRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * pk List로 장소 데이터 조회
     * Repository에서 엔티티 조회 → DTO로 변환하여 반환
     */
    public List<Place> getPlacesByIds(List<Integer> pks) {
        List<Place> places = placeRepository.findAllById(pks);
        return places;
    }
    
    /**
     * pk List로 식당 데이터 조회  
     * Repository에서 엔티티 조회 → DTO로 변환하여 반환
     */
    public List<Restaurant> getRestaurantsByIds(List<Integer> pks) {
        List<Restaurant> restaurants = restaurantRepository.findAllById(pks);
        return restaurants;
    }
    
    /**
     * pk List로 숙소 데이터 조회
     * Repository에서 엔티티 조회 → DTO로 변환하여 반환  
     */
    public List<Accommodation> getAccommodationsByIds(List<Integer> pks) {
        List<Accommodation> accommodations = accommodationRepository.findAllById(pks);
        return accommodations;
    }
    
    /**
     * 통합 여행 상세 정보 조회 메서드
     * Input: String domain, List<Integer> pks
     * Output: TravelDetailResponse<T> (domain에 따라 PlaceDto, RestaurantDto, AccommodationDto)
     */
    public TravelDetailResponse<?> getDetail(String domain, List<Integer> pks) {
        return switch (domain.toLowerCase()) {
            case "place" -> {
                List<Place> places = placeRepository.findAllById(pks);
                yield new TravelDetailResponse<>(domain, places);
            }
            case "restaurant" -> {
                List<Restaurant> restaurants = restaurantRepository.findAllById(pks);
                yield new TravelDetailResponse<>(domain, restaurants);
            }
            case "accom" -> {
                List<Accommodation> accommodations = accommodationRepository.findAllById(pks);
                yield new TravelDetailResponse<>(domain, accommodations);
            }
            default -> throw new IllegalArgumentException("Unsupported domain: " + domain + 
                ". Supported domains: place, restaurant, accom");
        };
    }
}