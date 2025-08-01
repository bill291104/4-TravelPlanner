package com.fastcampus.toyproject4_team4.dto;

import com.fastcampus.toyproject4_team4.Domains;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TravelPlanResponse(
        @JsonProperty("final_plan") TravelPlan finalPlan
) {
}

record TravelPlan(
        @JsonProperty("plan_title") String planTitle,
        @JsonProperty("destination") String destination,
        @JsonProperty("total_days") Integer total_days,
        @JsonProperty("daily_plans") List<DailyPlan> dailyPlans
) {}

record DailyPlan(
        @JsonProperty("day")Integer day,
        @JsonProperty("date")String date,
        @JsonProperty("description")String description,
        @JsonProperty("events")List<Event> events
) {}
record Event(
        @JsonProperty("time") String time,
        @JsonProperty("domain") Domains domain,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("address") String address,
        @JsonProperty("estimated_cost") String estimatedCost
) {}