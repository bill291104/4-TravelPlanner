package com.fastcampus.toyproject4_team4.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "PROMPT")
public class Prompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PROMPT_ID")
    private Long id;

    @OneToOne
    @JoinColumn(name = "SCENARIO_CODE")
    private Scenario scenario;

    @Column(name = "PROMPT_TEMPLATE", columnDefinition = "TEXT", nullable = false)
    private String promptTemplate;
}
