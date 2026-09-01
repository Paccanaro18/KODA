package com.paccanaro.koda.question;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Camada 4 de deduplicacao: quantas vezes e quando um usuario ja viu uma
 * questao. {@link QuestionService#practiceSession} prioriza o que tem menos
 * exposicao ao montar uma sessao.
 */
@Entity
@Table(name = "user_question_exposure")
public class UserQuestionExposure {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "times_seen", nullable = false)
    private int timesSeen;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected UserQuestionExposure() {
        // exigido pelo JPA
    }

    private UserQuestionExposure(UUID userId, UUID questionId) {
        this.userId = userId;
        this.questionId = questionId;
        this.timesSeen = 1;
        this.lastSeenAt = Instant.now();
    }

    public static UserQuestionExposure firstExposure(UUID userId, UUID questionId) {
        return new UserQuestionExposure(userId, questionId);
    }

    public void recordExposure() {
        this.timesSeen += 1;
        this.lastSeenAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public int getTimesSeen() {
        return timesSeen;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserQuestionExposure that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
