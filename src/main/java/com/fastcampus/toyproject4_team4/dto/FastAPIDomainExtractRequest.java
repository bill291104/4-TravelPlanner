package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.Domains;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FastAPIDomainExtractRequest(
        String context,
        @JsonProperty("domains") List<String> domains,
        @JsonProperty("prompt_template") String promptTemplate
) {
}
