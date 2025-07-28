package com.fastcampus.toyproject4_team4.controller;

import com.fastcampus.toyproject4_team4.dto.PlaceDto;
import com.fastcampus.toyproject4_team4.dto.RestaurantDto;
import com.fastcampus.toyproject4_team4.dto.AccommodationDto;
import com.fastcampus.toyproject4_team4.dto.TravelDetailResponse;
import com.fastcampus.toyproject4_team4.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/travel")
@RequiredArgsConstructor
public class TravelController {
    
    private final TravelService travelService;
    
    /**
     * 장소 데이터 조회 API
     * POST /api/travel/places
     * 요청: {"pks": [1, 2, 3]}
     * 응답: List<PlaceDto>
     */
    @PostMapping("/places")
    public ResponseEntity<List<PlaceDto>> getPlaces(@RequestBody PlaceRequest request) {
        List<PlaceDto> places = travelService.getPlacesByIds(request.pks());
        return ResponseEntity.ok(places);
    }
    
    /**
     * 식당 데이터 조회 API  
     * POST /api/travel/restaurants
     * 요청: {"pks": [1, 2, 3]}
     * 응답: List<RestaurantDto>
     */
    @PostMapping("/restaurants")
    public ResponseEntity<List<RestaurantDto>> getRestaurants(@RequestBody RestaurantRequest request) {
        List<RestaurantDto> restaurants = travelService.getRestaurantsByIds(request.pks());
        return ResponseEntity.ok(restaurants);
    }
    
    /**
     * 숙소 데이터 조회 API
     * POST /api/travel/accommodations  
     * 요청: {"pks": [1, 2, 3]}
     * 응답: List<AccommodationDto>
     */
    @PostMapping("/accommodations")
    public ResponseEntity<List<AccommodationDto>> getAccommodations(@RequestBody AccommodationRequest request) {
        List<AccommodationDto> accommodations = travelService.getAccommodationsByIds(request.pks());
        return ResponseEntity.ok(accommodations);
    }
    
    /**
     * 통합 여행 상세 정보 조회 API
     * POST /api/travel/details
     * 요청: {"domain": "place", "pks": [1, 2, 3]}
     * 응답: {"domain": "place", "details": [PlaceDto...]}
     */
    @PostMapping("/details")
    public ResponseEntity<TravelDetailResponse<?>> getDetails(@RequestBody TravelDetailRequest request) {
        TravelDetailResponse<?> response = travelService.getDetail(request.domain(), request.pks());
        return ResponseEntity.ok(response);
    }
    
    // 요청 DTO들 (record class)
    public record PlaceRequest(List<Integer> pks) {}
    public record RestaurantRequest(List<Integer> pks) {}  
    public record AccommodationRequest(List<Integer> pks) {}
    public record TravelDetailRequest(String domain, List<Integer> pks) {}
}

// 생성 이유:
// - HTTP 요청 처리: Python/프론트엔드에서 pk 리스트를 POST로 전송받음
// - JSON 바인딩: {"pks": [1,2,3]} → List<Integer> 자동 변환
// - 서비스 호출: TravelService 메서드로 비즈니스 로직 위임  
// - 응답 반환: DTO 리스트를 JSON으로 자동 직렬화하여 응답