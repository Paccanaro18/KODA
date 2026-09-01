package com.paccanaro.koda.engine;

import com.paccanaro.koda.curriculum.ProgressState;

import java.util.Map;
import java.util.UUID;

/**
 * Entrada do {@link AdaptiveEngine}: o que se sabe sobre o aluno, nada alem
 * disso. Um concept ausente de {@code progressStateByConcept} nunca foi
 * tentado ({@code unseen} — nao persistido, ver {@link ProgressState}).
 */
public record LearnerProfile(
        UUID userId,
        Map<UUID, ConceptKnowledge> knowledgeByConcept,
        Map<UUID, ProgressState> progressStateByConcept
) {
}
