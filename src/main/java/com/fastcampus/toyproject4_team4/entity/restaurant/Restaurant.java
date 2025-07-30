package com.fastcampus.toyproject4_team4.entity.restaurant;

import com.fastcampus.toyproject4_team4.entity.Hashtag;
import com.fastcampus.toyproject4_team4.entity.TravelStyle;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "restaurant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restaurant_id")
    private Long id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "lat", precision = 10, scale = 8)
    private BigDecimal lat;

    @Column(name = "lon", precision = 11, scale = 8)
    private BigDecimal lon;

    @Column(name = "address")
    private String address;

    @Column(name = "op_time", columnDefinition = "TEXT")
    private String opTime;

    @Column(name = "tel", length = 50)
    private String tel;

    @Column(name = "michelin_star")
    private Byte michelinStar;

    @Column(name = "url")
    private String url;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "embedded_at")
    private LocalDateTime embeddedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_style_id")
    private TravelStyle travelStyle;

    @OneToMany(mappedBy = "restaurant")
    private List<RestaurantReview> restaurantReviewList = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant")
    private List<RestaurantMenu> restaurantMenuList = new ArrayList<>();

    @JoinTable(name = "restaurant_hashtag", joinColumns = @JoinColumn(name = "restaurant_id"), inverseJoinColumns = @JoinColumn(name = "hashtag_id"))
    @ManyToMany
    private List<Hashtag> hashtags = new ArrayList<>();

    // ✅ 추가된 필드 (중복 식당 제거용)
    @Column(name = "google_place_id", length = 100, unique = true)
    private String googlePlaceId;
}
