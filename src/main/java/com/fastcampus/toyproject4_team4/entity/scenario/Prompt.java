package com.fastcampus.toyproject4_team4.entity.scenario;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prompt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Prompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prompt_id")
    private Long id;

    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private String promptTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario")
    private Scenario scenario;
}
