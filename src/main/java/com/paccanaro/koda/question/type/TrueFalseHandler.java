package com.paccanaro.koda.question.type;

import com.paccanaro.koda.question.QuestionTypeHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Verdadeiro/falso. payload: {@code {prompt, code?}}. correctAnswer / submittedAnswer: {@code {value: boolean}}.
 */
@Component
public class TrueFalseHandler implements QuestionTypeHandler {

    @Override
    public String typeName() {
        return "true_false";
    }

    @Override
    public JsonNode render(JsonNode payload) {
        return payload;
    }

    @Override
    public void validatePayload(JsonNode payload, JsonNode correctAnswer) {
        QuestionTypeSupport.requirePrompt(payload);

        if (!correctAnswer.path("value").isBoolean()) {
            throw new IllegalArgumentException("correctAnswer.value precisa ser um booleano");
        }
    }

    @Override
    public boolean score(JsonNode payload, JsonNode correctAnswer, JsonNode submittedAnswer) {
        return correctAnswer.path("value").asBoolean() == submittedAnswer.path("value").asBoolean();
    }
}
