package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.Domains;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FastAPIKeywordExtractRequest(
        String context,
        @JsonProperty("target_domain") Domains targetDomain,
        @JsonProperty("prompt_template") String promptTemplate
) {
}
