package com.paccanaro.koda.curriculum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Validador do grafo de pre-requisitos")
class CurriculumGraphValidatorTest {

    private final CurriculumGraphValidator validator = new CurriculumGraphValidator();

    @Test
    @DisplayName("aceita um grafo aciclico")
    void acceptsAcyclicGraph() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        // b exige a, c exige b — cadeia linear, sem ciclo.
        assertThatCode(() -> validator.validate(List.of(edge(b, a), edge(c, b))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejeita um ciclo direto")
    void rejectsDirectCycle() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        assertThatThrownBy(() -> validator.validate(List.of(edge(a, b), edge(b, a))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("rejeita um ciclo indireto")
    void rejectsIndirectCycle() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        assertThatThrownBy(() -> validator.validate(List.of(edge(a, b), edge(b, c), edge(c, a))))
                .isInstanceOf(IllegalStateException.class);
    }

    private static ConceptPrerequisite edge(UUID conceptId, UUID prerequisiteId) {
        return new ConceptPrerequisite(conceptId, prerequisiteId);
    }
}
