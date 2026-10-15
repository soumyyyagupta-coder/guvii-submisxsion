package com.example.courserec.auth;

import com.example.courserec.profile.ProfileDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthAndProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testDemoCoursesSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM courses WHERE is_demo = true",
                Integer.class
        );
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(12);

        Integer topicCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_topics",
                Integer.class
        );
        assertThat(topicCount).isNotNull().isGreaterThan(0);

        Integer skillCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_skills",
                Integer.class
        );
        assertThat(skillCount).isNotNull().isGreaterThan(0);
    }

    @Test
    void testAuthAndProfileLifecycle() throws Exception {
        String email = "testuser" + System.currentTimeMillis() + "@example.com";
        String password = "password123";
        String name = "Test Student";

        // 1. Register with short password -> fail
        var shortPassReq = new AuthDto.RegisterRequest(name, email, "short");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPassReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        // 2. Register valid user -> success
        var regReq = new AuthDto.RegisterRequest(name, email, password);
        String regResponseStr = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.id").isNumber())
                .andExpect(jsonPath("$.user.name").value(name))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andReturn().getResponse().getContentAsString();

        AuthDto.AuthResponse regResponse = objectMapper.readValue(regResponseStr, AuthDto.AuthResponse.class);
        String token = regResponse.token();
        assertThat(token).isNotBlank();

        // 3. Register duplicate email -> fail
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email is already registered"));

        // 4. Login with bad password -> fail
        var badLogin = new AuthDto.LoginRequest(email, "wrongpassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());

        // 5. Login with correct password -> success
        var goodLogin = new AuthDto.LoginRequest(email, password);
        String loginResponseStr = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn().getResponse().getContentAsString();

        AuthDto.AuthResponse loginResponse = objectMapper.readValue(loginResponseStr, AuthDto.AuthResponse.class);
        String loginToken = loginResponse.token();

        // 6. GET /api/auth/me without token -> fail
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());

        // 7. GET /api/auth/me with valid token -> success
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        // 8. requireAdmin should fail for STUDENT
        assertThrows(ApiException.class, () -> authService.requireAdmin("Bearer " + loginToken));

        // 9. GET /api/profile
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.interests").isArray())
                .andExpect(jsonPath("$.skills").isArray());

        // 10. PUT /api/profile
        var updateReq = new ProfileDto.ProfileUpdateRequest(
                "Updated Name",
                "Computer Science B.S.",
                "INTERMEDIATE",
                "Full Stack Engineer",
                List.of("Web Development", "Cloud"),
                List.of(new ProfileDto.SkillDto("Java", "INTERMEDIATE"), new ProfileDto.SkillDto("React", "BEGINNER"))
        );

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + loginToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.academicBackground").value("Computer Science B.S."))
                .andExpect(jsonPath("$.proficiencyLevel").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.careerGoal").value("Full Stack Engineer"))
                .andExpect(jsonPath("$.interests[0]").value("Web Development"))
                .andExpect(jsonPath("$.skills[0].name").value("Java"))
                .andExpect(jsonPath("$.skills[0].proficiency").value("INTERMEDIATE"));

        // 11. DELETE /api/profile
        mockMvc.perform(delete("/api/profile")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // 12. Subsequent calls with that token should fail with 401
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isUnauthorized());
    }
}
