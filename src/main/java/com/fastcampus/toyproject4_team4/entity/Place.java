package com.fastcampus.toyproject4_team4.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "places")
@Getter
@Setter
@NoArgsConstructor
public class Place {
    
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
    
    private String category;
    
    @Column(name = "operating_hours")
    private String operatingHours;
    
    @Column(name = "required_time")
    private String requiredTime;
}

// 생성 이유:
//  - JPA가 MySQL의 places 테이블과 Java 객체를 매핑하기 위해 필요
//  - @Entity: JPA에게 이 클래스가 데이터베이스 테이블과 연결됨을 알림
//  - @Id: pk 필드가 Primary Key임을 명시
//  - 각 @Column: 데이터베이스 컬럼명과 Java 필드명이 다를 때 매핑