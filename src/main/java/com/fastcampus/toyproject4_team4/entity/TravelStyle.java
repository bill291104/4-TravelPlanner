package com.fastcampus.toyproject4_team4.entity;

import com.fastcampus.toyproject4_team4.entity.accomodation.Accommodation;
import com.fastcampus.toyproject4_team4.entity.place.Place;
import com.fastcampus.toyproject4_team4.entity.restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "TRAVEL_STYLE")
public class TravelStyle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRAVEL_STYLE_ID")
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "REC_SEASON")
    private String recSeason;

    @Column(name = "AGE_GROUP")
    private String ageGroup;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "EMBEDDED_AT")
    private LocalDateTime embeddedAt;

    @OneToMany(mappedBy = "travelStyle")
    private List<Place> places = new ArrayList<>();

    @OneToMany(mappedBy = "travelStyle")
    private List<Accommodation> accommodations = new ArrayList<>();

    @OneToMany(mappedBy = "travelStyle")
    private List<Restaurant> restaurants = new ArrayList<>();
}
