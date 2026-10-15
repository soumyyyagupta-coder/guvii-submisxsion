package com.example.courserec.saved;

import com.example.courserec.catalog.CatalogNotFoundException;
import com.example.courserec.catalog.CatalogService;
import com.example.courserec.catalog.CourseResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class SavedCourseService {

    private final JdbcTemplate jdbcTemplate;
    private final CatalogService catalogService;

    public SavedCourseService(JdbcTemplate jdbcTemplate, CatalogService catalogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.catalogService = catalogService;
    }

    public List<CourseResponse> getSavedCourses(long userId) {
        String sql = "SELECT course_id FROM saved_courses WHERE user_id = ? ORDER BY saved_at DESC, course_id ASC";
        List<Long> courseIds = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getLong("course_id"),
                userId
        );

        if (courseIds.isEmpty()) {
            return Collections.emptyList();
        }

        return catalogService.getCoursesByIdsPreservingOrder(courseIds);
    }

    @Transactional
    public void saveCourse(long userId, long courseId) {
        if (!catalogService.courseExists(courseId)) {
            throw new CatalogNotFoundException("Course not found with id: " + courseId);
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM saved_courses WHERE user_id = ? AND course_id = ?",
                Integer.class,
                userId,
                courseId
        );

        if (count != null && count > 0) {
            return;
        }

        try {
            jdbcTemplate.update(
                    "INSERT INTO saved_courses (user_id, course_id, saved_at) VALUES (?, ?, ?)",
                    userId,
                    courseId,
                    Timestamp.from(Instant.now())
            );
        } catch (DataIntegrityViolationException ignored) {
            // Idempotent duplicate save handling
        }
    }

    @Transactional
    public void unsaveCourse(long userId, long courseId) {
        jdbcTemplate.update(
                "DELETE FROM saved_courses WHERE user_id = ? AND course_id = ?",
                userId,
                courseId
        );
    }
}
