package com.example.courserec.recommendation;

import java.util.Collections;
import java.util.List;

public record UserProfile(
    long userId,
    String academicBackground,
    String proficiencyLevel,
    String careerGoal,
    List<String> interests,
    List<UserSkill> skills
) {
    public record UserSkill(String name, String proficiency) {}

    public static UserProfile empty(long userId) {
        return new UserProfile(userId, null, null, null, Collections.emptyList(), Collections.emptyList());
    }
}
