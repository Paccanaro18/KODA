package com.paccanaro.koda.curriculum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Aresta do grafo de pre-requisitos: {@code concept} exige {@code prerequisite}.
 * Seedada por migration, nunca criada pela aplicacao — o construtor de pacote
 * existe so para {@link CurriculumGraphValidatorTest} montar grafos sinteticos.
 */
@Entity
@Table(name = "concept_prerequisites")
public class ConceptPrerequisite {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "concept_id", nullable = false)
    private UUID conceptId;

    @Column(name = "prerequisite_concept_id", nullable = false)
    private UUID prerequisiteConceptId;

    protected ConceptPrerequisite() {
        // exigido pelo JPA
    }

    ConceptPrerequisite(UUID conceptId, UUID prerequisiteConceptId) {
        this.conceptId = conceptId;
        this.prerequisiteConceptId = prerequisiteConceptId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConceptId() {
        return conceptId;
    }

    public UUID getPrerequisiteConceptId() {
        return prerequisiteConceptId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConceptPrerequisite that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
