package com.fastcampus.toyproject4_team4.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor
public class Restaurant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pk;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private Double lat;
    
    @Column(nullable = false)
    private Double lon;
    
    @Column(name = "exp_cost")
    private Double expCost;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "cuisine_type")
    private String cuisineType;
    
    @Column(name = "signature_menu")
    private String signatureMenu;
    
    @Column(name = "operating_hours")
    private String operatingHours;
}

//생성 이유:
//  - JPA가 MySQL의 restaurants 테이블과 Java 객체를 매핑하기 위해 필요
//  - Python RestaurantDetail 스키마와 동일한 필드 구조로 데이터 호환성 확보
//  - findAllById(List<Integer> pks) 메서드가 List<Restaurant> 객체를 반환하도록 함