package com.fastcampus.toyproject4_team4.entity;

import com.fastcampus.toyproject4_team4.entity.accomodation.Accommodation;
import com.fastcampus.toyproject4_team4.entity.place.Place;
import com.fastcampus.toyproject4_team4.entity.restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hashtag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hashtag_id")
    private Long id;

    @Column(name = "content", length = 255, unique = true, nullable = false)
    private String content;

    @ManyToMany(mappedBy = "hashtags")
    private List<Accommodation> accoms = new ArrayList<>();

    @ManyToMany(mappedBy = "hashtags")
    private List<Place> places = new ArrayList<>();

    @ManyToMany(mappedBy = "hashtags")
    private List<Restaurant> restaurants = new ArrayList<>();

}
