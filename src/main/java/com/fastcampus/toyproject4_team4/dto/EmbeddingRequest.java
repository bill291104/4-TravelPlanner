package com.fastcampus.toyproject4_team4.dto;

import java.util.List;
import java.util.Map;

public record EmbeddingRequest(
        Integer pk,
        String content,
        Map<String, String> metadata,
        List<String> relatedContents
) {}
