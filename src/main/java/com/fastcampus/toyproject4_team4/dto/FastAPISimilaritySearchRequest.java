package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.Domains;

import java.util.List;

public record FastAPISimilaritySearchRequest(
        String context,
        Domains targetDomain,
        List<String> targetKeywords,
        String promptTemplate
) {
}
