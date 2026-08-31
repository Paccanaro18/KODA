package com.paccanaro.koda.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Preferencias do estudante. Separado de {@link User} por privacidade (secao 22). */
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(optional = false, fetch = jakarta.persistence.FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "display_name")
    private String displayName;

    @Column(nullable = false)
    private String locale = "pt-BR";

    @Column(nullable = false)
    private String timezone = "America/Sao_Paulo";

    @Column(name = "learning_goal")
    private String learningGoal;

    @Column(name = "daily_goal_minutes", nullable = false)
    private int dailyGoalMinutes = 15;

    @Column(name = "prefers_reduced_motion", nullable = false)
    private boolean prefersReducedMotion = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Profile() {
        // exigido pelo JPA
    }

    private Profile(User user, String displayName) {
        this.user = user;
        this.displayName = displayName;
    }

    public static Profile createFor(User user, String displayName) {
        return new Profile(user, displayName);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLocale() {
        return locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getLearningGoal() {
        return learningGoal;
    }

    public int getDailyGoalMinutes() {
        return dailyGoalMinutes;
    }

    public boolean isPrefersReducedMotion() {
        return prefersReducedMotion;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Profile profile)) {
            return false;
        }
        return userId != null && userId.equals(profile.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}
