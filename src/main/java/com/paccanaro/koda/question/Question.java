package com.paccanaro.koda.question;

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
 * Questao curada — o "chapeu" estavel sob o qual as versoes imutaveis vivem.
 * Seedada por migration nesta fase; nenhum fluxo da aplicacao ainda cria uma.
 *
 * <p>{@code questionType} resolve o {@link QuestionTypeHandler} certo no
 * {@link QuestionTypeRegistry} — e o que torna o tipo extensivel sem alterar
 * nucleo nenhum (MNT-01).
 */
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "question_type", nullable = false)
    private String questionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionStatus status;

    protected Question() {
        // exigido pelo JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public String getQuestionType() {
        return questionType;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Question question)) {
            return false;
        }
        return id != null && id.equals(question.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
