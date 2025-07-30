package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.Domains;

import java.util.List;

public record FastAPIDomainExtractRequest(
        String context,
        List<Domains> domains,
        String promptTemplate
) {
}
