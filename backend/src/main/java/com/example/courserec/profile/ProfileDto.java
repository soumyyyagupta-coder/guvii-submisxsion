package com.example.courserec.profile;

import java.util.List;

public final class ProfileDto {

    private ProfileDto() {}

    public record SkillDto(
            String name,
            String proficiency
    ) {}

    public record ProfileResponse(
            long id,
            String name,
            String email,
            String academicBackground,
            String proficiencyLevel,
            String careerGoal,
            List<String> interests,
            List<SkillDto> skills
    ) {}

    public record ProfileUpdateRequest(
            String name,
            String academicBackground,
            String proficiencyLevel,
            String careerGoal,
            List<String> interests,
            List<SkillDto> skills
    ) {}
}
