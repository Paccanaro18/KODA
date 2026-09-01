package com.paccanaro.koda.question;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Uma resposta, gravada uma vez e para sempre — a fonte da verdade de todo o
 * modelo de conhecimento (docs/architecture/02-modelo-de-dados.md). Sem
 * metodo de update deliberadamente: nada aqui deveria mudar depois de escrito.
 */
@Entity
@Table(name = "question_attempts")
public class QuestionAttempt {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "question_version_id", nullable = false, updatable = false)
    private UUID questionVersionId;

    @Column(name = "submitted_answer", nullable = false, updatable = false, columnDefinition = "text")
    private String submittedAnswer;

    @Column(name = "is_correct", nullable = false, updatable = false)
    private boolean correct;

    @Column(name = "response_time_ms", nullable = false, updatable = false)
    private int responseTimeMs;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "difficulty_at_time", nullable = false, updatable = false)
    private int difficultyAtTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected QuestionAttempt() {
        // exigido pelo JPA
    }

    private QuestionAttempt(UUID userId, UUID questionVersionId, String submittedAnswer,
                            boolean correct, int responseTimeMs, int difficultyAtTime) {
        this.userId = userId;
        this.questionVersionId = questionVersionId;
        this.submittedAnswer = submittedAnswer;
        this.correct = correct;
        this.responseTimeMs = responseTimeMs;
        this.difficultyAtTime = difficultyAtTime;
    }

    public static QuestionAttempt record(UUID userId, UUID questionVersionId, String submittedAnswer,
                                         boolean correct, int responseTimeMs, int difficultyAtTime) {
        return new QuestionAttempt(userId, questionVersionId, submittedAnswer, correct, responseTimeMs, difficultyAtTime);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getQuestionVersionId() {
        return questionVersionId;
    }

    public boolean isCorrect() {
        return correct;
    }

    public int getDifficultyAtTime() {
        return difficultyAtTime;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof QuestionAttempt that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
