package com.example.courserec.recommendation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CourseDto(
    long id,
    String title,
    String description,
    String category,
    String difficultyLevel,
    int durationHours,
    String platform,
    String courseUrl,
    List<String> careerGoals,
    List<String> prerequisites,
    List<String> topics,
    List<String> skills,
    @JsonProperty("demo") boolean demo
) {}
