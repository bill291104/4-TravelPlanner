package com.fastcampus.toyproject4_team4.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "SCENARIO")
public class Scenario {

    @Id
    @Column(name = "CODE")
    private String code;

    @Column(name = "NAME", nullable = false, unique = true)
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @OneToOne(mappedBy = "scenario")
    private Prompt prompt;
}
