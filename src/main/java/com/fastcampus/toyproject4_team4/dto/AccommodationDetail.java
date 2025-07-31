package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.entity.accomodation.Accommodation;
import com.fastcampus.toyproject4_team4.entity.accomodation.Amenity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record AccommodationDetail(
        Long id,
        String name,
        String type,
        Byte starRating,
        BigDecimal minPrice,
        Integer minCapacity,
        Integer maxCapacity,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String description,
        BigDecimal avgRating,
        LocalTime checkinTime,
        LocalTime checkoutTime,
        String url,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime embeddedAt,
        TravelStyleDetail travelStyle,
        List<AmenityDetail> amenities,
        List<HashtagDetail> hashtags
) {
    public static AccommodationDetail from(Accommodation accommodation) {
        return new AccommodationDetail(
                accommodation.getId(),
                accommodation.getName(),
                accommodation.getType(),
                accommodation.getStarRating(),
                accommodation.getMinPrice(),
                accommodation.getMinCapacity(),
                accommodation.getMaxCapacity(),
                accommodation.getAddress(),
                accommodation.getLatitude(),
                accommodation.getLongitude(),
                accommodation.getDescription(),
                accommodation.getAvgRating(),
                accommodation.getCheckinTime(),
                accommodation.getCheckoutTime(),
                accommodation.getUrl(),
                accommodation.getCreatedAt(),
                accommodation.getUpdatedAt(),
                accommodation.getEmbeddedAt(),
                TravelStyleDetail.from(accommodation.getTravelStyle()),
                accommodation.getAmenities().stream().map(AmenityDetail::from).toList(),
                accommodation.getHashtags().stream().map(HashtagDetail::from).toList()
        );
    }
}

record AmenityDetail(
        Long id,
        String name,
        String description,
        Byte isFree,
        BigDecimal price
) {
    static AmenityDetail from(Amenity amenity) {
        if (amenity == null) return null;
        return new AmenityDetail(
                amenity.getId(),
                amenity.getName(),
                amenity.getDescription(),
                amenity.getIsFree(),
                amenity.getPrice()
        );
    }
}