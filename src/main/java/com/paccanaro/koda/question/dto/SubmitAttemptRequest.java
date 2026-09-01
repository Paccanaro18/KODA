package com.paccanaro.koda.question.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

public record SubmitAttemptRequest(
        @NotNull UUID questionVersionId,
        @NotNull JsonNode submittedAnswer,
        @PositiveOrZero int responseTimeMs
) {
}
