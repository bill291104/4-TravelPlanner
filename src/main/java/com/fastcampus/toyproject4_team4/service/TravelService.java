package com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.dto.PlaceDto;
import com.fastcampus.toyproject4_team4.dto.RestaurantDto;
import com.fastcampus.toyproject4_team4.dto.AccommodationDto;
import com.fastcampus.toyproject4_team4.dto.TravelDetailResponse;
import com.fastcampus.toyproject4_team4.entity.Place;
import com.fastcampus.toyproject4_team4.entity.Restaurant;
import com.fastcampus.toyproject4_team4.entity.Accommodation;
import com.fastcampus.toyproject4_team4.repository.PlaceRepository;
import com.fastcampus.toyproject4_team4.repository.RestaurantRepository;  
import com.fastcampus.toyproject4_team4.repository.AccommodationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Arrays;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class TravelService {
    
    private final PlaceRepository placeRepository;
    private final RestaurantRepository restaurantRepository;
    private final AccommodationRepository accommodationRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * pk List로 장소 데이터 조회
     * Repository에서 엔티티 조회 → DTO로 변환하여 반환
     */
    public List<PlaceDto> getPlacesByIds(List<Integer> pks) {
        List<Place> places = placeRepository.findAllById(pks);
        return places.stream()
                .map(this::convertToDto)
                .toList();
    }
    
    /**
     * pk List로 식당 데이터 조회  
     * Repository에서 엔티티 조회 → DTO로 변환하여 반환
     */
    public List<RestaurantDto> getRestaurantsByIds(List<Integer> pks) {
        List<Restaurant> restaurants = restaurantRepository.findAllById(pks);
        return restaurants.stream()
                .map(this::convertToDto)
                .toList();
    }
    
    /**
     * pk List로 숙소 데이터 조회
     * Repository에서 엔티티 조회 → DTO로 변환하여 반환  
     */
    public List<AccommodationDto> getAccommodationsByIds(List<Integer> pks) {
        List<Accommodation> accommodations = accommodationRepository.findAllById(pks);
        return accommodations.stream()
                .map(this::convertToDto)
                .toList();
    }
    
    /**
     * 통합 여행 상세 정보 조회 메서드
     * Input: String domain, List<Integer> pks
     * Output: TravelDetailResponse<T> (domain에 따라 PlaceDto, RestaurantDto, AccommodationDto)
     */
    public TravelDetailResponse<?> getDetail(String domain, List<Integer> pks) {
        return switch (domain.toLowerCase()) {
            case "place", "places" -> {
                List<PlaceDto> places = getPlacesByIds(pks);
                yield new TravelDetailResponse<>(domain, places);
            }
            case "restaurant", "restaurants" -> {
                List<RestaurantDto> restaurants = getRestaurantsByIds(pks);
                yield new TravelDetailResponse<>(domain, restaurants);
            }
            case "accommodation", "accommodations" -> {
                List<AccommodationDto> accommodations = getAccommodationsByIds(pks);
                yield new TravelDetailResponse<>(domain, accommodations);
            }
            default -> throw new IllegalArgumentException("Unsupported domain: " + domain + 
                ". Supported domains: place, restaurant, accommodation");
        };
    }
    
    // 엔티티 → DTO 변환 메서드들
    private PlaceDto convertToDto(Place place) {
        return new PlaceDto(
            place.getPk(),
            place.getName(),
            place.getLat(),
            place.getLon(),
            place.getExpCost(),
            place.getDescription(),
            place.getCategory(),
            place.getOperatingHours(),
            place.getRequiredTime()
        );
    }
    
    private RestaurantDto convertToDto(Restaurant restaurant) {
        return new RestaurantDto(
            restaurant.getPk(),
            restaurant.getName(),
            restaurant.getLat(),
            restaurant.getLon(),
            restaurant.getExpCost(),
            restaurant.getDescription(),
            restaurant.getCuisineType(),
            restaurant.getSignatureMenu(),
            restaurant.getOperatingHours()
        );
    }
    
    private AccommodationDto convertToDto(Accommodation accommodation) {
        // JSON 문자열을 List<String>으로 변환
        List<String> amenities = parseAmenities(accommodation.getAmenities());
        
        return new AccommodationDto(
            accommodation.getPk(),
            accommodation.getName(),
            accommodation.getLat(),
            accommodation.getLon(),
            accommodation.getExpCost(),
            accommodation.getDescription(),
            accommodation.getAccomType(),
            accommodation.getGrade(),
            amenities,
            accommodation.getCheckInOutTime(),
            accommodation.getBookingUrl()
        );
    }
    
    private List<String> parseAmenities(String amenitiesJson) {
        if (amenitiesJson == null || amenitiesJson.trim().isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(amenitiesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}