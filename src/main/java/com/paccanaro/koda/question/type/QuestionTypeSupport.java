package com.paccanaro.koda.question.type;

import tools.jackson.databind.JsonNode;

/** Checagens repetidas entre os handlers de tipo. Nao e parte do contrato publico. */
final class QuestionTypeSupport {

    private QuestionTypeSupport() {
    }

    static void requirePrompt(JsonNode payload) {
        String prompt = payload.path("prompt").asString(null);
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("payload.prompt e obrigatorio");
        }
    }

    static boolean containsOptionId(JsonNode options, String optionId) {
        for (JsonNode option : options) {
            if (optionId.equals(option.path("id").asString(null))) {
                return true;
            }
        }
        return false;
    }

    static String normalize(String text) {
        return text.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
