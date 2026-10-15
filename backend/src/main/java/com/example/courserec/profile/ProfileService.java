package com.example.courserec.profile;

import com.example.courserec.auth.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProfileService {

    private final JdbcTemplate jdbcTemplate;

    public ProfileService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ProfileDto.ProfileResponse getProfile(long userId) {
        String userSql = "SELECT id, name, email, academic_background, proficiency_level, career_goal FROM users WHERE id = ?";
        var users = jdbcTemplate.query(userSql, (rs, rowNum) -> new Object() {
            final long id = rs.getLong("id");
            final String name = rs.getString("name");
            final String email = rs.getString("email");
            final String academicBackground = rs.getString("academic_background") != null ? rs.getString("academic_background") : "";
            final String proficiencyLevel = rs.getString("proficiency_level") != null ? rs.getString("proficiency_level") : "";
            final String careerGoal = rs.getString("career_goal") != null ? rs.getString("career_goal") : "";
        }, userId);

        if (users.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User profile not found");
        }

        var user = users.get(0);

        List<String> interests = jdbcTemplate.query(
                "SELECT interest_name FROM user_interests WHERE user_id = ? ORDER BY id ASC",
                (rs, rowNum) -> rs.getString("interest_name"),
                userId
        );

        List<ProfileDto.SkillDto> skills = jdbcTemplate.query(
                "SELECT skill_name, proficiency FROM user_skills WHERE user_id = ? ORDER BY id ASC",
                (rs, rowNum) -> new ProfileDto.SkillDto(
                        rs.getString("skill_name"),
                        rs.getString("proficiency") != null ? rs.getString("proficiency") : ""
                ),
                userId
        );

        return new ProfileDto.ProfileResponse(
                user.id,
                user.name,
                user.email,
                user.academicBackground,
                user.proficiencyLevel,
                user.careerGoal,
                interests,
                skills
        );
    }

    @Transactional
    public ProfileDto.ProfileResponse updateProfile(long userId, ProfileDto.ProfileUpdateRequest request) {
        Integer userExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                userId
        );

        if (userExists == null || userExists == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User profile not found");
        }

        String updatedName = request.name() != null && !request.name().trim().isEmpty()
                ? request.name().trim()
                : null;
        String academicBackground = request.academicBackground() != null ? request.academicBackground().trim() : "";
        String proficiencyLevel = request.proficiencyLevel() != null ? request.proficiencyLevel().trim() : "";
        String careerGoal = request.careerGoal() != null ? request.careerGoal().trim() : "";

        if (updatedName != null) {
            jdbcTemplate.update(
                    "UPDATE users SET name = ?, academic_background = ?, proficiency_level = ?, career_goal = ? WHERE id = ?",
                    updatedName,
                    academicBackground,
                    proficiencyLevel,
                    careerGoal,
                    userId
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE users SET academic_background = ?, proficiency_level = ?, career_goal = ? WHERE id = ?",
                    academicBackground,
                    proficiencyLevel,
                    careerGoal,
                    userId
            );
        }

        // Update interests
        jdbcTemplate.update("DELETE FROM user_interests WHERE user_id = ?", userId);
        if (request.interests() != null) {
            for (String interest : request.interests()) {
                if (interest != null && !interest.trim().isEmpty()) {
                    jdbcTemplate.update(
                            "INSERT INTO user_interests (user_id, interest_name) VALUES (?, ?)",
                            userId,
                            interest.trim()
                    );
                }
            }
        }

        // Update skills
        jdbcTemplate.update("DELETE FROM user_skills WHERE user_id = ?", userId);
        if (request.skills() != null) {
            for (ProfileDto.SkillDto skill : request.skills()) {
                if (skill != null && skill.name() != null && !skill.name().trim().isEmpty()) {
                    String prof = skill.proficiency() != null ? skill.proficiency().trim() : "";
                    jdbcTemplate.update(
                            "INSERT INTO user_skills (user_id, skill_name, proficiency) VALUES (?, ?, ?)",
                            userId,
                            skill.name().trim(),
                            prof
                    );
                }
            }
        }

        return getProfile(userId);
    }

    @Transactional
    public void deleteAccount(long userId) {
        Integer userExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                userId
        );

        if (userExists == null || userExists == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "User profile not found");
        }

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }
}
