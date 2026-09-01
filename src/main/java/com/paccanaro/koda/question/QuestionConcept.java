package com.paccanaro.koda.question;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/** Qual concept uma versao de questao testa. Seedada por migration. */
@Entity
@Table(name = "question_concepts")
public class QuestionConcept {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "question_version_id", nullable = false)
    private UUID questionVersionId;

    @Column(name = "concept_id", nullable = false)
    private UUID conceptId;

    protected QuestionConcept() {
        // exigido pelo JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getQuestionVersionId() {
        return questionVersionId;
    }

    public UUID getConceptId() {
        return conceptId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof QuestionConcept that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
