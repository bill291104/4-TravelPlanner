package  com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.dto.*;
import com.fastcampus.toyproject4_team4.entity.Hashtag;
import com.fastcampus.toyproject4_team4.entity.accomodation.AccommodationReview;
import com.fastcampus.toyproject4_team4.entity.accomodation.Accommodation;
import com.fastcampus.toyproject4_team4.entity.accomodation.Amenity;
import com.fastcampus.toyproject4_team4.entity.place.Place;
import com.fastcampus.toyproject4_team4.entity.place.PlaceReview;
import com.fastcampus.toyproject4_team4.entity.restaurant.Restaurant;
import com.fastcampus.toyproject4_team4.entity.restaurant.Menu;
import com.fastcampus.toyproject4_team4.entity.restaurant.RestaurantReview;
import com.fastcampus.toyproject4_team4.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerService {

    // FastAPI 서버 주소 주입
    @Value("${fastapi.url}")
    private String fastApiUrl;

    // Repository 주입
    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository placeReviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantReviewRepository restaurantReviewRepository;
    private final AccommodationRepository accommodationRepository;
    private final AccommodationReviewRepository accommodationReviewRepository;

    public Page<?> getAllMain(Domains mainDomain, Pageable pageable) {
        return switch (mainDomain) {
            case PLACE -> getAllPlaces(pageable).map(PlaceDetail::from);
            case RESTAURANT -> getAllRestaurants(pageable).map(RestaurantDetail::from);
            case ACCOM -> getAllAccoms(pageable).map(AccommodationDetail::from);
            default -> throw new IllegalArgumentException("잘못된 메인 도메인입니다.");
        };
    }

    // Place Read
    private Page<Place> getAllPlaces(Pageable pageable) {
        return placeRepository.findAll(pageable);
    }
    // Restaurant Read
    private Page<Restaurant> getAllRestaurants(Pageable pageable) {
        return restaurantRepository.findAll(pageable);
    }
    // Accom Read
    private Page<Accommodation> getAllAccoms(Pageable pageable) {
        return accommodationRepository.findAll(pageable);
    }

    public Page<?> getAllSub(Domains subDomain, Long pk, Pageable pageable) {
        return switch (subDomain) {
            case PLACE_REVIEW -> getReviewsByPlace(pk, pageable).map(PlaceReviewDetail::from);
            case ACCOM_REVIEW -> getReviewsByAccom(pk, pageable).map(AccommodationReviewDetail::from);
            case RESTAURANT_REVIEW -> getReviewsByRestaurant(pk, pageable).map(RestaurantReviewDetail::from);
            default -> throw new IllegalArgumentException("잘못된 서브 도메인입니다.");
        };
    }

    // PlaceReview Read
    private Page<PlaceReview> getReviewsByPlace(Long placeId, Pageable pageable) {
        Place place = placeRepository.findById(placeId).orElseThrow(IllegalArgumentException::new);
        return placeReviewRepository.findByPlace(place, pageable);
    }
    // AccomReview Read
    private Page<AccommodationReview> getReviewsByAccom(Long accomId, Pageable pageable) {
        Accommodation accom = accommodationRepository.findById(accomId).orElseThrow(IllegalArgumentException::new);
        return accommodationReviewRepository.findByAccommodation(accom, pageable);
    }
    // RestaurantReview Read
    private Page<RestaurantReview> getReviewsByRestaurant(Long restaurantId, Pageable pageable) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(IllegalArgumentException::new);
        return restaurantReviewRepository.findByRestaurant(restaurant, pageable);
    }

    public void embedding(Domains domain, Long pk) {
        switch (domain) {
            case PLACE -> embedPlace(pk);
            case ACCOM -> embedAccom(pk);
            case RESTAURANT -> embedRestaurant(pk);
            case PLACE_REVIEW -> embedPlaceReview(pk);
            case ACCOM_REVIEW -> embedAccomReview(pk);
            case RESTAURANT_REVIEW -> embedRestaurantReview(pk);
        }
    }

    public void embeddingBatch(Domains domain, List<Long> pks) {
        switch (domain) {
            case PLACE -> embedPlaceBatch(pks);
            case ACCOM -> embedAccomBatch(pks);
            case RESTAURANT -> embedRestaurantBatch(pks);
            case PLACE_REVIEW -> embedPlaceReviewBatch(pks);
            case ACCOM_REVIEW -> embedAccomReviewBatch(pks);
            case RESTAURANT_REVIEW -> embedRestaurantReviewBatch(pks);
        }
    }
    // Place Create
    private void embedPlace(Long placeId) {
        // 영속성 엔티티 가져 오기
        Place place = placeRepository.findById(placeId).orElseThrow(IllegalArgumentException::new);
        // 관련 데이터: 여행 스타일
        String travelStyle = place.getTravelStyle().getName() + ": " + place.getTravelStyle().getDescription();
        // 관련 데이터: 해시 태그
        String hashTags = "해시 태그\n" + place.getHashtags().stream().map(Hashtag::getContent).collect(Collectors.joining(", "));
        // 관련 데이터 조립
        List<String> relatedContent = List.of(travelStyle, hashTags);
        // 임베딩 요청 객체 만들기
        EmbeddingRequest request = new EmbeddingRequest(placeId, place.getDescription(), new HashMap<>(), relatedContent);
        // 요청, 응답
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=place", request);
        // 임베딩 시간 기록 하기
        place.setEmbeddedAt(LocalDateTime.now());
        placeRepository.save(place);
    }
    // Place Batch Create
    private void embedPlaceBatch(List<Long> placeIds) {
        List<Place> places = placeRepository.findAllById(placeIds);

        List<EmbeddingRequest> requests = places.stream()
                .map(place -> {
                    String travelStyle = place.getTravelStyle().getName() + ": " + place.getTravelStyle().getDescription();
                    String hashTags = "해시 태그\n" + place.getHashtags().stream().map(Hashtag::getContent).collect(Collectors.joining(", "));
                    List<String> relatedContent = List.of(travelStyle, hashTags);
                    return new EmbeddingRequest(place.getId(), place.getDescription(), new HashMap<>(), relatedContent);
                })
                .toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding/batch?domain=place", requests);

        places.forEach(place -> place.setEmbeddedAt(LocalDateTime.now()));
        placeRepository.saveAll(places);
    }
    // Accom Create
    private void embedAccom(Long accomId) {
        Accommodation accom = accommodationRepository.findById(accomId).orElseThrow(IllegalArgumentException::new);
        // 관련 데이터: 편의 시설
        String amenities = "편의 시설: " + accom.getAmenities().stream().map(Amenity::getName).collect(Collectors.joining(", "));
        // 관련 데이터: 여행 스타일
        String travelStyle = accom.getTravelStyle().getName() + ": " + accom.getTravelStyle().getDescription();
        // 관련 데이터: 해시 태그
        String hashTags = "해시 태그\n" + accom.getHashtags().stream().map(Hashtag::getContent).collect(Collectors.joining(", "));
        // 관련 데이터 조립
        List<String> relatedContent = List.of(amenities, travelStyle, hashTags);

        EmbeddingRequest request = new EmbeddingRequest(accomId, accom.getDescription(), new HashMap<>(), relatedContent);
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=accom", request);

        accom.setEmbeddedAt(LocalDateTime.now());
        accommodationRepository.save(accom);
    }
    // Accom Batch Create
    private void embedAccomBatch(List<Long> accomIds) {
        List<Accommodation> accommodations = accommodationRepository.findAllById(accomIds);

        List<EmbeddingRequest> requests = accommodations.stream().map(accom -> {
            String amenities = "편의 시설: " + accom.getAmenities().stream().map(Amenity::getName).collect(Collectors.joining(", "));
            String travelStyle = accom.getTravelStyle().getName() + ": " + accom.getTravelStyle().getDescription();
            String hashTags = "해시 태그\n" + accom.getHashtags().stream().map(Hashtag::getContent).collect(Collectors.joining(", "));
            List<String> relatedContent = List.of(amenities, travelStyle, hashTags);
            return new EmbeddingRequest(accom.getId(), accom.getDescription(), new HashMap<>(), relatedContent);
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding/batch?domain=accom", requests);

        accommodations.forEach(accom -> accom.setEmbeddedAt(LocalDateTime.now()));
        accommodationRepository.saveAll(accommodations);
    }
    // Restaurant Create
    public void embedRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(IllegalArgumentException::new);

        // 관련 데이터: 메뉴
        String restaurantMenus = "메뉴: " + restaurant.getMenus().stream().map(Menu::getName).collect(Collectors.joining(", "));
        // 관련 데이터: 여행 스타일
        String travelStyle = restaurant.getTravelStyle().getName() + ": " + restaurant.getTravelStyle().getDescription();
        // 관련 데이터: 해시 태그
        String hashTags = "해시 태그\n" + restaurant.getHashtags().stream().map(Hashtag::getContent).collect(Collectors.joining(", "));
        // 관련 데이터 조립
        List<String> relatedContent = List.of(restaurantMenus, travelStyle, hashTags);

        EmbeddingRequest request = new EmbeddingRequest(restaurantId, restaurant.getDescription(), new HashMap<>(), relatedContent);
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=restaurant", request);

        restaurant.setEmbeddedAt(LocalDateTime.now());
        restaurantRepository.save(restaurant);
    }
    // Restaurant Batch Create
    public void embedRestaurantBatch(List<Long> restaurantIds) {
        List<Restaurant> restaurants = restaurantRepository.findAllById(restaurantIds);

        List<EmbeddingRequest> requests = restaurants.stream().map(restaurant -> {
            String restaurantMenus = "메뉴: " + restaurant.getMenus().stream().map(Menu::getName).collect(Collectors.joining(", "));
            String travelStyle = restaurant.getTravelStyle().getName() + ": " + restaurant.getTravelStyle().getDescription();
            String hashTags = "해시 태그\n" + restaurant.getHashtags().stream().map(Hashtag::getContent).collect(Collectors.joining(", "));
            List<String> relatedContent = List.of(restaurantMenus, travelStyle, hashTags);
            return new EmbeddingRequest(restaurant.getId(), restaurant.getDescription(), new HashMap<>(), relatedContent);
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding/batch?domain=restaurant", requests);

        restaurants.forEach(restaurant -> restaurant.setEmbeddedAt(LocalDateTime.now()));
        restaurantRepository.saveAll(restaurants);
    }
    // PlaceReview Create
    public void embedPlaceReview(Long placeReviewId) {
        PlaceReview review = placeReviewRepository.findById(placeReviewId).orElseThrow(IllegalArgumentException::new);
        // metadata 에 fk 함께 임베딩 하기
        HashMap<String, Long> metadata = new HashMap<>();
        metadata.put("fk", review.getPlace().getId());

        EmbeddingRequest request = new EmbeddingRequest(placeReviewId, review.getComment(), metadata, new ArrayList<>());
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=place_review", request);

        review.setEmbeddedAt(LocalDateTime.now());
        placeReviewRepository.save(review);
    }
    // PlaceReview Batch Create
    public void embedPlaceReviewBatch(List<Long> placeReviewIds) {
        List<PlaceReview> reviews = placeReviewRepository.findAllById(placeReviewIds);

        List<EmbeddingRequest> requests = reviews.stream().map(placeReview -> {
            HashMap<String, Long> metadata = new HashMap<>();
            metadata.put("fk", placeReview.getPlace().getId());
            return new EmbeddingRequest(placeReview.getId(), placeReview.getComment(), metadata, new ArrayList<>());
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding/batch?domain=place_review", requests);

        reviews.forEach(placeReview -> placeReview.setEmbeddedAt(LocalDateTime.now()));
        placeReviewRepository.saveAll(reviews);
    }
    // AccomReview Create
    public void embedAccomReview(Long accomReviewId) {
        AccommodationReview review = accommodationReviewRepository.findById(accomReviewId).orElseThrow(IllegalArgumentException::new);

        HashMap<String, Long> metadata = new HashMap<>();
        metadata.put("fk", review.getAccommodation().getId());

        EmbeddingRequest request = new EmbeddingRequest(accomReviewId, review.getComment(), metadata, new ArrayList<>());
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=accom_review", request);

        review.setEmbeddedAt(LocalDateTime.now());
        accommodationReviewRepository.save(review);
    }
    // AccomReview Batch Create
    public void embedAccomReviewBatch(List<Long> accomReviewIds) {
        List<AccommodationReview> reviews = accommodationReviewRepository.findAllById(accomReviewIds);

        List<EmbeddingRequest> requests = reviews.stream().map(review -> {
            HashMap<String, Long> metadata = new HashMap<>();
            metadata.put("fk", review.getAccommodation().getId());
            return new EmbeddingRequest(review.getId(), review.getComment(), metadata, new ArrayList<>());
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding/batch?domain=accom_review", requests);

        reviews.forEach(review -> review.setEmbeddedAt(LocalDateTime.now()));
        accommodationReviewRepository.saveAll(reviews);
    }
    // RestaurantReview Create
    public void embedRestaurantReview(Long restaurantReviewId) {
        RestaurantReview restaurantReview = restaurantReviewRepository.findById(restaurantReviewId).orElseThrow(IllegalArgumentException::new);

        HashMap<String, Long> metadata = new HashMap<>();
        metadata.put("fk", restaurantReview.getRestaurant().getId());

        EmbeddingRequest request = new EmbeddingRequest(restaurantReviewId, restaurantReview.getComment(), metadata, new ArrayList<>());
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=restaurant_review", request);

        restaurantReview.setEmbeddedAt(LocalDateTime.now());
        restaurantReviewRepository.save(restaurantReview);
    }
    // RestaurantReview Batch Create
    public void embedRestaurantReviewBatch(List<Long> restaurantReviewIds) {
        List<RestaurantReview> reviews = restaurantReviewRepository.findAllById(restaurantReviewIds);

        List<EmbeddingRequest> requests = reviews.stream().map(review -> {
            HashMap<String, Long> metadata = new HashMap<>();
            metadata.put("fk", review.getRestaurant().getId());
            return new EmbeddingRequest(review.getId(), review.getComment(), metadata, new ArrayList<>());
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding/batch?domain=restaurant_review", requests);

        reviews.forEach(review -> review.setEmbeddedAt(LocalDateTime.now()));
        restaurantReviewRepository.saveAll(reviews);
    }

    public void delete(Domains domain, Long pk) {
        switch (domain) {
            case PLACE -> deletePlace(pk);
            case ACCOM -> deleteAccom(pk);
            case RESTAURANT -> deleteRestaurant(pk);
            case PLACE_REVIEW -> deletePlaceReview(pk);
            case ACCOM_REVIEW -> deleteAccomReview(pk);
            case RESTAURANT_REVIEW -> deleteRestaurantReview(pk);
            default -> throw new IllegalArgumentException("잘못된 도메인입니다.");
        }
    }

    public void deleteBatch(Domains domain, List<Long> pks) {
        switch (domain) {
            case PLACE -> deletePlaceBatch(pks);
            case ACCOM -> deleteAccomBatch(pks);
            case RESTAURANT -> deleteRestaurantBatch(pks);
            case PLACE_REVIEW -> deletePlaceReviewBatch(pks);
            case ACCOM_REVIEW -> deleteAccomReviewBatch(pks);
            case RESTAURANT_REVIEW -> deleteRestaurantReviewBatch(pks);
            default -> throw new IllegalArgumentException("잘못된 도메인입니다.");
        }
    }
    // Accom Delete
    public void deleteAccom(Long accomId) {
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=accom&pk=" + accomId);
    }
    // Accom Batch Delete
    public void deleteAccomBatch(List<Long> accomIds) {
        String queryString = accomIds.stream().map(accomId -> "pks=" + accomId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding/batch?domain=accom&" + queryString);
    }
    // Place Delete
    public void deletePlace(Long placeId) {
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=place&pk=" + placeId);
    }
    // Place Batch Delete
    public void deletePlaceBatch(List<Long> placeIds) {
        String queryString = placeIds.stream().map(placeId -> "pks=" + placeId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding/batch?domain=accom&" + queryString);
    }
    // Restaurant Delete
    public void deleteRestaurant(Long restaurantId) {
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=restaurant&pk=" + restaurantId);
    }
    // Restaurant Batch Delete
    public void deleteRestaurantBatch(List<Long> restaurantIds) {
        String queryString = restaurantIds.stream().map(restaurantId -> "pks=" + restaurantId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding/batch?domain=restaurant&pk=" + queryString);
    }
    // AccomReview Delete
    public void deleteAccomReview(Long accomReviewId) {
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=accom_review&pk=" + accomReviewId);
    }
    // AccomReview Batch Delete
    public void deleteAccomReviewBatch(List<Long> accomReviewIds) {
        String queryString = accomReviewIds.stream().map(accomReviewId -> "pks=" + accomReviewId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding/batch?domain=accom_review&pk=" + queryString);
    }
    // PlaceReview Delete
    public void deletePlaceReview(Long placeReviewId) {
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=place_review&pk=" + placeReviewId);
    }
    // PlaceReview Batch Delete
    public void deletePlaceReviewBatch(List<Long> placeReviewIds) {
        String queryString = placeReviewIds.stream().map(placeReviewId -> "pks=" + placeReviewId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding/batch?domain=place_review&pk=" + queryString);
    }
    // RestaurantReview Delete
    public void deleteRestaurantReview(Long restaurantReviewId) {
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=restaurant_review&pk=" + restaurantReviewId);
    }
    // RestaurantReview Batch Delete
    public void deleteRestaurantReviewBatch(List<Long> restaurantReviewIds) {
        String queryString = restaurantReviewIds.stream().map(restaurantReviewId -> "pks=" + restaurantReviewId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding/batch?domain=restaurant_review&pk=" + queryString);
    }
}