package com.paccanaro.koda.question.type;

import com.paccanaro.koda.question.QuestionTypeHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Resposta curta em texto. payload: {@code {prompt, code?}}.
 * correctAnswer: {@code {text, acceptable?: [...]}}. submittedAnswer: {@code {text}}.
 *
 * <p>Corrige por match normalizado (trim + minusculas) contra {@code text} e
 * qualquer alternativa em {@code acceptable} — sem isso, "Decimal" e "decimal"
 * contariam como respostas diferentes.
 */
@Component
public class FillInBlankHandler implements QuestionTypeHandler {

    @Override
    public String typeName() {
        return "fill_in_blank";
    }

    @Override
    public JsonNode render(JsonNode payload) {
        return payload;
    }

    @Override
    public void validatePayload(JsonNode payload, JsonNode correctAnswer) {
        QuestionTypeSupport.requirePrompt(payload);

        String text = correctAnswer.path("text").asString(null);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("correctAnswer.text e obrigatorio");
        }
    }

    @Override
    public boolean score(JsonNode payload, JsonNode correctAnswer, JsonNode submittedAnswer) {
        String submitted = QuestionTypeSupport.normalize(submittedAnswer.path("text").asString(""));

        if (submitted.equals(QuestionTypeSupport.normalize(correctAnswer.path("text").asString("")))) {
            return true;
        }
        for (JsonNode acceptable : correctAnswer.path("acceptable")) {
            if (submitted.equals(QuestionTypeSupport.normalize(acceptable.asString("")))) {
                return true;
            }
        }
        return false;
    }
}
