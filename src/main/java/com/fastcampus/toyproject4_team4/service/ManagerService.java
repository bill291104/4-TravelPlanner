package  com.fastcampus.toyproject4_team4.service;


import com.fastcampus.toyproject4_team4.dto.FastAPIEmbedRequest;
import com.fastcampus.toyproject4_team4.entity.accomodation.AccomReview;
import com.fastcampus.toyproject4_team4.entity.accomodation.Accommodation;
import com.fastcampus.toyproject4_team4.entity.accomodation.Amenity;
import com.fastcampus.toyproject4_team4.entity.place.Place;
import com.fastcampus.toyproject4_team4.entity.place.PlaceReview;
import com.fastcampus.toyproject4_team4.entity.restaurant.Restaurant;
import com.fastcampus.toyproject4_team4.entity.restaurant.RestaurantMenu;
import com.fastcampus.toyproject4_team4.entity.restaurant.RestaurantReview;
import com.fastcampus.toyproject4_team4.repository.*;
import jakarta.validation.constraints.Null;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerService {

    // FastAPI 서버 주소 주입
    @Value("${fastapi.server.url}")
    private String fastApiUrl;

    // Repository 주입
    private final PlaceRepository placeRepository;
    private final PlaceReviewRepository placeReviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantReviewRepository restaurantReviewRepository;
    private final AccomRepository accomRepository;
    private final AccomReviewRepository accomReviewRepository;
    private final AmenityRepository amenityRepository;
    private final RestaurantMenuRepository restaurantMenuRepository;

    public Page<Place> getAllPlaces(Pageable pageable) {
        return placeRepository.findAll(pageable);
    }

    public Page<Restaurant> getAllRestaurants(Pageable pageable) {
        return restaurantRepository.findAll(pageable);
    }

    public Page<Accommodation> getAllAccoms(Pageable pageable) {
        return accomRepository.findAll(pageable);
    }

    // PlaceReview Read
    public Page<PlaceReview> getReviewsByPlace(Integer placeId, Pageable pageable) {
        Place place = placeRepository.findById(placeId).orElseThrow(IllegalArgumentException::new);
        return placeReviewRepository.findByPlace(place, pageable);
    }
    // AccomReview Read
    public Page<AccomReview> getReviewsByAccom(Integer accomId, Pageable pageable) {
        Accommodation accom = accomRepository.findById(accomId).orElseThrow(IllegalArgumentException::new);
        return accomReviewRepository.findByAccommodation(accom, pageable);
    }
    // RestaurantReview Read
    public Page<RestaurantReview> getReviewsByRestaurant(Integer restaurantId, Pageable pageable) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(IllegalArgumentException::new);
        return restaurantReviewRepository.findByRestaurant(restaurant, pageable);
    }
    // Amenity Read
    public Page<Amenity> getAmenitiesByAccom(Integer accomId, Pageable pageable) {
        Accommodation accom = accomRepository.findById(accomId).orElseThrow(IllegalArgumentException::new);
        return amenityRepository.findByAccommodation(accom, pageable);
    }
    // RestaurantMenu Read
    public Page<RestaurantMenu> getRestaurantMenusByRestaurant(Integer restaurantId, Pageable pageable) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(IllegalArgumentException::new);
        return restaurantMenuRepository.findByRestaurant(restaurant, pageable);
    }

    // Place Create
    public void embedPlace(Integer placeId) {
        Place place = placeRepository.findById(placeId).orElseThrow(IllegalArgumentException::new);
        FastAPIEmbedRequest request = new FastAPIEmbedRequest(placeId, place.getDescription(), null, null);
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=place", request);
    }
    // Place Batch Create
    public void embedPlaceBatch(List<Integer> placeIds) {
        List<FastAPIEmbedRequest> requests = placeIds.stream().map(placeId -> {
            Place place = placeRepository.findById(placeId).orElseThrow(IllegalArgumentException::new);
            return new FastAPIEmbedRequest(placeId, place.getDescription(), null, null);
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=place", requests);
    }
    // Accom Create
    public void embedAccom(Integer accomId) {
        Accommodation accom = accomRepository.findById(accomId).orElseThrow(IllegalArgumentException::new);
        List<String> amenities = accom.getAmenityList().stream().map(Amenity::getName).toList();
        FastAPIEmbedRequest request = new FastAPIEmbedRequest(accomId, accom.getDescription(), null, amenities);
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=accom", request);
    }
    // Accom Batch Create
    public void embedAccomBatch(List<Integer> accomIds) {
        List<FastAPIEmbedRequest> requests = accomIds.stream().map(accomId -> {
            Accommodation accom = accomRepository.findById(accomId).orElseThrow(IllegalArgumentException::new);
            List<String> amenities = accom.getAmenityList().stream().map(Amenity::getName).toList();
            return new FastAPIEmbedRequest(accomId, accom.getDescription(), null, amenities);
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=accom", requests);
    }
    // Restaurant Create
    public void embedRestaurant(Integer restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(IllegalArgumentException::new);
        List<String> restaurantMenuList = restaurant.getRestaurantMenuList().stream().map(RestaurantMenu::getRestaurantMenuName).toList();
        FastAPIEmbedRequest request = new FastAPIEmbedRequest(restaurantId, restaurant.getDescription(), null, restaurantMenuList);
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=restaurant", request);
    }
    // Restaurant Batch Create
    public void embedRestaurantBatch(List<Integer> restaurantIds) {
        List<FastAPIEmbedRequest> requests = restaurantIds.stream().map(restaurantId -> {
            Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(IllegalArgumentException::new);
            List<String> restaurantMenuList = restaurant.getRestaurantMenuList().stream().map(RestaurantMenu::getRestaurantMenuName).toList();
            return new FastAPIEmbedRequest(restaurantId, restaurant.getDescription(), null, restaurantMenuList);
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=restaurant", requests);
    }
    // PlaceReview Create
    public void embedPlaceReview(Integer placeReviewId) {
        PlaceReview placeReview = placeReviewRepository.findById(placeReviewId).orElseThrow(IllegalArgumentException::new);
        FastAPIEmbedRequest request = new FastAPIEmbedRequest(placeReviewId, placeReview.getComment(), null, null);
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=place_review", request);
    }
    // PlaceReview Batch Create
    public void embedPlaceReviewBatch(List<Integer> placeReviewIds) {
        List<FastAPIEmbedRequest> requests = placeReviewIds.stream().map(placeReviewId -> {
            PlaceReview placeReview = placeReviewRepository.findById(placeReviewId).orElseThrow(IllegalArgumentException::new);
            return new FastAPIEmbedRequest(placeReviewId, placeReview.getComment(), null, null);
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=place_review", requests);
    }
    // AccomReview Create
    public void embedAccomReview(Integer accomReviewId) {
        AccomReview accomReview = accomReviewRepository.findById(accomReviewId).orElseThrow(IllegalArgumentException::new);
        FastAPIEmbedRequest request = new FastAPIEmbedRequest(accomReviewId, accomReview.getComment(), null, null);
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=accom_review", request);
    }
    // AccomReview Batch Create
    public void embedAccomReviewBatch(List<Integer> accomReviewIds) {
        List<FastAPIEmbedRequest> requests = accomReviewIds.stream().map(accomReviewId -> {
            AccomReview accomReview = accomReviewRepository.findById(accomReviewId).orElseThrow(IllegalArgumentException::new);
            return new FastAPIEmbedRequest(accomReviewId, accomReview.getComment(), null, null);
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=accom_review", requests);
    }
    // RestaurantReview Create
    public void embedRestaurantReview(Integer restaurantReviewId) {
        RestaurantReview restaurantReview = restaurantReviewRepository.findById(restaurantReviewId).orElseThrow(IllegalArgumentException::new);
        FastAPIEmbedRequest request = new FastAPIEmbedRequest(restaurantReviewId, restaurantReview.getComment(), null, null);
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=restaurant_review", request);
    }
    // RestaurantReview Batch Create
    public void embedRestaurantReviewBatch(List<Integer> restaurantReviewIds) {
        List<FastAPIEmbedRequest> requests = restaurantReviewIds.stream().map(restaurantReviewId -> {
            RestaurantReview restaurantReview = restaurantReviewRepository.findById(restaurantReviewId).orElseThrow(IllegalArgumentException::new);
            return new FastAPIEmbedRequest(restaurantReviewId, restaurantReview.getComment(), null, null);
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=restaurant_review", requests);
    }

    // Accom Delete
    public void deleteAccom(Integer accomId) {
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=accom&pk=" + accomId);
    }
    // Accom Batch Delete
    public void deleteAccomBatch(List<Integer> accomIds) {
        String queryString = accomIds.stream().map(accomId -> "pks=" + accomId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=accom&" + queryString);
    }
    // Place Delete
    public void deletePlace(Integer placeId) {
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=place&pk=" + placeId);
    }
    // Place Batch Delete
    public void deletePlaceBatch(List<Integer> placeIds) {
        String queryString = placeIds.stream().map(placeId -> "pks=" + placeId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=accom&" + queryString);
    }
    // Restaurant Delete
    public void deleteRestaurant(Integer restaurantId) {
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=restaurant&pk=" + restaurantId);
    }
    // Restaurant Batch Delete
    public void deleteRestaurantBatch(List<Integer> restaurantIds) {
        String queryString = restaurantIds.stream().map(restaurantId -> "pks=" + restaurantId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=restaurant&pk=" + queryString);
    }
    // AccomReview Delete
    public void deleteAccomReview(Integer accomReviewId) {
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=accom_review&pk=" + accomReviewId);
    }
    // AccomReview Batch Delete
    public void deleteAccomReviewBatch(List<Integer> accomReviewIds) {
        String queryString = accomReviewIds.stream().map(accomReviewId -> "pks=" + accomReviewId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=accom_review&pk=" + queryString);
    }
    // PlaceReview Delete
    public void deletePlaceReview(Integer placeReviewId) {
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=place_review&pk=" + placeReviewId);
    }
    // PlaceReview Batch Delete
    public void deletePlaceReviewBatch(List<Integer> placeReviewIds) {
        String queryString = placeReviewIds.stream().map(placeReviewId -> "pks=" + placeReviewId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=place_review&pk=" + queryString);
    }
    // RestaurantReview Delete
    public void deleteRestaurantReview(Integer restaurantReviewId) {
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=restaurant_review&pk=" + restaurantReviewId);
    }
    // RestaurantReview Batch Delete
    public void deleteRestaurantReviewBatch(List<Integer> restaurantReviewIds) {
        String queryString = restaurantReviewIds.stream().map(restaurantReviewId -> "pks=" + restaurantReviewId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=restaurant_review&pk=" + queryString);
    }

}