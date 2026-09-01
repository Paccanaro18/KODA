package com.paccanaro.koda.engine;

import java.time.Instant;
import java.util.UUID;

/**
 * Projecao do historico de tentativas de um usuario num concept — reconstruivel
 * a qualquer momento a partir de {@code question_attempts} (docs/architecture/02-modelo-de-dados.md).
 *
 * @param recentAccuracy taxa de acerto nas ultimas tentativas (janela curta, definida por quem monta este objeto)
 * @param lastDifficultyAttempted dificuldade da tentativa mais recente — ancora de onde o {@link AdaptiveEngine} ajusta
 * @param highestDifficultyCorrect maior dificuldade ja acertada; evidencia de progressao, nao so volume de acertos
 */
public record ConceptKnowledge(
        UUID conceptId,
        int attemptsTotal,
        int attemptsCorrect,
        double recentAccuracy,
        int consecutiveErrors,
        int lastDifficultyAttempted,
        int highestDifficultyCorrect,
        Instant lastPracticedAt
) {
}
