package com.fastcampus.toyproject4_team4.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Entity
@Table(name = "accommodations")
@Getter
@Setter
@NoArgsConstructor
public class Accommodation {
    
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
    
    @Column(name = "accom_type", nullable = false)
    private String accomType;
    
    private String grade;
    
    @Column(columnDefinition = "JSON")
    private String amenities;
    
    @Column(name = "check_in_out_time")
    private String checkInOutTime;
    
    @Column(name = "booking_url")
    private String bookingUrl;
}