package com.example.courserec.catalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<CourseRow> COURSE_ROW_MAPPER = (rs, rowNum) -> new CourseRow(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("category"),
            rs.getString("difficulty_level"),
            rs.getObject("duration_hours") != null ? rs.getInt("duration_hours") : null,
            rs.getString("platform"),
            rs.getString("course_url"),
            rs.getString("career_goals"),
            rs.getString("prerequisites"),
            rs.getBoolean("is_demo")
    );

    public CatalogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CourseResponse> searchCourses(String q, String topic, String level, String platform, Integer maxDurationHours) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT c.id, c.title, c.description, c.category, c.difficulty_level,
                                c.duration_hours, c.platform, c.course_url, c.career_goals,
                                c.prerequisites, c.is_demo
                FROM courses c
                WHERE 1=1\s""");

        List<Object> params = new ArrayList<>();

        if (q != null && !q.trim().isEmpty()) {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            sql.append("""
                    AND (
                        LOWER(c.title) LIKE ?
                        OR LOWER(c.description) LIKE ?
                        OR LOWER(c.category) LIKE ?
                        OR LOWER(c.platform) LIKE ?
                        OR LOWER(c.career_goals) LIKE ?
                        OR EXISTS (SELECT 1 FROM course_topics ct WHERE ct.course_id = c.id AND LOWER(ct.topic_name) LIKE ?)
                        OR EXISTS (SELECT 1 FROM course_skills cs WHERE cs.course_id = c.id AND LOWER(cs.skill_name) LIKE ?)
                    )\s""");
            for (int i = 0; i < 7; i++) {
                params.add(pattern);
            }
        }

        if (topic != null && !topic.trim().isEmpty() && !topic.trim().equalsIgnoreCase("all")) {
            String topicPattern = "%" + topic.trim().toLowerCase() + "%";
            sql.append("AND EXISTS (SELECT 1 FROM course_topics ct WHERE ct.course_id = c.id AND LOWER(ct.topic_name) LIKE ?) ");
            params.add(topicPattern);
        }

        if (level != null && !level.trim().isEmpty() && !level.trim().equalsIgnoreCase("all")) {
            sql.append("AND LOWER(c.difficulty_level) = ? ");
            params.add(level.trim().toLowerCase());
        }

        if (platform != null && !platform.trim().isEmpty() && !platform.trim().equalsIgnoreCase("all")) {
            String platformPattern = "%" + platform.trim().toLowerCase() + "%";
            sql.append("AND LOWER(c.platform) LIKE ? ");
            params.add(platformPattern);
        }

        if (maxDurationHours != null && maxDurationHours > 0) {
            sql.append("AND c.duration_hours <= ? ");
            params.add(maxDurationHours);
        }

        sql.append("ORDER BY c.id ASC");

        List<CourseRow> rows = jdbcTemplate.query(sql.toString(), COURSE_ROW_MAPPER, params.toArray());
        return toCourseResponses(rows);
    }

    public CourseResponse getCourseById(long id) {
        String sql = """
                SELECT id, title, description, category, difficulty_level,
                       duration_hours, platform, course_url, career_goals,
                       prerequisites, is_demo
                FROM courses
                WHERE id = ?
                """;

        List<CourseRow> rows = jdbcTemplate.query(sql, COURSE_ROW_MAPPER, id);
        if (rows.isEmpty()) {
            throw new CatalogNotFoundException("Course not found with id: " + id);
        }

        return toCourseResponses(rows).get(0);
    }

    public boolean courseExists(long id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM courses WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    public List<CourseResponse> getCoursesByIdsPreservingOrder(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyList();
        }

        String inSql = String.join(",", Collections.nCopies(courseIds.size(), "?"));
        String sql = "SELECT id, title, description, category, difficulty_level, duration_hours, platform, course_url, career_goals, prerequisites, is_demo FROM courses WHERE id IN (" + inSql + ")";

        List<CourseRow> rows = jdbcTemplate.query(
                sql,
                ps -> {
                    for (int i = 0; i < courseIds.size(); i++) {
                        ps.setLong(i + 1, courseIds.get(i));
                    }
                },
                COURSE_ROW_MAPPER
        );

        List<CourseResponse> responses = toCourseResponses(rows);
        Map<Long, CourseResponse> responseMap = responses.stream()
                .collect(Collectors.toMap(CourseResponse::id, r -> r));

        List<CourseResponse> ordered = new ArrayList<>(courseIds.size());
        for (Long id : courseIds) {
            CourseResponse cr = responseMap.get(id);
            if (cr != null) {
                ordered.add(cr);
            }
        }
        return ordered;
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        validateCourseRequest(request);

        String sql = """
                INSERT INTO courses (
                    title, description, category, difficulty_level,
                    duration_hours, platform, course_url, career_goals,
                    prerequisites, is_demo
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, request.title().trim());
            ps.setString(2, request.description() != null ? request.description().trim() : "");
            ps.setString(3, request.category() != null ? request.category().trim() : "General");
            ps.setString(4, request.difficultyLevel() != null ? request.difficultyLevel().trim() : "Beginner");
            if (request.durationHours() != null) {
                ps.setInt(5, request.durationHours());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setString(6, request.platform() != null ? request.platform().trim() : "Online");
            ps.setString(7, request.courseUrl() != null ? request.courseUrl().trim() : "");
            ps.setString(8, formatList(request.careerGoals()));
            ps.setString(9, formatList(request.prerequisites()));
            ps.setBoolean(10, Boolean.TRUE.equals(request.demo()));
            return ps;
        }, keyHolder);

        long courseId;
        Number key = keyHolder.getKey();
        if (key != null) {
            courseId = key.longValue();
        } else if (keyHolder.getKeys() != null && !keyHolder.getKeys().isEmpty()) {
            Object val = keyHolder.getKeys().values().iterator().next();
            courseId = ((Number) val).longValue();
        } else {
            throw new IllegalStateException("Failed to obtain generated id for course");
        }

        insertTopics(courseId, request.topics());
        insertSkills(courseId, request.skills());

        return getCourseById(courseId);
    }

    @Transactional
    public CourseResponse updateCourse(long id, CourseRequest request) {
        validateCourseRequest(request);

        if (!courseExists(id)) {
            throw new CatalogNotFoundException("Course not found with id: " + id);
        }

        String sql = """
                UPDATE courses SET
                    title = ?,
                    description = ?,
                    category = ?,
                    difficulty_level = ?,
                    duration_hours = ?,
                    platform = ?,
                    course_url = ?,
                    career_goals = ?,
                    prerequisites = ?,
                    is_demo = ?
                WHERE id = ?
                """;

        jdbcTemplate.update(
                sql,
                request.title().trim(),
                request.description() != null ? request.description().trim() : "",
                request.category() != null ? request.category().trim() : "General",
                request.difficultyLevel() != null ? request.difficultyLevel().trim() : "Beginner",
                request.durationHours(),
                request.platform() != null ? request.platform().trim() : "Online",
                request.courseUrl() != null ? request.courseUrl().trim() : "",
                formatList(request.careerGoals()),
                formatList(request.prerequisites()),
                Boolean.TRUE.equals(request.demo()),
                id
        );

        jdbcTemplate.update("DELETE FROM course_topics WHERE course_id = ?", id);
        insertTopics(id, request.topics());

        jdbcTemplate.update("DELETE FROM course_skills WHERE course_id = ?", id);
        insertSkills(id, request.skills());

        return getCourseById(id);
    }

    @Transactional
    public void deleteCourse(long id) {
        if (!courseExists(id)) {
            throw new CatalogNotFoundException("Course not found with id: " + id);
        }

        jdbcTemplate.update("DELETE FROM course_topics WHERE course_id = ?", id);
        jdbcTemplate.update("DELETE FROM course_skills WHERE course_id = ?", id);
        jdbcTemplate.update("DELETE FROM saved_courses WHERE course_id = ?", id);
        jdbcTemplate.update("DELETE FROM courses WHERE id = ?", id);
    }

    private void insertTopics(long courseId, List<String> topics) {
        if (topics == null || topics.isEmpty()) {
            return;
        }
        List<String> cleanTopics = topics.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        if (!cleanTopics.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO course_topics (course_id, topic_name) VALUES (?, ?)",
                    cleanTopics,
                    cleanTopics.size(),
                    (ps, topic) -> {
                        ps.setLong(1, courseId);
                        ps.setString(2, topic);
                    }
            );
        }
    }

    private void insertSkills(long courseId, List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return;
        }
        List<String> cleanSkills = skills.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        if (!cleanSkills.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO course_skills (course_id, skill_name) VALUES (?, ?)",
                    cleanSkills,
                    cleanSkills.size(),
                    (ps, skill) -> {
                        ps.setLong(1, courseId);
                        ps.setString(2, skill);
                    }
            );
        }
    }

    private List<CourseResponse> toCourseResponses(List<CourseRow> rows) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> courseIds = rows.stream().map(CourseRow::id).toList();
        String inSql = String.join(",", Collections.nCopies(courseIds.size(), "?"));

        Map<Long, List<String>> topicsMap = new HashMap<>();
        jdbcTemplate.query(
                "SELECT course_id, topic_name FROM course_topics WHERE course_id IN (" + inSql + ") ORDER BY id ASC",
                ps -> {
                    for (int i = 0; i < courseIds.size(); i++) {
                        ps.setLong(i + 1, courseIds.get(i));
                    }
                },
                rs -> {
                    long cId = rs.getLong("course_id");
                    String topic = rs.getString("topic_name");
                    topicsMap.computeIfAbsent(cId, k -> new ArrayList<>()).add(topic);
                }
        );

        Map<Long, List<String>> skillsMap = new HashMap<>();
        jdbcTemplate.query(
                "SELECT course_id, skill_name FROM course_skills WHERE course_id IN (" + inSql + ") ORDER BY id ASC",
                ps -> {
                    for (int i = 0; i < courseIds.size(); i++) {
                        ps.setLong(i + 1, courseIds.get(i));
                    }
                },
                rs -> {
                    long cId = rs.getLong("course_id");
                    String skill = rs.getString("skill_name");
                    skillsMap.computeIfAbsent(cId, k -> new ArrayList<>()).add(skill);
                }
        );

        return rows.stream().map(row -> new CourseResponse(
                row.id(),
                row.title(),
                row.description(),
                row.category(),
                row.difficultyLevel(),
                row.durationHours(),
                row.platform(),
                row.courseUrl(),
                parseList(row.careerGoalsRaw()),
                parseList(row.prerequisitesRaw()),
                topicsMap.getOrDefault(row.id(), List.of()),
                skillsMap.getOrDefault(row.id(), List.of()),
                row.demo()
        )).toList();
    }

    private void validateCourseRequest(CourseRequest request) {
        if (request == null) {
            throw new CatalogValidationException("Course payload is required");
        }
        if (request.title() == null || request.title().trim().isEmpty()) {
            throw new CatalogValidationException("Course title is required");
        }
        if (request.durationHours() != null && request.durationHours() < 0) {
            throw new CatalogValidationException("Duration hours must be non-negative");
        }
    }

    public static List<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String inner = trimmed.substring(1, trimmed.length() - 1).trim();
            if (inner.isEmpty()) {
                return List.of();
            }
            return Arrays.stream(inner.split(","))
                    .map(s -> s.trim().replaceAll("^[\"']|[\"']$", "").trim())
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static String formatList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private record CourseRow(
            long id,
            String title,
            String description,
            String category,
            String difficultyLevel,
            Integer durationHours,
            String platform,
            String courseUrl,
            String careerGoalsRaw,
            String prerequisitesRaw,
            boolean demo
    ) {}
}
