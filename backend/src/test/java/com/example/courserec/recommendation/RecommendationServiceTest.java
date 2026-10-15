package com.example.courserec.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository repository;

    private RecommendationScorer scorer;
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        scorer = new RecommendationScorer();
        service = new RecommendationService(repository, scorer);
    }

    @Test
    @DisplayName("Empty catalog returns an empty list")
    void testEmptyCatalogReturnsEmptyList() {
        when(repository.findUserProfile(1L)).thenReturn(UserProfile.empty(1L));
        when(repository.findAllCourses()).thenReturn(List.of());

        List<RecommendationResponse> result = service.getRecommendations(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return empty list for empty catalog");
    }

    @Test
    @DisplayName("Tie-break by course ID ascending when scores are identical")
    void testDeterministicTieBreaking() {
        UserProfile profile = UserProfile.empty(1L);

        // Two courses that will both score 0.0 with empty profile
        CourseDto courseA = new CourseDto(20L, "Course B", "Desc", "Cat", "BEGINNER", 10, "P", "url", List.of(), List.of(), List.of(), List.of(), false);
        CourseDto courseB = new CourseDto(5L, "Course A", "Desc", "Cat", "BEGINNER", 10, "P", "url", List.of(), List.of(), List.of(), List.of(), false);

        when(repository.findUserProfile(1L)).thenReturn(profile);
        when(repository.findAllCourses()).thenReturn(List.of(courseA, courseB));

        List<RecommendationResponse> result = service.getRecommendations(1L);

        assertEquals(2, result.size());
        assertEquals(result.get(0).score(), result.get(1).score(), "Scores should be equal");
        // Course with ID 5 must come before Course with ID 20
        assertEquals(5L, result.get(0).course().id(), "Lower course ID should break ties");
        assertEquals(20L, result.get(1).course().id());
    }

    @Test
    @DisplayName("Courses are ordered by descending score")
    void testDescendingScoreOrdering() {
        UserProfile profile = new UserProfile(
                1L,
                "Software Engineering",
                "INTERMEDIATE",
                "Frontend Engineer",
                List.of("React", "TypeScript"),
                List.of(new UserProfile.UserSkill("JavaScript", "INTERMEDIATE"))
        );

        CourseDto highMatch = new CourseDto(
                1L,
                "Advanced React and TypeScript",
                "Master React development with TypeScript",
                "Web Development",
                "INTERMEDIATE",
                30,
                "Coursera",
                "https://example.com/react",
                List.of("Frontend Engineer"),
                List.of("JavaScript"),
                List.of("React", "TypeScript", "Frontend"),
                List.of("React", "TypeScript"),
                true
        );

        CourseDto lowMatch = new CourseDto(
                2L,
                "Introduction to Rust",
                "Learn systems programming in Rust",
                "Systems",
                "BEGINNER",
                15,
                "edX",
                "https://example.com/rust",
                List.of("Systems Engineer"),
                List.of(),
                List.of("Rust", "Memory Safety"),
                List.of("Rust"),
                false
        );

        when(repository.findUserProfile(1L)).thenReturn(profile);
        when(repository.findAllCourses()).thenReturn(List.of(lowMatch, highMatch));

        List<RecommendationResponse> result = service.getRecommendations(1L);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).course().id(), "High match course should rank first");
        assertEquals(2L, result.get(1).course().id(), "Low match course should rank second");
        assertTrue(result.get(0).score() > result.get(1).score(), "First result score must be higher than second");
    }
}
