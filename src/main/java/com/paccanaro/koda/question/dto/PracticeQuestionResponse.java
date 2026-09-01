package com.paccanaro.koda.question.dto;

import tools.jackson.databind.JsonNode;

import java.util.UUID;

/** {@code payload} nunca contem a resposta certa — passou por {@code render()} do tipo. */
public record PracticeQuestionResponse(
        UUID questionVersionId,
        String type,
        String conceptTitle,
        JsonNode payload,
        Integer estimatedTimeSeconds
) {
}
