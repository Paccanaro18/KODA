package com.paccanaro.koda.curriculum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Trilha dentro de um {@link Subject} (ex: Fundamentos da linguagem).
 * Seedada por migration, nunca criada pela aplicacao.
 *
 * <p>A ligacao com o subject e uma coluna crua (nao {@code @ManyToOne}): o
 * mapa de aprendizado agrupa por id em memoria em {@link CurriculumService},
 * entao nao ha necessidade de a JPA atravessar a associacao.
 */
@Entity
@Table(name = "topics")
public class Topic {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Topic() {
        // exigido pelo JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Topic topic)) {
            return false;
        }
        return id != null && id.equals(topic.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
