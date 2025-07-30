package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.Domains;

import java.util.List;

public record DetailResponse(
        Domains domain,
        List<?> details
) {
    public static DetailResponse from(Domains domain, List<?> details) {
        return new DetailResponse(domain, details);
    }
}
