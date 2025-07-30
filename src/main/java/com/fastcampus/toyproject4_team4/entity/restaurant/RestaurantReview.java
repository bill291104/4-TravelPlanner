package com.fastcampus.toyproject4_team4.entity.restaurant;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "RESTAURANT_REVIEW")
public class RestaurantReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REVIEW_ID")
    private Long id;

    @Column(name = "RATING")
    private Byte rating;

    @Column(name = "COMMENT", columnDefinition = "TEXT", nullable = false)
    private String comment;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "EMBEDDED_AT")
    private LocalDateTime embeddedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESTAURANT_ID")
    private Restaurant restaurant;
}
