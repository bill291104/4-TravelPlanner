package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.Domains;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FastAPISimilaritySearchRequest(
        String context,
        @JsonProperty("target_domain") Domains targetDomain,
        @JsonProperty("target_keywords") List<String> targetKeywords,
        @JsonProperty("prompt_template") String promptTemplate
) {
}
