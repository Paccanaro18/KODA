package com.paccanaro.koda.auth.dto;

import com.paccanaro.koda.user.Profile;
import com.paccanaro.koda.user.User;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String role,
        String displayName,
        String locale,
        String timezone,
        String learningGoal,
        int dailyGoalMinutes,
        boolean prefersReducedMotion
) {
    public static MeResponse of(User user, Profile profile) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                profile.getDisplayName(),
                profile.getLocale(),
                profile.getTimezone(),
                profile.getLearningGoal(),
                profile.getDailyGoalMinutes(),
                profile.isPrefersReducedMotion());
    }
}
