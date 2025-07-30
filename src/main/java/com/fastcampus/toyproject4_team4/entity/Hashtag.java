package com.fastcampus.toyproject4_team4.entity;

import com.fastcampus.toyproject4_team4.entity.accomodation.Accommodation;
import com.fastcampus.toyproject4_team4.entity.place.Place;
import com.fastcampus.toyproject4_team4.entity.restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "HASHTAG")
public class Hashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HASHTAG_ID")
    private Long id;

    @Column(name = "CONTENT", nullable = false)
    private String content;

    @ManyToMany(mappedBy = "hashtags")
    private List<Accommodation> accommodations = new ArrayList<>();

    @ManyToMany(mappedBy = "hashtags")
    private List<Place> places = new ArrayList<>();

    @ManyToMany(mappedBy = "hashtags")
    private List<Restaurant> restaurants = new ArrayList<>();

}
