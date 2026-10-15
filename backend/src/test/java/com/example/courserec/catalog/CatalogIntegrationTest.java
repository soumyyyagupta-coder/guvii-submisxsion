package com.example.courserec.catalog;

import com.example.courserec.auth.AuthDto;
import com.example.courserec.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String studentToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // Create student user and session
        String studentEmail = "student_" + System.currentTimeMillis() + "@test.com";
        AuthDto.AuthResponse studentAuth = authService.register(new AuthDto.RegisterRequest("Student User", studentEmail, "password123"));
        studentToken = studentAuth.token();

        // Create admin user and session
        String adminEmail = "admin_" + System.currentTimeMillis() + "@test.com";
        AuthDto.AuthResponse adminAuth = authService.register(new AuthDto.RegisterRequest("Admin User", adminEmail, "adminpassword123"));
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", adminEmail);
        adminToken = adminAuth.token();
    }

    @Test
    @DisplayName("GET /api/courses returns seeded courses array with expected shape")
    void testGetCourses() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].title").isString())
                .andExpect(jsonPath("$[0].topics").isArray())
                .andExpect(jsonPath("$[0].skills").isArray())
                .andExpect(jsonPath("$[0].careerGoals").isArray())
                .andExpect(jsonPath("$[0].prerequisites").isArray())
                .andExpect(jsonPath("$[0].demo").isBoolean());
    }

    @Test
    @DisplayName("GET /api/courses with filters applies query parameters correctly")
    void testGetCoursesWithFilters() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .param("q", "Python")
                        .param("level", "Beginner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/courses/{id} returns course or 404 with error message")
    void testGetCourseById() throws Exception {
        // Query an existing course id from database
        Long existingId = jdbcTemplate.queryForObject("SELECT id FROM courses LIMIT 1", Long.class);

        mockMvc.perform(get("/api/courses/" + existingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingId))
                .andExpect(jsonPath("$.title").isNotEmpty());

        mockMvc.perform(get("/api/courses/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("not found")));
    }

    @Test
    @DisplayName("Admin CRUD lifecycle: create, update, delete course with authorization checks")
    void testAdminCourseLifecycle() throws Exception {
        CourseRequest newCourse = new CourseRequest(
                "Cloud Architecture Fundamentals",
                "Learn modern distributed system architecture on the cloud",
                "Cloud Computing",
                "Intermediate",
                25,
                "Coursera",
                "https://example.com/cloud",
                List.of("Cloud Architect", "DevOps Engineer"),
                List.of("Networking Basics"),
                List.of("AWS", "Kubernetes", "Microservices"),
                List.of("Docker", "Terraform"),
                false
        );

        // Unauthorized (missing token) -> 401
        mockMvc.perform(post("/api/admin/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCourse)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isNotEmpty());

        // Forbidden (student token) -> 403
        mockMvc.perform(post("/api/admin/courses")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCourse)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").isNotEmpty());

        // Authorized (admin token) -> 201 Created
        String responseJson = mockMvc.perform(post("/api/admin/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCourse)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Cloud Architecture Fundamentals"))
                .andExpect(jsonPath("$.topics", hasItem("Kubernetes")))
                .andExpect(jsonPath("$.skills", hasItem("Docker")))
                .andReturn().getResponse().getContentAsString();

        CourseResponse created = objectMapper.readValue(responseJson, CourseResponse.class);
        long createdId = created.id();

        // Update course -> 200 OK
        CourseRequest updatedCourse = new CourseRequest(
                "Advanced Cloud Architecture",
                "Updated description for cloud architecture",
                "Cloud Computing",
                "Advanced",
                45,
                "Coursera",
                "https://example.com/cloud-adv",
                List.of("Principal Architect"),
                List.of("AWS Basics"),
                List.of("Serverless", "Event-Driven"),
                List.of("Kafka", "Go"),
                false
        );

        mockMvc.perform(put("/api/admin/courses/" + createdId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedCourse)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Advanced Cloud Architecture"))
                .andExpect(jsonPath("$.durationHours").value(45))
                .andExpect(jsonPath("$.topics", hasItem("Serverless")))
                .andExpect(jsonPath("$.skills", hasItem("Kafka")));

        // Delete course -> 204 No Content
        mockMvc.perform(delete("/api/admin/courses/" + createdId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify it is gone -> 404
        mockMvc.perform(get("/api/courses/" + createdId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Saved courses lifecycle: save, get, duplicate save, unsave")
    void testSavedCoursesLifecycle() throws Exception {
        Long courseId = jdbcTemplate.queryForObject("SELECT id FROM courses LIMIT 1", Long.class);

        // GET saved initially empty
        mockMvc.perform(get("/api/saved")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", empty()));

        // POST save course -> 204 No Content
        mockMvc.perform(post("/api/saved/" + courseId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNoContent());

        // Duplicate save course -> 204 No Content (idempotent)
        mockMvc.perform(post("/api/saved/" + courseId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNoContent());

        // GET saved now contains the course
        mockMvc.perform(get("/api/saved")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(courseId));

        // DELETE unsave course -> 204 No Content
        mockMvc.perform(delete("/api/saved/" + courseId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNoContent());

        // GET saved is empty again
        mockMvc.perform(get("/api/saved")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }
}
