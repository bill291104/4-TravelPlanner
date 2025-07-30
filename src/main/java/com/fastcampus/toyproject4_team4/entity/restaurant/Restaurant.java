package com.fastcampus.toyproject4_team4.entity.restaurant;

import com.fastcampus.toyproject4_team4.entity.Hashtag;
import com.fastcampus.toyproject4_team4.entity.TravelStyle;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "RESTAURANT")
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RESTAURANT_ID")
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "LATITUDE", precision = 9, scale = 6, nullable = false)
    private BigDecimal latitude;

    @Column(name = "LONGITUDE", precision = 9, scale = 6, nullable = false)
    private BigDecimal longitude;

    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "OP_TIME", columnDefinition = "TEXT")
    private String opTime;

    @Column(name = "TEL", length = 50)
    private String tel;

    @Column(name = "MICHELIN_STAR")
    private Byte michelinStar;

    @Column(name = "URL")
    private String url;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "EMBEDDED_AT")
    private LocalDateTime embeddedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRAVEL_STYLE_ID")
    private TravelStyle travelStyle;

    @OneToMany(mappedBy = "restaurant")
    private List<RestaurantReview> restaurantReviews = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant")
    private List<Menu> menus = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "RESTAURANT_HASHTAG", joinColumns = @JoinColumn(name = "RESTAURANT_ID"), inverseJoinColumns = @JoinColumn(name = "HASHTAG_ID"))
    private List<Hashtag> hashtags = new ArrayList<>();
}
