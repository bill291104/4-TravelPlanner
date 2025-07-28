package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.Domains;

public record FastAPIKeywordExtractRequest(
        String context,
        Domains targetDomain,
        String promptTemplate
) {
}
