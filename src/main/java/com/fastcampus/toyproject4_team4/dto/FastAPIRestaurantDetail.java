package com.fastcampus.toyproject4_team4.dto;

import java.math.BigDecimal;
import java.util.List;

public record FastAPIRestaurantDetail(
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal exp_cost,
        String description,
        Long id,
        String address,
        String operating_hours,
        String tel,
        String michelin_star,
        String url,
        String travel_style,
        List<String> menus,
        List<String> hashtags
) {
    public static FastAPIRestaurantDetail from(RestaurantDetail restaurantDetail) {
        return new FastAPIRestaurantDetail(
                restaurantDetail.name(),
                restaurantDetail.latitude(),
                restaurantDetail.longitude(),
                BigDecimal.valueOf(restaurantDetail.menus() == null || restaurantDetail.menus().isEmpty() ? 0 : restaurantDetail.menus().getFirst().price()),
                restaurantDetail.description(),
                restaurantDetail.id(),
                restaurantDetail.address(),
                restaurantDetail.opTime(),
                restaurantDetail.tel(),
                restaurantDetail.michelinStar() == null ? "" : restaurantDetail.michelinStar().toString(),
                restaurantDetail.url(),
                restaurantDetail.travelStyle() == null ? "" : restaurantDetail.travelStyle().description(),
                restaurantDetail.menus().stream().map(RestaurantDetail.MenuDetail::name).toList(),
                restaurantDetail.hashtags().stream().map(HashtagDetail::content).toList()
        );
    }
}
