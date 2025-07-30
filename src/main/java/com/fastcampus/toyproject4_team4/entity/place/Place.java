package com.fastcampus.toyproject4_team4.entity.place;

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
@Table(name = "PLACE")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PLACE_ID")
    private Long id;

    @Column(name = "PLACE_NAME", nullable = false)
    private String placeName;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "LATITUDE", precision = 9, scale = 6, nullable = false)
    private BigDecimal latitude;

    @Column(name = "LONGITUDE", precision = 9, scale = 6, nullable = false)
    private BigDecimal longitude;

    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "EMBEDDED_AT")
    private LocalDateTime embeddedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRAVEL_STYLE_ID")
    private TravelStyle travelStyle;

    @OneToMany(mappedBy = "place")
    private List<PlaceReview> placeReviews = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "PLACE_HASHTAG", joinColumns = @JoinColumn(name = "PLACE_ID"), inverseJoinColumns = @JoinColumn(name = "HASHTAG_ID"))
    private List<Hashtag> hashtags = new ArrayList<>();
}
