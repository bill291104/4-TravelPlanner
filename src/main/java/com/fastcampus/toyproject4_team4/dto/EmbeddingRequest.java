package com.fastcampus.toyproject4_team4.dto;

import java.util.List;
import java.util.Map;

public record EmbeddingRequest(
        Long pk,
        String content,
        Map<String, Long> metadata,
        List<String> relatedContents
) {}
