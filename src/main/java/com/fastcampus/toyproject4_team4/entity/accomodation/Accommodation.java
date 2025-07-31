package com.fastcampus.toyproject4_team4.entity.accomodation;

import com.fastcampus.toyproject4_team4.entity.Hashtag;
import com.fastcampus.toyproject4_team4.entity.TravelStyle;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "ACCOMMODATION")
public class Accommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ACCOMMODATION_ID")
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "STAR_RATING")
    private Byte starRating;

    @Column(name = "MIN_PRICE", precision = 19, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "MIN_CAPACITY")
    private Integer minCapacity;

    @Column(name = "MAX_CAPACITY")
    private Integer maxCapacity;

    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "LATITUDE", precision = 9, scale = 6, nullable = false)
    private BigDecimal latitude;

    @Column(name = "LONGITUDE", precision = 9, scale = 6, nullable = false)
    private BigDecimal longitude;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "AVG_RATING", precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(name = "CHECKIN_TIME")
    private LocalTime checkinTime;

    @Column(name = "CHECKOUT_TIME")
    private LocalTime checkoutTime;

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

    @ManyToMany
    @JoinTable(name = "ACCOMMODATION_AMENITY", joinColumns = @JoinColumn(name = "ACCOMMODATION_ID"), inverseJoinColumns = @JoinColumn(name = "AMENITY_ID"))
    private List<Amenity> amenities = new ArrayList<>();

    @OneToMany(mappedBy = "accommodation")
    private List<AccommodationReview> accommodationReviews = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "ACCOMMODATION_HASHTAG", joinColumns = @JoinColumn(name = "ACCOMMODATION_ID"), inverseJoinColumns = @JoinColumn(name = "HASHTAG_ID"))
    private List<Hashtag> hashtags = new ArrayList<>();

    // ✅ 추가된 필드 (중복 숙소 제거용)
    @Column(name = "google_place_id", length = 100, unique = true)
    private String googlePlaceId;

}