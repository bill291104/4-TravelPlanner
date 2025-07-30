package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.Domains;

import java.util.List;

public record TravelPlanResponse(
        TravelPlan finalPlan
) {
}

record TravelPlan(
        String planTitle,
        String destination,
        Integer total_days,
        List<DailyPlan> dailyPlans
) {}

record DailyPlan(
        Integer day,
        String date,
        String description,
        List<Event> events
) {}
record Event(
        String time,
        Domains domain,
        String name,
        String description,
        String address,
        String estimatedCost
) {}