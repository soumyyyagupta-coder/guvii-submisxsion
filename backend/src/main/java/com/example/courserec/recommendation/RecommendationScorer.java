package com.example.courserec.recommendation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class RecommendationScorer {

    public RecommendationResponse scoreAndExplain(UserProfile profile, CourseDto course) {
        List<String> reasons = new ArrayList<>();

        double interestScore = calculateInterestScore(profile, course, reasons);
        double careerScore = calculateCareerScore(profile, course, reasons);
        double skillScore = calculateSkillScore(profile, course, reasons);
        double levelScore = calculateLevelScore(profile, course, reasons);

        double interest = roundToOneDecimal(interestScore);
        double career = roundToOneDecimal(careerScore);
        double skill = roundToOneDecimal(skillScore);
        double level = roundToOneDecimal(levelScore);

        double total = (0.40 * interest) + (0.30 * career) + (0.20 * skill) + (0.10 * level);
        double finalScore = roundToOneDecimal(total);

        if (reasons.isEmpty()) {
            reasons.add("Available course in catalog");
        }

        ScoreComponents components = new ScoreComponents(interest, career, skill, level);
        return new RecommendationResponse(course, finalScore, components, Collections.unmodifiableList(reasons));
    }

    public double calculateInterestScore(UserProfile profile, CourseDto course, List<String> reasons) {
        List<String> interests = profile.interests();
        if (interests == null || interests.isEmpty()) {
            return 0.0;
        }

        String courseCategory = normalize(course.category());
        String courseTitle = normalize(course.title());
        String courseDescription = normalize(course.description());

        List<String> normalizedTopics = course.topics() != null
                ? course.topics().stream().map(this::normalize).filter(s -> !s.isEmpty()).toList()
                : Collections.emptyList();

        double totalMatchPoints = 0.0;
        List<String> matchedInterestNames = new ArrayList<>();

        for (String rawInterest : interests) {
            String normInterest = normalize(rawInterest);
            if (normInterest.isEmpty()) {
                continue;
            }

            double bestMatch = 0.0;
            String matchDetail = null;

            // 1. Topic matching
            for (int i = 0; i < normalizedTopics.size(); i++) {
                String normTopic = normalizedTopics.get(i);
                if (normTopic.equals(normInterest)) {
                    bestMatch = Math.max(bestMatch, 1.0);
                    matchDetail = "Topic: " + course.topics().get(i);
                    break;
                } else if (normTopic.contains(normInterest) || normInterest.contains(normTopic)) {
                    if (bestMatch < 0.9) {
                        bestMatch = 0.9;
                        matchDetail = "Topic: " + course.topics().get(i);
                    }
                }
            }

            // 2. Category matching
            if (bestMatch < 0.85 && !courseCategory.isEmpty()) {
                if (courseCategory.equals(normInterest) || courseCategory.contains(normInterest) || normInterest.contains(courseCategory)) {
                    bestMatch = Math.max(bestMatch, 0.85);
                    matchDetail = "Category: " + course.category();
                }
            }

            // 3. Title matching
            if (bestMatch < 0.75 && !courseTitle.isEmpty() && courseTitle.contains(normInterest)) {
                bestMatch = Math.max(bestMatch, 0.75);
                matchDetail = "Title relevance";
            }

            // 4. Token overlap with topics / category / title
            if (bestMatch < 0.65) {
                Set<String> interestTokens = tokenize(normInterest);
                if (!interestTokens.isEmpty()) {
                    long tokenHits = interestTokens.stream()
                            .filter(t -> courseTitle.contains(t) || courseCategory.contains(t)
                                    || normalizedTopics.stream().anyMatch(top -> top.contains(t)))
                            .count();
                    if (tokenHits > 0) {
                        double tokenRatio = (double) tokenHits / interestTokens.size();
                        double tokenScore = tokenRatio * 0.65;
                        if (tokenScore > bestMatch) {
                            bestMatch = tokenScore;
                            matchDetail = "Related subject";
                        }
                    }
                }
            }

            // 5. Description matching
            if (bestMatch < 0.40 && !courseDescription.isEmpty() && courseDescription.contains(normInterest)) {
                bestMatch = Math.max(bestMatch, 0.40);
                matchDetail = "Description match";
            }

            if (bestMatch > 0.0) {
                totalMatchPoints += bestMatch;
                matchedInterestNames.add(rawInterest.trim());
            }
        }

        if (matchedInterestNames.isEmpty() || totalMatchPoints <= 0.0) {
            return 0.0;
        }

        // Add user-friendly reason
        if (matchedInterestNames.size() == 1) {
            reasons.add("Matches your interest in " + matchedInterestNames.get(0));
        } else {
            reasons.add("Matches your interests in " + String.join(", ", matchedInterestNames));
        }

        double ratio = totalMatchPoints / interests.size();
        // Give bonus for matching multiple interests while scaling appropriately
        double multiBonus = Math.min(100.0, totalMatchPoints * 35.0);
        return Math.min(100.0, Math.max(ratio * 100.0, multiBonus));
    }

    public double calculateCareerScore(UserProfile profile, CourseDto course, List<String> reasons) {
        String rawGoal = profile.careerGoal();
        if (rawGoal == null || rawGoal.isBlank()) {
            return 0.0;
        }

        String normUserGoal = normalize(rawGoal);
        Set<String> userGoalTokens = careerTokens(normUserGoal);

        double bestMatch = 0.0;
        String matchedTarget = null;

        // 1. Check course.careerGoals
        List<String> courseGoals = course.careerGoals();
        if (courseGoals != null && !courseGoals.isEmpty()) {
            for (String cg : courseGoals) {
                String normCg = normalize(cg);
                Set<String> cgTokens = careerTokens(normCg);
                boolean subjectOverlap = !Collections.disjoint(userGoalTokens, cgTokens);
                if (normCg.equals(normUserGoal)) {
                    bestMatch = Math.max(bestMatch, 100.0);
                    matchedTarget = cg;
                    break;
                } else if (subjectOverlap && (normCg.contains(normUserGoal) || normUserGoal.contains(normCg))) {
                    if (bestMatch < 85.0) {
                        bestMatch = 85.0;
                        matchedTarget = cg;
                    }
                } else if (!userGoalTokens.isEmpty()) {
                    long shared = userGoalTokens.stream().filter(cgTokens::contains).count();
                    if (shared > 0) {
                        double overlap = (double) shared / Math.max(userGoalTokens.size(), cgTokens.size());
                        double score = Math.max(45.0, overlap * 80.0);
                        if (score > bestMatch) {
                            bestMatch = score;
                            matchedTarget = cg;
                        }
                    }
                }
            }
        }

        // 2. Check course category, topics, and title
        if (bestMatch < 75.0) {
            String cat = normalize(course.category());
            if (!cat.isEmpty() && (cat.equals(normUserGoal) || cat.contains(normUserGoal) || normUserGoal.contains(cat))) {
                bestMatch = Math.max(bestMatch, 75.0);
            }
        }

        if (bestMatch < 65.0 && course.topics() != null) {
            for (String top : course.topics()) {
                String normTop = normalize(top);
                if (normTop.equals(normUserGoal) || normTop.contains(normUserGoal) || normUserGoal.contains(normTop)) {
                    bestMatch = Math.max(bestMatch, 65.0);
                    break;
                }
            }
        }

        if (bestMatch < 60.0) {
            String title = normalize(course.title());
            if (!title.isEmpty() && title.contains(normUserGoal)) {
                bestMatch = Math.max(bestMatch, 60.0);
            }
        }

        if (bestMatch < 50.0 && !userGoalTokens.isEmpty()) {
            String fullCourseText = normalize(course.title() + " " + course.category() + " " + String.join(" ", course.topics() != null ? course.topics() : List.of()));
            long tokenHits = userGoalTokens.stream().filter(fullCourseText::contains).count();
            if (tokenHits > 0) {
                double tokenOverlap = (double) tokenHits / userGoalTokens.size();
                bestMatch = Math.max(bestMatch, tokenOverlap * 50.0);
            }
        }

        if (bestMatch > 0.0) {
            if (bestMatch >= 80.0) {
                reasons.add("Directly aligns with your career goal: " + rawGoal.trim());
            } else if (bestMatch >= 50.0) {
                reasons.add("Supports your career goal of " + rawGoal.trim());
            } else {
                reasons.add("Relates to your career goal: " + rawGoal.trim());
            }
        }

        return Math.min(100.0, bestMatch);
    }

    public double calculateSkillScore(UserProfile profile, CourseDto course, List<String> reasons) {
        List<UserProfile.UserSkill> userSkills = profile.skills();
        List<String> coursePrereqs = course.prerequisites() != null ? course.prerequisites() : Collections.emptyList();
        List<String> courseSkills = course.skills() != null ? course.skills() : Collections.emptyList();

        if (userSkills == null || userSkills.isEmpty()) {
            // Mention course prerequisites even when user has no skills listed
            if (!coursePrereqs.isEmpty()) {
                reasons.add("Prerequisites recommended: " + String.join(", ", coursePrereqs));
            }
            return 0.0;
        }

        List<String> matchedSkillNames = new ArrayList<>();
        List<String> satisfiedPrereqs = new ArrayList<>();

        for (UserProfile.UserSkill skill : userSkills) {
            if (skill == null || skill.name() == null || skill.name().isBlank()) {
                continue;
            }
            String normUserSkill = normalize(skill.name());

            // Check against course skills
            for (String cs : courseSkills) {
                String normCs = normalize(cs);
                if (normCs.equals(normUserSkill) || normCs.contains(normUserSkill) || normUserSkill.contains(normCs)) {
                    if (!matchedSkillNames.contains(skill.name().trim())) {
                        matchedSkillNames.add(skill.name().trim());
                    }
                    break;
                }
            }

            // Check against course topics / title if courseSkills is empty
            if (courseSkills.isEmpty()) {
                boolean foundInTopicsOrTitle = (course.topics() != null && course.topics().stream().anyMatch(t -> normalize(t).contains(normUserSkill)))
                        || normalize(course.title()).contains(normUserSkill);
                if (foundInTopicsOrTitle && !matchedSkillNames.contains(skill.name().trim())) {
                    matchedSkillNames.add(skill.name().trim());
                }
            }

            // Check against prerequisites
            for (String prereq : coursePrereqs) {
                String normPrereq = normalize(prereq);
                if (normPrereq.contains(normUserSkill) || normUserSkill.contains(normPrereq)) {
                    if (!satisfiedPrereqs.contains(prereq)) {
                        satisfiedPrereqs.add(prereq);
                    }
                }
            }
        }

        // Explanations for skills
        if (!matchedSkillNames.isEmpty()) {
            if (matchedSkillNames.size() == 1) {
                reasons.add("Builds on your skill: " + matchedSkillNames.get(0));
            } else {
                reasons.add("Builds on your skills: " + String.join(", ", matchedSkillNames));
            }
        }

        if (!satisfiedPrereqs.isEmpty()) {
            reasons.add("You meet the prerequisite: " + String.join(", ", satisfiedPrereqs));
        }

        // If there are prerequisites that the user does not meet, explain them
        List<String> unmetPrereqs = new ArrayList<>();
        for (String prereq : coursePrereqs) {
            if (!satisfiedPrereqs.contains(prereq)) {
                unmetPrereqs.add(prereq);
            }
        }
        if (!unmetPrereqs.isEmpty()) {
            reasons.add("Prerequisites recommended: " + String.join(", ", unmetPrereqs));
        }

        if (matchedSkillNames.isEmpty() && satisfiedPrereqs.isEmpty()) {
            return 0.0;
        }

        double baseScore = matchedSkillNames.size() * 35.0;
        if (!courseSkills.isEmpty()) {
            double coverage = (double) matchedSkillNames.size() / courseSkills.size();
            baseScore = Math.max(baseScore, coverage * 85.0);
        }
        double prereqBonus = satisfiedPrereqs.size() * 20.0;

        return Math.min(100.0, baseScore + prereqBonus);
    }

    public double calculateLevelScore(UserProfile profile, CourseDto course, List<String> reasons) {
        String userLevel = profile.proficiencyLevel();
        if (userLevel == null || userLevel.isBlank()) {
            return 0.0;
        }

        int userRank = parseLevelRank(userLevel);
        int courseRank = parseLevelRank(course.difficultyLevel());

        if (courseRank == 0) {
            // Course is suitable for all levels
            reasons.add("Suitable for all proficiency levels");
            return 90.0;
        }

        if (userRank > 0 && courseRank > 0) {
            int diff = courseRank - userRank;
            if (diff == 0) {
                reasons.add("Matches your " + capitalize(userLevel.trim()) + " proficiency level");
                return 100.0;
            } else if (diff == 1) {
                reasons.add("Great next step up from your " + capitalize(userLevel.trim()) + " level");
                return 50.0;
            } else if (diff == -1) {
                reasons.add("Foundational refresher for your " + capitalize(userLevel.trim()) + " level");
                return 50.0;
            } else if (diff >= 2) {
                reasons.add("Advanced course - foundational preparation recommended");
                return 15.0;
            } else {
                reasons.add("Introductory course below your " + capitalize(userLevel.trim()) + " level");
                return 15.0;
            }
        }

        // Unknown level string: fall back to normalized string equality
        if (normalize(userLevel).equals(normalize(course.difficultyLevel()))) {
            reasons.add("Matches your " + capitalize(userLevel.trim()) + " level");
            return 100.0;
        }

        return 0.0;
    }

    private int parseLevelRank(String level) {
        if (level == null || level.isBlank()) {
            return -1;
        }
        String norm = normalize(level);
        if (norm.contains("all") || norm.contains("any") || norm.contains("mixed") || norm.contains("everyone")) {
            return 0;
        }
        if (norm.contains("begin") || norm.contains("intro") || norm.contains("basic") || norm.contains("entry") || norm.contains("novice")) {
            return 1;
        }
        if (norm.contains("inter") || norm.contains("med")) {
            return 2;
        }
        if (norm.contains("adv") || norm.contains("expert") || norm.contains("master")) {
            return 3;
        }
        return -1;
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase(Locale.ROOT);
    }

    public Set<String> tokenize(String s) {
        if (s == null || s.isBlank()) {
            return Collections.emptySet();
        }
        String[] words = s.trim().toLowerCase(Locale.ROOT).split("[^a-zA-Z0-9]+");
        Set<String> set = new HashSet<>();
        for (String w : words) {
            if (w.length() > 1 && !isStopWord(w)) {
                set.add(w);
            }
        }
        return set;
    }

    private Set<String> careerTokens(String goal) {
        Set<String> tokens = new HashSet<>(tokenize(goal));
        tokens.removeAll(Set.of("engineer", "developer", "scientist", "analyst", "architect",
                "specialist", "manager", "officer", "consultant", "expert", "professional"));
        return tokens;
    }

    private boolean isStopWord(String w) {
        return switch (w) {
            case "and", "or", "the", "in", "to", "for", "with", "a", "an", "of", "on", "at", "by", "from", "as", "is" -> true;
            default -> false;
        };
    }

    public static double roundToOneDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
