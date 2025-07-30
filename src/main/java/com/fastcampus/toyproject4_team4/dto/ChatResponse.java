package com.fastcampus.toyproject4_team4.dto;

public record ChatResponse(
        String reply,
        boolean isFull    // 모든 도메인에 데이터가 들어 있는가
) {
}
