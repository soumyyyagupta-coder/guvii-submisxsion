package com.example.courserec.recommendation;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RecommendationService {

    private final RecommendationRepository repository;
    private final RecommendationScorer scorer;

    public RecommendationService(RecommendationRepository repository, RecommendationScorer scorer) {
        this.repository = repository;
        this.scorer = scorer;
    }

    public List<RecommendationResponse> getRecommendations(long userId) {
        UserProfile userProfile = repository.findUserProfile(userId);
        List<CourseDto> courses = repository.findAllCourses();

        if (courses == null || courses.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecommendationResponse> recommendations = new ArrayList<>(courses.size());
        for (CourseDto course : courses) {
            RecommendationResponse response = scorer.scoreAndExplain(userProfile, course);
            recommendations.add(response);
        }

        // Rank by descending score, then ascending course ID (deterministic tie-break)
        recommendations.sort((a, b) -> {
            int scoreCompare = Double.compare(b.score(), a.score());
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return Long.compare(a.course().id(), b.course().id());
        });

        return Collections.unmodifiableList(recommendations);
    }
}
