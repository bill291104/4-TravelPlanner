package com.fastcampus.toyproject4_team4.entity.place;

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
@Table(name = "place")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long id;

    @Column(name = "place_name")
    private String placeName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "lat", precision = 9, scale = 5)
    private BigDecimal lat;

    @Column(name = "lon", precision = 9, scale = 5)
    private BigDecimal lon;

    @Column(name = "address")
    private String address;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "embedded_at")
    private LocalDateTime embeddedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_style_id")
    private TravelStyle travelStyle;

    @OneToMany(mappedBy = "place")
    private List<PlaceReview> placeReviewList = new ArrayList<>();

    @JoinTable(name = "place_hashtag", joinColumns = @JoinColumn(name = "place_id"), inverseJoinColumns = @JoinColumn(name = "hashtag_id"))
    @ManyToMany
    private List<Hashtag> hashtags = new ArrayList<>();
}
