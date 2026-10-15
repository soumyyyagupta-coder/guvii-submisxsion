package com.example.courserec.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationScorerTest {

    private RecommendationScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new RecommendationScorer();
    }

    @Test
    @DisplayName("Complete profile with perfect match achieves near 100 score")
    void testPerfectMatchScoring() {
        UserProfile profile = new UserProfile(
                1L,
                "Computer Science",
                "INTERMEDIATE",
                "Machine Learning Engineer",
                List.of("Machine Learning", "Python"),
                List.of(
                        new UserProfile.UserSkill("Python", "INTERMEDIATE"),
                        new UserProfile.UserSkill("Linear Algebra", "INTERMEDIATE")
                )
        );

        CourseDto course = new CourseDto(
                101L,
                "Applied Machine Learning in Python",
                "Comprehensive course covering ML algorithms with Python",
                "Data Science",
                "INTERMEDIATE",
                40,
                "Coursera",
                "https://example.com/ml-python",
                List.of("Machine Learning Engineer", "Data Scientist"),
                List.of("Python Basics", "Linear Algebra"),
                List.of("Machine Learning", "Python", "Data Science"),
                List.of("Python", "Scikit-Learn", "Model Evaluation"),
                true
        );

        RecommendationResponse response = scorer.scoreAndExplain(profile, course);

        assertNotNull(response);
        assertEquals(101L, response.course().id());
        assertTrue(response.score() >= 80.0, "Score should be high for matching profile");
        assertTrue(response.components().interest() >= 80.0, "Interest component should be high");
        assertEquals(100.0, response.components().career(), "Career component should be 100 for exact career goal match");
        assertTrue(response.components().skill() >= 50.0, "Skill component should reflect matching skills and prereqs");
        assertEquals(100.0, response.components().level(), "Level component should be 100 for matching proficiency");

        // Verify formula: 0.40 * interest + 0.30 * career + 0.20 * skill + 0.10 * level
        double expected = RecommendationScorer.roundToOneDecimal(
                0.40 * response.components().interest() +
                0.30 * response.components().career() +
                0.20 * response.components().skill() +
                0.10 * response.components().level()
        );
        assertEquals(expected, response.score(), 0.1);

        assertFalse(response.reasons().isEmpty(), "Reasons must be provided");
        assertTrue(response.reasons().stream().anyMatch(r -> r.toLowerCase().contains("interest")));
        assertTrue(response.reasons().stream().anyMatch(r -> r.toLowerCase().contains("career")));
    }

    @Test
    @DisplayName("Empty profile scores 0 across all components without division by zero")
    void testEmptyProfileNoDivisionByZero() {
        UserProfile emptyProfile = UserProfile.empty(2L);

        CourseDto course = new CourseDto(
                202L,
                "Web Development Bootcamp",
                "Learn full stack development",
                "Web Development",
                "BEGINNER",
                60,
                "Udemy",
                "https://example.com/web-dev",
                List.of("Full Stack Developer"),
                List.of("Basic HTML"),
                List.of("HTML", "CSS", "JavaScript"),
                List.of("HTML", "CSS", "JavaScript"),
                false
        );

        RecommendationResponse response = scorer.scoreAndExplain(emptyProfile, course);

        assertNotNull(response);
        assertEquals(0.0, response.components().interest());
        assertEquals(0.0, response.components().career());
        assertEquals(0.0, response.components().skill());
        assertEquals(0.0, response.components().level());
        assertEquals(0.0, response.score());
        assertFalse(response.reasons().isEmpty(), "Should have a fallback reason or note prerequisites");
    }

    @Test
    @DisplayName("Case-insensitivity and whitespace trimming for tokens")
    void testTokenNormalization() {
        UserProfile profile = new UserProfile(
                3L,
                "Self-taught",
                "  beginner  ",
                "  DATA SCIENTIST  ",
                List.of("  pYtHoN  ", "DATA ANALYSIS"),
                List.of(new UserProfile.UserSkill("  python  ", "BEGINNER"))
        );

        CourseDto course = new CourseDto(
                303L,
                "Python for Data Analysis",
                "Introduction to Python and Pandas",
                "Data Science",
                "BEGINNER",
                20,
                "edX",
                "https://example.com/python-da",
                List.of("Data Scientist"),
                List.of("None"),
                List.of("Python", "Data Analysis"),
                List.of("Python", "Pandas"),
                true
        );

        RecommendationResponse response = scorer.scoreAndExplain(profile, course);

        assertTrue(response.components().interest() > 0.0);
        assertEquals(100.0, response.components().career(), "Should match career goal case-insensitively");
        assertTrue(response.components().skill() > 0.0, "Should match skill case-insensitively");
        assertEquals(100.0, response.components().level(), "Should match level case-insensitively");
    }

    @Test
    @DisplayName("Difficulty level matching distances: same, all levels, 1 step, 2 steps")
    void testDifficultyLevelDistances() {
        UserProfile beginnerProfile = new UserProfile(4L, "", "BEGINNER", "", List.of(), List.of());
        UserProfile advancedProfile = new UserProfile(5L, "", "ADVANCED", "", List.of(), List.of());

        CourseDto beginnerCourse = new CourseDto(1L, "T1", "", "", "BEGINNER", 10, "", "", List.of(), List.of(), List.of(), List.of(), false);
        CourseDto interCourse = new CourseDto(2L, "T2", "", "", "INTERMEDIATE", 10, "", "", List.of(), List.of(), List.of(), List.of(), false);
        CourseDto advCourse = new CourseDto(3L, "T3", "", "", "ADVANCED", 10, "", "", List.of(), List.of(), List.of(), List.of(), false);
        CourseDto allLevelsCourse = new CourseDto(4L, "T4", "", "", "ALL_LEVELS", 10, "", "", List.of(), List.of(), List.of(), List.of(), false);

        assertEquals(100.0, scorer.calculateLevelScore(beginnerProfile, beginnerCourse, new java.util.ArrayList<>()));
        assertEquals(90.0, scorer.calculateLevelScore(beginnerProfile, allLevelsCourse, new java.util.ArrayList<>()));
        assertEquals(50.0, scorer.calculateLevelScore(beginnerProfile, interCourse, new java.util.ArrayList<>()));
        assertEquals(15.0, scorer.calculateLevelScore(beginnerProfile, advCourse, new java.util.ArrayList<>()));

        assertEquals(100.0, scorer.calculateLevelScore(advancedProfile, advCourse, new java.util.ArrayList<>()));
        assertEquals(50.0, scorer.calculateLevelScore(advancedProfile, interCourse, new java.util.ArrayList<>()));
        assertEquals(15.0, scorer.calculateLevelScore(advancedProfile, beginnerCourse, new java.util.ArrayList<>()));
    }

    @Test
    @DisplayName("Generic job titles do not make unrelated careers match")
    void testGenericRoleWordDoesNotCreateCareerMatch() {
        UserProfile profile = new UserProfile(6L, "", "BEGINNER", "Machine Learning Engineer", List.of(), List.of());
        CourseDto cloudCourse = new CourseDto(5L, "Cloud Architecture", "", "Cloud Computing", "BEGINNER",
                20, "", "", List.of("Cloud Engineer"), List.of(), List.of("AWS"), List.of(), false);

        RecommendationResponse response = scorer.scoreAndExplain(profile, cloudCourse);

        assertEquals(0.0, response.components().career());
        assertTrue(response.reasons().stream().noneMatch(reason -> reason.contains("career goal")));
    }
}
