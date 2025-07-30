package com.fastcampus.toyproject4_team4.repository;

import com.fastcampus.toyproject4_team4.entity.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario,String> {
    Optional<Scenario> findByCode(String code);
    Optional<Scenario> findByName(String name);
}
