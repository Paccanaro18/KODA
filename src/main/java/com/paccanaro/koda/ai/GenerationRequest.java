package com.paccanaro.koda.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Um pedido na fila de pre-geracao — o unico jeito de disparar o
 * {@link AiGatewayClient}. O aluno nunca cria isto no caminho do request
 * (ARC-01); so o {@code GenerationWorker}, assincrono, le e processa.
 */
@Entity
@Table(name = "generation_requests")
public class GenerationRequest {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "concept_id", nullable = false, updatable = false)
    private UUID conceptId;

    @Column(name = "question_type", nullable = false, updatable = false)
    private String questionType;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "target_difficulty", nullable = false, updatable = false)
    private int targetDifficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationRequestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected GenerationRequest() {
        // exigido pelo JPA
    }

    private GenerationRequest(UUID conceptId, String questionType, int targetDifficulty) {
        this.conceptId = conceptId;
        this.questionType = questionType;
        this.targetDifficulty = targetDifficulty;
        this.status = GenerationRequestStatus.PENDING;
    }

    public static GenerationRequest enqueue(UUID conceptId, String questionType, int targetDifficulty) {
        return new GenerationRequest(conceptId, questionType, targetDifficulty);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void markProcessing() {
        this.status = GenerationRequestStatus.PROCESSING;
    }

    public void markDone() {
        this.status = GenerationRequestStatus.DONE;
        this.processedAt = Instant.now();
    }

    public void markFailed() {
        this.status = GenerationRequestStatus.FAILED;
        this.processedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getConceptId() {
        return conceptId;
    }

    public String getQuestionType() {
        return questionType;
    }

    public int getTargetDifficulty() {
        return targetDifficulty;
    }

    public GenerationRequestStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GenerationRequest that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
