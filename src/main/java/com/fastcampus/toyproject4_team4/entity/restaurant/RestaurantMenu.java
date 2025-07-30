package com.fastcampus.toyproject4_team4.entity.restaurant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "restaurant_menu")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantMenu {

    @Id
    @Column(name = "restaurant_menu_id")
    private Integer id;

    @Column(name = "restaurant_menu_name", length = 100)
    private String restaurantMenuName;

    @Column(name = "restaurant_menu_price")
    private Integer restaurantMenuPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;
}
