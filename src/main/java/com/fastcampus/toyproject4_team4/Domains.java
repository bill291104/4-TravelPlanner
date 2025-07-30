package com.fastcampus.toyproject4_team4;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

public enum Domains {
    PLACE("place"),
    RESTAURANT("restaurant"),
    ACCOM("accom"),

    PLACE_REVIEW("place_review"),
    RESTAURANT_REVIEW("restaurant_review"),
    ACCOM_REVIEW("accom_review"),
    TRAVEL_STYLE("travel_style");

    // 1. 생성자로 받은 값을 저장할 final 변수 선언
    private final String name;

    // 2. 생성자에서 변수 초기화
    Domains(String name) {
        this.name = name;
    }

    // 3. 외부에서 값을 사용할 수 있도록 getter 추가
    // @JsonValue 어노테이션을 붙이면 Jackson이 JSON으로 변환할 때 이 메서드의 반환값을 사용합니다.
    // 즉, "PLACE"가 아닌 "place"로 직렬화됩니다.
    @JsonValue
    public String getName() {
        return name;
    }

    public static List<Domains> getMains() {
        return List.of(PLACE, RESTAURANT, ACCOM);
    }
}