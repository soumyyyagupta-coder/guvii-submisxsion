package com.example.courserec.catalog;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CourseResponse(
        Long id,
        String title,
        String description,
        String category,
        String difficultyLevel,
        Integer durationHours,
        String platform,
        String courseUrl,
        List<String> careerGoals,
        List<String> prerequisites,
        List<String> topics,
        List<String> skills,
        @JsonProperty("demo") boolean demo
) {
    public CourseResponse {
        careerGoals = careerGoals != null ? List.copyOf(careerGoals) : List.of();
        prerequisites = prerequisites != null ? List.copyOf(prerequisites) : List.of();
        topics = topics != null ? List.copyOf(topics) : List.of();
        skills = skills != null ? List.copyOf(skills) : List.of();
    }
}
