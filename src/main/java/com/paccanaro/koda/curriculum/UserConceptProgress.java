package com.paccanaro.koda.curriculum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Evidencia real de pratica de um usuario num concept. So existe linha aqui
 * para o que o aluno de fato tentou — escrita pelo {@code engine} a cada
 * tentativa registrada (ver {@code AdaptiveEngine#assessProgress}).
 */
@Entity
@Table(name = "user_concept_progress")
public class UserConceptProgress {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "concept_id", nullable = false)
    private UUID conceptId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressState state;

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    protected UserConceptProgress() {
        // exigido pelo JPA
    }

    private UserConceptProgress(UUID userId, UUID conceptId, ProgressState state, int progressPercent) {
        this.userId = userId;
        this.conceptId = conceptId;
        this.state = state;
        this.progressPercent = progressPercent;
    }

    public static UserConceptProgress start(UUID userId, UUID conceptId, ProgressState state, int progressPercent) {
        return new UserConceptProgress(userId, conceptId, state, progressPercent);
    }

    public void applyAssessment(ProgressState newState, int newProgressPercent) {
        this.state = newState;
        this.progressPercent = newProgressPercent;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getConceptId() {
        return conceptId;
    }

    public ProgressState getState() {
        return state;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserConceptProgress that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
