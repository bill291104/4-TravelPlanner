package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.entity.restaurant.Menu;
import com.fastcampus.toyproject4_team4.entity.restaurant.Restaurant;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RestaurantDetail(
        Long id,
        String name,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        String opTime,
        String tel,
        Byte michelinStar,
        String url,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updatedAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime embeddedAt,
        TravelStyleDetail travelStyle,
        List<MenuDetail> menus,
        List<HashtagDetail> hashtags
) {
    public static RestaurantDetail from(Restaurant restaurant) {
        return new RestaurantDetail(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.getAddress(),
                restaurant.getOpTime(),
                restaurant.getTel(),
                restaurant.getMichelinStar(),
                restaurant.getUrl(),
                restaurant.getCreatedAt(),
                restaurant.getUpdatedAt(),
                restaurant.getEmbeddedAt(),
                TravelStyleDetail.from(restaurant.getTravelStyle()),
                restaurant.getMenus().stream().map(MenuDetail::from).toList(),
                restaurant.getHashtags().stream().map(HashtagDetail::from).toList()
        );
    }

    record MenuDetail(
            Long id,
            String name,
            Integer price
    ) {
        static MenuDetail from(Menu menu) {
            return new MenuDetail(
                    menu.getId(),
                    menu.getName(),
                    menu.getPrice()
            );
        }
    }
}
