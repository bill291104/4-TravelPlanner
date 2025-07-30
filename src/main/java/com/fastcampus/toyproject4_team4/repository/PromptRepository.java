package com.fastcampus.toyproject4_team4.repository;

import com.fastcampus.toyproject4_team4.entity.Prompt;
import com.fastcampus.toyproject4_team4.entity.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromptRepository extends JpaRepository<Prompt, Long> {
    Optional<Prompt> findByScenario(Scenario scenario);
}
