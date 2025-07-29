package com.fastcampus.toyproject4_team4.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Prompt {
    @Id
    private String id;

    @OneToOne
    @JoinColumn(name = "SCENARIO_CODE")
    private Scenario scenario;

    @Column(name = "PROMPT_TEMPLATE", nullable = false)
    private String promptTemplate;
}
