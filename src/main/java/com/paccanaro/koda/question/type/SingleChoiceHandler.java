package com.paccanaro.koda.question.type;

import com.paccanaro.koda.question.QuestionTypeHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Multipla escolha, uma alternativa certa. payload: {@code {prompt, code?, options:[{id,label}]}}.
 * correctAnswer: {@code {optionId}}. submittedAnswer: {@code {optionId}}.
 */
@Component
public class SingleChoiceHandler implements QuestionTypeHandler {

    @Override
    public String typeName() {
        return "single_choice";
    }

    @Override
    public JsonNode render(JsonNode payload) {
        return payload;
    }

    @Override
    public void validatePayload(JsonNode payload, JsonNode correctAnswer) {
        QuestionTypeSupport.requirePrompt(payload);

        JsonNode options = payload.path("options");
        if (!options.isArray() || options.size() < 2) {
            throw new IllegalArgumentException("single_choice exige ao menos 2 opcoes");
        }

        String correctOptionId = correctAnswer.path("optionId").asString(null);
        if (correctOptionId == null || !QuestionTypeSupport.containsOptionId(options, correctOptionId)) {
            throw new IllegalArgumentException("correctAnswer.optionId precisa apontar para uma opcao existente");
        }
    }

    @Override
    public boolean score(JsonNode payload, JsonNode correctAnswer, JsonNode submittedAnswer) {
        String expected = correctAnswer.path("optionId").asString(null);
        String submitted = submittedAnswer.path("optionId").asString(null);
        return expected != null && expected.equals(submitted);
    }
}
