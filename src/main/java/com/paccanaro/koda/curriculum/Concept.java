package com.paccanaro.koda.curriculum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * No do grafo de aprendizado (ex: Tipos primitivos). Seedado por migration,
 * nunca criado pela aplicacao.
 */
@Entity
@Table(name = "concepts")
public class Concept {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Concept() {
        // exigido pelo JPA
    }

    public UUID getId() {
        return id;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Concept concept)) {
            return false;
        }
        return id != null && id.equals(concept.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
