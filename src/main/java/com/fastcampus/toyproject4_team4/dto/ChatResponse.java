package com.fastcampus.toyproject4_team4.dto;

public record ChatResponse(
//      빈 도메인 체크한 후 재질문 하는 문자열을 넘겨줄 클래스

        String reply,
        boolean followUp    // 기본값 false, 재질문이면 true
) {
}
