package com.paccanaro.koda.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Rastreabilidade completa de uma chamada ao LLM — aceita ou rejeitada, com o
 * motivo exato (docs/architecture/02-modelo-de-dados.md, secao 20). Gravada
 * uma vez e para sempre, como {@link com.paccanaro.koda.question.QuestionAttempt}:
 * nenhum metodo de update, o historico de geracao nao deveria mudar depois
 * de escrito.
 */
@Entity
@Table(name = "ai_generations")
public class AiGeneration {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "generation_request_id", updatable = false)
    private UUID generationRequestId;

    @Column(name = "question_id", updatable = false)
    private UUID questionId;

    @Column(nullable = false, updatable = false)
    private String model;

    @Column(name = "prompt_version", nullable = false, updatable = false)
    private String promptVersion;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String specification;

    @Column(name = "raw_output", updatable = false, columnDefinition = "text")
    private String rawOutput;

    @Column(name = "validation_results", nullable = false, updatable = false, columnDefinition = "text")
    private String validationResults;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private GenerationOutcome outcome;

    @Column(name = "input_tokens", updatable = false)
    private Integer inputTokens;

    @Column(name = "output_tokens", updatable = false)
    private Integer outputTokens;

    @Column(name = "cost_usd", precision = 10, scale = 6, updatable = false)
    private BigDecimal costUsd;

    @Column(name = "latency_ms", updatable = false)
    private Integer latencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiGeneration() {
        // exigido pelo JPA
    }

    private AiGeneration(UUID generationRequestId, UUID questionId, String model, String promptVersion,
                         String specification, String rawOutput, String validationResults, GenerationOutcome outcome,
                         Integer inputTokens, Integer outputTokens, BigDecimal costUsd, Integer latencyMs) {
        this.generationRequestId = generationRequestId;
        this.questionId = questionId;
        this.model = model;
        this.promptVersion = promptVersion;
        this.specification = specification;
        this.rawOutput = rawOutput;
        this.validationResults = validationResults;
        this.outcome = outcome;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.costUsd = costUsd;
        this.latencyMs = latencyMs;
    }

    public static AiGeneration record(UUID generationRequestId, UUID questionId, String model, String promptVersion,
                                      String specification, String rawOutput, String validationResults,
                                      GenerationOutcome outcome, Integer inputTokens, Integer outputTokens,
                                      BigDecimal costUsd, Integer latencyMs) {
        return new AiGeneration(generationRequestId, questionId, model, promptVersion, specification, rawOutput,
                validationResults, outcome, inputTokens, outputTokens, costUsd, latencyMs);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public GenerationOutcome getOutcome() {
        return outcome;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AiGeneration that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
