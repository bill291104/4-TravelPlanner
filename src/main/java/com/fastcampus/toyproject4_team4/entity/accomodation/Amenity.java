package com.fastcampus.toyproject4_team4.entity.accomodation;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "AMENITY")
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AMENITY_ID")
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "IS_FREE")
    private Byte isFree;

    @Column(name = "PRICE", precision = 19, scale = 2)
    private BigDecimal price;

    @ManyToMany(mappedBy = "amenities")
    private List<Accommodation> accommodations = new ArrayList<>();
}