package com.example.courserec.recommendation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class RecommendationRepository {

    private final JdbcTemplate jdbcTemplate;

    public RecommendationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserProfile findUserProfile(long userId) {
        String userSql = "SELECT academic_background, proficiency_level, career_goal FROM users WHERE id = ?";
        List<UserProfile> profiles = jdbcTemplate.query(userSql, (rs, rowNum) -> {
            String academic = rs.getString("academic_background");
            String level = rs.getString("proficiency_level");
            String career = rs.getString("career_goal");
            return new UserProfile(userId, academic, level, career, new ArrayList<>(), new ArrayList<>());
        }, userId);

        UserProfile base = profiles.isEmpty() ? UserProfile.empty(userId) : profiles.get(0);

        String interestSql = "SELECT interest_name FROM user_interests WHERE user_id = ? ORDER BY id ASC";
        List<String> interests = jdbcTemplate.query(interestSql, (rs, rowNum) -> rs.getString("interest_name"), userId);

        String skillSql = "SELECT skill_name, proficiency FROM user_skills WHERE user_id = ? ORDER BY id ASC";
        List<UserProfile.UserSkill> skills = jdbcTemplate.query(skillSql, (rs, rowNum) ->
                new UserProfile.UserSkill(rs.getString("skill_name"), rs.getString("proficiency")), userId);

        return new UserProfile(
                userId,
                base.academicBackground(),
                base.proficiencyLevel(),
                base.careerGoal(),
                interests != null ? interests : Collections.emptyList(),
                skills != null ? skills : Collections.emptyList()
        );
    }

    public List<CourseDto> findAllCourses() {
        String courseSql = """
                SELECT id, title, description, category, difficulty_level, duration_hours,
                       platform, course_url, career_goals, prerequisites, is_demo
                FROM courses
                ORDER BY id ASC
                """;

        String topicSql = "SELECT course_id, topic_name FROM course_topics ORDER BY id ASC";
        String skillSql = "SELECT course_id, skill_name FROM course_skills ORDER BY id ASC";

        Map<Long, List<String>> topicsByCourse = new HashMap<>();
        jdbcTemplate.query(topicSql, rs -> {
            long courseId = rs.getLong("course_id");
            String topic = rs.getString("topic_name");
            if (topic != null && !topic.isBlank()) {
                topicsByCourse.computeIfAbsent(courseId, k -> new ArrayList<>()).add(topic.trim());
            }
        });

        Map<Long, List<String>> skillsByCourse = new HashMap<>();
        jdbcTemplate.query(skillSql, rs -> {
            long courseId = rs.getLong("course_id");
            String skill = rs.getString("skill_name");
            if (skill != null && !skill.isBlank()) {
                skillsByCourse.computeIfAbsent(courseId, k -> new ArrayList<>()).add(skill.trim());
            }
        });

        return jdbcTemplate.query(courseSql, (rs, rowNum) -> {
            long id = rs.getLong("id");
            String title = rs.getString("title");
            String description = rs.getString("description");
            String category = rs.getString("category");
            String difficultyLevel = rs.getString("difficulty_level");
            int durationHours = rs.getInt("duration_hours");
            String platform = rs.getString("platform");
            String courseUrl = rs.getString("course_url");
            String careerGoalsRaw = rs.getString("career_goals");
            String prerequisitesRaw = rs.getString("prerequisites");
            boolean demo = rs.getBoolean("is_demo");

            List<String> careerGoals = parseList(careerGoalsRaw);
            List<String> prerequisites = parseList(prerequisitesRaw);
            List<String> topics = topicsByCourse.getOrDefault(id, Collections.emptyList());
            List<String> skills = skillsByCourse.getOrDefault(id, Collections.emptyList());

            return new CourseDto(
                    id,
                    title != null ? title : "",
                    description != null ? description : "",
                    category != null ? category : "",
                    difficultyLevel != null ? difficultyLevel : "",
                    durationHours,
                    platform != null ? platform : "",
                    courseUrl != null ? courseUrl : "",
                    careerGoals,
                    prerequisites,
                    topics,
                    skills,
                    demo
            );
        });
    }

    public static List<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String inner = trimmed.substring(1, trimmed.length() - 1).trim();
            if (inner.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> items = new ArrayList<>();
            for (String part : inner.split(",")) {
                String clean = part.trim().replaceAll("^\"|\"$|^'|'$", "").trim();
                if (!clean.isEmpty()) {
                    items.add(clean);
                }
            }
            return Collections.unmodifiableList(items);
        }
        List<String> items = new ArrayList<>();
        for (String part : trimmed.split("[,;\\|\\n]+")) {
            String clean = part.trim();
            if (!clean.isEmpty()) {
                items.add(clean);
            }
        }
        return Collections.unmodifiableList(items);
    }
}
