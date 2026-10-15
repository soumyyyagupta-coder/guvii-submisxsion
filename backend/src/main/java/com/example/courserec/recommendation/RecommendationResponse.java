package com.example.courserec.recommendation;

import java.util.List;

public record RecommendationResponse(
    CourseDto course,
    double score,
    ScoreComponents components,
    List<String> reasons
) {}
