package com.fastcampus.toyproject4_team4.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Prompt {
    @Id
    private String id;

    @OneToOne(mappedBy = "CODE")
    private Scenario scenario;

    @Column(name = "PROMPT_TEMPLATE", nullable = false)
    private String promptTemplate;
}
