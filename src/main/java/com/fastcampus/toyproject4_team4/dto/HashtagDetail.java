package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.entity.Hashtag;

public record HashtagDetail(
        Long id,
        String content
) {
    public static HashtagDetail from(Hashtag hashtag) {
        return new HashtagDetail(
                hashtag.getId(),
                hashtag.getContent()
        );
    }
}
