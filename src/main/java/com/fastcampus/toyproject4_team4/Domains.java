package com.fastcampus.toyproject4_team4;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

public enum Domains {
    PLACE("place"),
    RESTAURANT("restaurant"),
    ACCOM("accom"),

    PLACE_REVIEW("place_review"),
    RESTAURANT_REVIEW("restaurant_review"),
    ACCOM_REVIEW("accom_review");

    private final String name;

    Domains(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    public static List<Domains> getMains() {
        return List.of(PLACE, RESTAURANT, ACCOM);
    }
}