package com.paccanaro.koda.question.dto;

import tools.jackson.databind.JsonNode;

/**
 * O gabarito ({@code correctAnswer}) so chega ao cliente aqui — nunca antes
 * da submissao (docs/architecture/03-estrategia-ia.md § 5).
 */
public record AttemptResultResponse(
        boolean correct,
        JsonNode correctAnswer,
        String explanation,
        JsonNode distractorRationales,
        String concept
) {
}
