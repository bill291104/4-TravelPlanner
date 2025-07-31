package com.fastcampus.toyproject4_team4.service;

import com.fastcampus.toyproject4_team4.Domains;
import com.fastcampus.toyproject4_team4.dto.*;
import com.fastcampus.toyproject4_team4.entity.Hashtag;
import com.fastcampus.toyproject4_team4.entity.TravelStyle;
import com.fastcampus.toyproject4_team4.entity.accomodation.Accommodation;
import com.fastcampus.toyproject4_team4.entity.accomodation.AccommodationReview;
import com.fastcampus.toyproject4_team4.entity.accomodation.Amenity;
import com.fastcampus.toyproject4_team4.entity.place.Place;
import com.fastcampus.toyproject4_team4.entity.place.PlaceReview;
import com.fastcampus.toyproject4_team4.entity.restaurant.Menu;
import com.fastcampus.toyproject4_team4.entity.restaurant.Restaurant;
import com.fastcampus.toyproject4_team4.entity.restaurant.RestaurantReview;
import com.fastcampus.toyproject4_team4.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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

    @Transactional
    public Page<?> getAllMain(Domains mainDomain, Pageable pageable) {
        return switch (mainDomain) {
            case PLACE -> getAllPlaces(pageable).map(PlaceDetail::from);
            case RESTAURANT -> getAllRestaurants(pageable).map(RestaurantDetail::from);
            case ACCOM -> getAllAccoms(pageable).map(AccommodationDetail::from);
            case PLACE_REVIEW -> getAllPlaceReviews(pageable).map(PlaceReviewDetail::from);
            case RESTAURANT_REVIEW -> getAllRestaurantReviews(pageable).map(RestaurantReviewDetail::from);
            case ACCOM_REVIEW -> getAllAccomReviews(pageable).map(AccommodationReviewDetail::from);
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
    // PlaceReview Read
    private Page<PlaceReview> getAllPlaceReviews(Pageable pageable) {
        return placeReviewRepository.findAll(pageable);
    }
    // RestaurantReview Read
    private Page<RestaurantReview> getAllRestaurantReviews(Pageable pageable) {
        return restaurantReviewRepository.findAll(pageable);
    }
    // AccomReview Read
    private Page<AccommodationReview> getAllAccomReviews(Pageable pageable) {
        return accommodationReviewRepository.findAll(pageable);
    }

    @Transactional
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

    @Transactional
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

    @Transactional
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
        Place place = placeRepository.findById(placeId).orElseThrow(IllegalArgumentException::new);
        List<String> relatedContent = new ArrayList<>();

        // Null-check: TravelStyle
        TravelStyle style = place.getTravelStyle();
        if (style != null) {
            relatedContent.add(style.getName() + ": " + style.getDescription());
        }

        // Null-check: Hashtags
        List<Hashtag> hashtags = place.getHashtags();
        if (!CollectionUtils.isEmpty(hashtags)) {
            relatedContent.add("해시 태그\n" + hashtags.stream().map(Hashtag::getContent).collect(Collectors.joining(", ")));
        }

        EmbeddingRequest request = new EmbeddingRequest(placeId, place.getDescription(), new HashMap<>(), relatedContent);
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=place", request);

        place.setEmbeddedAt(LocalDateTime.now());
        placeRepository.save(place);
    }
    // Place Batch Create
    private void embedPlaceBatch(List<Long> placeIds) {
        List<Place> places = placeRepository.findAllById(placeIds);
        List<EmbeddingRequest> requests = places.stream()
                .map(place -> {
                    List<String> relatedContent = new ArrayList<>();
                    // Null-check: TravelStyle
                    TravelStyle style = place.getTravelStyle();
                    if (style != null) {
                        relatedContent.add(style.getName() + ": " + style.getDescription());
                    }
                    // Null-check: Hashtags
                    List<Hashtag> hashtags = place.getHashtags();
                    if (!CollectionUtils.isEmpty(hashtags)) {
                        relatedContent.add("해시 태그\n" + hashtags.stream().map(Hashtag::getContent).collect(Collectors.joining(", ")));
                    }
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
        List<String> relatedContent = new ArrayList<>();

        // Null-check: Amenities
        List<Amenity> amenities = accom.getAmenities();
        if (!CollectionUtils.isEmpty(amenities)) {
            relatedContent.add("편의 시설: " + amenities.stream().map(Amenity::getName).collect(Collectors.joining(", ")));
        }

        // Null-check: TravelStyle
        TravelStyle style = accom.getTravelStyle();
        if (style != null) {
            relatedContent.add(style.getName() + ": " + style.getDescription());
        }

        // Null-check: Hashtags
        List<Hashtag> hashtags = accom.getHashtags();
        if (!CollectionUtils.isEmpty(hashtags)) {
            relatedContent.add("해시 태그\n" + hashtags.stream().map(Hashtag::getContent).collect(Collectors.joining(", ")));
        }

        EmbeddingRequest request = new EmbeddingRequest(accomId, accom.getDescription(), new HashMap<>(), relatedContent);
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=accom", request);

        accom.setEmbeddedAt(LocalDateTime.now());
        accommodationRepository.save(accom);
    }
    // Accom Batch Create
    private void embedAccomBatch(List<Long> accomIds) {
        List<Accommodation> accommodations = accommodationRepository.findAllById(accomIds);

        List<EmbeddingRequest> requests = accommodations.stream().map(accom -> {
            List<String> relatedContent = new ArrayList<>();
            // Null-check: Amenities
            List<Amenity> amenities = accom.getAmenities();
            if (!CollectionUtils.isEmpty(amenities)) {
                relatedContent.add("편의 시설: " + amenities.stream().map(Amenity::getName).collect(Collectors.joining(", ")));
            }
            // Null-check: TravelStyle
            TravelStyle style = accom.getTravelStyle();
            if (style != null) {
                relatedContent.add(style.getName() + ": " + style.getDescription());
            }
            // Null-check: Hashtags
            List<Hashtag> hashtags = accom.getHashtags();
            if (!CollectionUtils.isEmpty(hashtags)) {
                relatedContent.add("해시 태그\n" + hashtags.stream().map(Hashtag::getContent).collect(Collectors.joining(", ")));
            }
            return new EmbeddingRequest(accom.getId(), accom.getDescription(), new HashMap<>(), relatedContent);
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding/batch?domain=accom", requests);

        accommodations.forEach(accom -> accom.setEmbeddedAt(LocalDateTime.now()));
        accommodationRepository.saveAll(accommodations);
    }
    // Restaurant Create
    private void embedRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(IllegalArgumentException::new);
        List<String> relatedContent = new ArrayList<>();

        // Null-check: Menus
        List<Menu> menus = restaurant.getMenus();
        if (!CollectionUtils.isEmpty(menus)) {
            relatedContent.add("메뉴: " + menus.stream().map(Menu::getName).collect(Collectors.joining(", ")));
        }

        // Null-check: TravelStyle
        TravelStyle style = restaurant.getTravelStyle();
        if (style != null) {
            relatedContent.add(style.getName() + ": " + style.getDescription());
        }

        // Null-check: Hashtags
        List<Hashtag> hashtags = restaurant.getHashtags();
        if (!CollectionUtils.isEmpty(hashtags)) {
            relatedContent.add("해시 태그\n" + hashtags.stream().map(Hashtag::getContent).collect(Collectors.joining(", ")));
        }

        EmbeddingRequest request = new EmbeddingRequest(restaurantId, restaurant.getDescription(), new HashMap<>(), relatedContent);
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=restaurant", request);

        restaurant.setEmbeddedAt(LocalDateTime.now());
        restaurantRepository.save(restaurant);
    }
    // Restaurant Batch Create
    private void embedRestaurantBatch(List<Long> restaurantIds) {
        List<Restaurant> restaurants = restaurantRepository.findAllById(restaurantIds);

        List<EmbeddingRequest> requests = restaurants.stream().map(restaurant -> {
            List<String> relatedContent = new ArrayList<>();
            // Null-check: Menus
            List<Menu> menus = restaurant.getMenus();
            if (!CollectionUtils.isEmpty(menus)) {
                relatedContent.add("메뉴: " + menus.stream().map(Menu::getName).collect(Collectors.joining(", ")));
            }
            // Null-check: TravelStyle
            TravelStyle style = restaurant.getTravelStyle();
            if (style != null) {
                relatedContent.add(style.getName() + ": " + style.getDescription());
            }
            // Null-check: Hashtags
            List<Hashtag> hashtags = restaurant.getHashtags();
            if (!CollectionUtils.isEmpty(hashtags)) {
                relatedContent.add("해시 태그\n" + hashtags.stream().map(Hashtag::getContent).collect(Collectors.joining(", ")));
            }
            return new EmbeddingRequest(restaurant.getId(), restaurant.getDescription(), new HashMap<>(), relatedContent);
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding/batch?domain=restaurant", requests);

        restaurants.forEach(restaurant -> restaurant.setEmbeddedAt(LocalDateTime.now()));
        restaurantRepository.saveAll(restaurants);
    }
    // PlaceReview Create
    private void embedPlaceReview(Long placeReviewId) {
        PlaceReview review = placeReviewRepository.findById(placeReviewId).orElseThrow(IllegalArgumentException::new);
        HashMap<String, Long> metadata = new HashMap<>();

        // Null-check: Place
        if (review.getPlace() != null) {
            metadata.put("fk", review.getPlace().getId());
        }

        EmbeddingRequest request = new EmbeddingRequest(placeReviewId, review.getComment(), metadata, new ArrayList<>());
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=place_review", request);

        review.setEmbeddedAt(LocalDateTime.now());
        placeReviewRepository.save(review);
    }
    // PlaceReview Batch Create
    private void embedPlaceReviewBatch(List<Long> placeReviewIds) {
        List<PlaceReview> reviews = placeReviewRepository.findAllById(placeReviewIds);

        List<EmbeddingRequest> requests = reviews.stream().map(placeReview -> {
            HashMap<String, Long> metadata = new HashMap<>();
            // Null-check: Place
            if (placeReview.getPlace() != null) {
                metadata.put("fk", placeReview.getPlace().getId());
            }
            return new EmbeddingRequest(placeReview.getId(), placeReview.getComment(), metadata, new ArrayList<>());
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding/batch?domain=place_review", requests);

        reviews.forEach(placeReview -> placeReview.setEmbeddedAt(LocalDateTime.now()));
        placeReviewRepository.saveAll(reviews);
    }
    // AccomReview Create
    private void embedAccomReview(Long accomReviewId) {
        AccommodationReview review = accommodationReviewRepository.findById(accomReviewId).orElseThrow(IllegalArgumentException::new);
        HashMap<String, Long> metadata = new HashMap<>();

        // Null-check: Accommodation
        if (review.getAccommodation() != null) {
            metadata.put("fk", review.getAccommodation().getId());
        }

        EmbeddingRequest request = new EmbeddingRequest(accomReviewId, review.getComment(), metadata, new ArrayList<>());
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=accom_review", request);

        review.setEmbeddedAt(LocalDateTime.now());
        accommodationReviewRepository.save(review);
    }
    // AccomReview Batch Create
    private void embedAccomReviewBatch(List<Long> accomReviewIds) {
        List<AccommodationReview> reviews = accommodationReviewRepository.findAllById(accomReviewIds);

        List<EmbeddingRequest> requests = reviews.stream().map(review -> {
            HashMap<String, Long> metadata = new HashMap<>();
            // Null-check: Accommodation
            if (review.getAccommodation() != null) {
                metadata.put("fk", review.getAccommodation().getId());
            }
            return new EmbeddingRequest(review.getId(), review.getComment(), metadata, new ArrayList<>());
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding/batch?domain=accom_review", requests);

        reviews.forEach(review -> review.setEmbeddedAt(LocalDateTime.now()));
        accommodationReviewRepository.saveAll(reviews);
    }
    // RestaurantReview Create
    private void embedRestaurantReview(Long restaurantReviewId) {
        RestaurantReview restaurantReview = restaurantReviewRepository.findById(restaurantReviewId).orElseThrow(IllegalArgumentException::new);
        HashMap<String, Long> metadata = new HashMap<>();

        // Null-check: Restaurant
        if (restaurantReview.getRestaurant() != null) {
            metadata.put("fk", restaurantReview.getRestaurant().getId());
        }

        EmbeddingRequest request = new EmbeddingRequest(restaurantReviewId, restaurantReview.getComment(), metadata, new ArrayList<>());
        APIUtil.sendPostRequest(fastApiUrl + "/embedding?domain=restaurant_review", request);

        restaurantReview.setEmbeddedAt(LocalDateTime.now());
        restaurantReviewRepository.save(restaurantReview);
    }
    // RestaurantReview Batch Create
    private void embedRestaurantReviewBatch(List<Long> restaurantReviewIds) {
        List<RestaurantReview> reviews = restaurantReviewRepository.findAllById(restaurantReviewIds);

        List<EmbeddingRequest> requests = reviews.stream().map(review -> {
            HashMap<String, Long> metadata = new HashMap<>();
            // Null-check: Restaurant
            if (review.getRestaurant() != null) {
                metadata.put("fk", review.getRestaurant().getId());
            }
            return new EmbeddingRequest(review.getId(), review.getComment(), metadata, new ArrayList<>());
        }).toList();
        APIUtil.sendPostRequest(fastApiUrl + "/embedding/batch?domain=restaurant_review", requests);

        reviews.forEach(review -> review.setEmbeddedAt(LocalDateTime.now()));
        restaurantReviewRepository.saveAll(reviews);
    }

    @Transactional
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

    @Transactional
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
    private void deleteAccom(Long accomId) {
        Accommodation accommodation = accommodationRepository.findById(accomId).orElseThrow(IllegalArgumentException::new);
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=accom&pk=" + accomId);
        accommodation.setEmbeddedAt(null);
        accommodationRepository.save(accommodation);
    }
    // Accom Batch Delete
    private void deleteAccomBatch(List<Long> accomIds) {
        List<Accommodation> accommodations = accommodationRepository.findAllById(accomIds);
        String queryString = accomIds.stream().map(accomId -> "pks=" + accomId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding/batch?domain=accom&" + queryString);
        accommodations.forEach(accommodation -> accommodation.setEmbeddedAt(null));
        accommodationRepository.saveAll(accommodations);
    }
    // Place Delete
    private void deletePlace(Long placeId) {
        Place place = placeRepository.findById(placeId).orElseThrow(IllegalArgumentException::new);
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=place&pk=" + placeId);
        place.setEmbeddedAt(null);
        placeRepository.save(place);
    }
    // Place Batch Delete
    private void deletePlaceBatch(List<Long> placeIds) {
        List<Place> places = placeRepository.findAllById(placeIds);
        String queryString = placeIds.stream().map(placeId -> "pks=" + placeId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding/batch?domain=accom&" + queryString);
        places.forEach(place -> place.setEmbeddedAt(null));
        placeRepository.saveAll(places);
    }
    // Restaurant Delete
    private void deleteRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(IllegalArgumentException::new);
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=restaurant&pk=" + restaurantId);
        restaurant.setEmbeddedAt(null);
        restaurantRepository.save(restaurant);
    }
    // Restaurant Batch Delete
    private void deleteRestaurantBatch(List<Long> restaurantIds) {
        List<Restaurant> restaurants = restaurantRepository.findAllById(restaurantIds);
        String queryString = restaurantIds.stream().map(restaurantId -> "pks=" + restaurantId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding/batch?domain=restaurant&pk=" + queryString);
        restaurants.forEach(restaurant -> restaurant.setEmbeddedAt(null));
        restaurantRepository.saveAll(restaurants);
    }
    // AccomReview Delete
    private void deleteAccomReview(Long accomReviewId) {
        AccommodationReview review = accommodationReviewRepository.findById(accomReviewId).orElseThrow(IllegalArgumentException::new);
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=accom_review&pk=" + accomReviewId);
        review.setEmbeddedAt(null);
        accommodationReviewRepository.save(review);
    }
    // AccomReview Batch Delete
    private void deleteAccomReviewBatch(List<Long> accomReviewIds) {
        List<AccommodationReview> reviews = accommodationReviewRepository.findAllById(accomReviewIds);
        String queryString = accomReviewIds.stream().map(accomReviewId -> "pks=" + accomReviewId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding/batch?domain=accom_review&pk=" + queryString);
        reviews.forEach(review -> review.setEmbeddedAt(null));
        accommodationReviewRepository.saveAll(reviews);
    }
    // PlaceReview Delete
    private void deletePlaceReview(Long placeReviewId) {
        PlaceReview review = placeReviewRepository.findById(placeReviewId).orElseThrow(IllegalArgumentException::new);
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=place_review&pk=" + placeReviewId);
        review.setEmbeddedAt(null);
        placeReviewRepository.save(review);
    }
    // PlaceReview Batch Delete
    private void deletePlaceReviewBatch(List<Long> placeReviewIds) {
        List<PlaceReview> reviews = placeReviewRepository.findAllById(placeReviewIds);
        String queryString = placeReviewIds.stream().map(placeReviewId -> "pks=" + placeReviewId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding/batch?domain=place_review&pk=" + queryString);
        reviews.forEach(placeReview -> placeReview.setEmbeddedAt(null));
        placeReviewRepository.saveAll(reviews);
    }
    // RestaurantReview Delete
    private void deleteRestaurantReview(Long restaurantReviewId) {
        RestaurantReview review = restaurantReviewRepository.findById(restaurantReviewId).orElseThrow(IllegalArgumentException::new);
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding?domain=restaurant_review&pk=" + restaurantReviewId);
        review.setEmbeddedAt(null);
        restaurantReviewRepository.save(review);
    }
    // RestaurantReview Batch Delete
    private void deleteRestaurantReviewBatch(List<Long> restaurantReviewIds) {
        List<RestaurantReview> reviews = restaurantReviewRepository.findAllById(restaurantReviewIds);
        String queryString = restaurantReviewIds.stream().map(restaurantReviewId -> "pks=" + restaurantReviewId).collect(Collectors.joining("&"));
        APIUtil.sendDeleteRequest(fastApiUrl + "/embedding/batch?domain=restaurant_review&pk=" + queryString);
        reviews.forEach(review -> review.setEmbeddedAt(null));
        restaurantReviewRepository.saveAll(reviews);
    }
}