package com.paccanaro.koda.question;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Versao imutavel de uma {@link Question}. Uma tentativa referencia a
 * <b>versao</b> respondida, nunca a questao — editar uma questao nao pode
 * corromper retroativamente o historico de aprendizado (DAT-02).
 *
 * <p>{@code payload}, {@code correctAnswer} e {@code distractorRationales} sao
 * texto JSON, nao {@code jsonb} mapeado nativamente: evita depender de como
 * Hibernate 7 serializa {@code JsonNode} do Jackson 3, combinacao nova demais
 * pra apostar sem verificar. O parsing acontece explicitamente no codigo, via
 * {@code ObjectMapper}, em {@link QuestionService} e nos handlers.
 */
@Entity
@Table(name = "question_versions")
public class QuestionVersion {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "correct_answer", nullable = false, columnDefinition = "text")
    private String correctAnswer;

    @Column(nullable = false, columnDefinition = "text")
    private String explanation;

    @Column(name = "distractor_rationales", columnDefinition = "text")
    private String distractorRationales;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "declared_difficulty", nullable = false)
    private int declaredDifficulty;

    @Column(name = "measured_difficulty", precision = 4, scale = 3)
    private BigDecimal measuredDifficulty;

    @Column(name = "estimated_time_seconds")
    private Integer estimatedTimeSeconds;

    @Column(name = "canonical_hash", nullable = false)
    private byte[] canonicalHash;

    @Column(name = "quality_score", precision = 4, scale = 3)
    private BigDecimal qualityScore;

    @Column(nullable = false)
    private String language;

    protected QuestionVersion() {
        // exigido pelo JPA
    }

    private QuestionVersion(UUID questionId, int version, String payload, String correctAnswer, String explanation,
                            String distractorRationales, int declaredDifficulty, Integer estimatedTimeSeconds,
                            byte[] canonicalHash, BigDecimal qualityScore, String language) {
        this.questionId = questionId;
        this.version = version;
        this.payload = payload;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.distractorRationales = distractorRationales;
        this.declaredDifficulty = declaredDifficulty;
        this.estimatedTimeSeconds = estimatedTimeSeconds;
        this.canonicalHash = canonicalHash;
        this.qualityScore = qualityScore;
        this.language = language;
    }

    /** Primeira versao de uma questao gerada por IA. Imutavel a partir daqui (DAT-02). */
    public static QuestionVersion firstVersion(UUID questionId, String payload, String correctAnswer,
                                               String explanation, String distractorRationales, int declaredDifficulty,
                                               Integer estimatedTimeSeconds, byte[] canonicalHash,
                                               BigDecimal qualityScore) {
        return new QuestionVersion(questionId, 1, payload, correctAnswer, explanation, distractorRationales,
                declaredDifficulty, estimatedTimeSeconds, canonicalHash, qualityScore, "pt-BR");
    }

    public UUID getId() {
        return id;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public int getVersion() {
        return version;
    }

    public String getPayload() {
        return payload;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getDistractorRationales() {
        return distractorRationales;
    }

    public int getDeclaredDifficulty() {
        return declaredDifficulty;
    }

    public Integer getEstimatedTimeSeconds() {
        return estimatedTimeSeconds;
    }

    public byte[] getCanonicalHash() {
        return canonicalHash;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof QuestionVersion that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
