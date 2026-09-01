package com.paccanaro.koda.curriculum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Materia curada do curriculo (ex: Programacao). Seedada por migration
 * (Flyway), nunca criada pela aplicacao — por isso nao ha factory publica.
 */
@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Subject() {
        // exigido pelo JPA
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Subject subject)) {
            return false;
        }
        return id != null && id.equals(subject.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
