package com.paccanaro.koda.engine;

import java.util.UUID;

/**
 * Saida do {@link AdaptiveEngine} para uma unica questao dentro de uma sessao:
 * qual concept, em qual dificuldade-alvo, e por que (UX-02 — toda selecao
 * carrega um motivo legivel). Quem resolve isto numa questao real do banco e
 * o {@code QuestionService}; o engine nunca sabe o que existe no banco.
 */
public record QuestionSpecification(
        UUID conceptId,
        int targetDifficulty,
        String selectionReason
) {
}
