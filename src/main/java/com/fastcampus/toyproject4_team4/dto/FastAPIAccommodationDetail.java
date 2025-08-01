package com.fastcampus.toyproject4_team4.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record FastAPIAccommodationDetail(
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal exp_cost,
        String description,
        Long id,
        String type,
        Byte star_rating,
        BigDecimal min_price,
        Integer min_capacity,
        Integer max_capacity,
        String address,
        BigDecimal avg_rating,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime checkin_time,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime checkout_time,
        String url,
        String travel_style,
        List<String> amenities,
        List<String> hashtags
) {
    public static FastAPIAccommodationDetail from(AccommodationDetail accommodationDetail) {
        return new FastAPIAccommodationDetail(
                accommodationDetail.name(),
                accommodationDetail.latitude(),
                accommodationDetail.longitude(),
                accommodationDetail.minPrice(),
                accommodationDetail.description(),
                accommodationDetail.id(),
                accommodationDetail.type(),
                accommodationDetail.starRating(),
                accommodationDetail.minPrice(),
                accommodationDetail.minCapacity(),
                accommodationDetail.maxCapacity(),
                accommodationDetail.address(),
                accommodationDetail.avgRating(),
                accommodationDetail.checkinTime(),
                accommodationDetail.checkoutTime(),
                accommodationDetail.url(),
                accommodationDetail.travelStyle() == null ? "" : accommodationDetail.travelStyle().description(),
                accommodationDetail.amenities().stream().map(AmenityDetail::name).toList(),
                accommodationDetail.hashtags().stream().map(HashtagDetail::content).toList()
        );
    }
}
