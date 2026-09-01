package com.paccanaro.koda.question.type;

import com.paccanaro.koda.question.QuestionTypeHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Set;

/**
 * Multipla escolha, mais de uma alternativa certa. payload: {@code {prompt, options:[{id,label}]}}.
 * correctAnswer / submittedAnswer: {@code {optionIds:[...]}}. Corrige por igualdade de conjunto —
 * ordem nao importa, mas faltar ou sobrar uma opcao conta como erro.
 */
@Component
public class MultipleSelectHandler implements QuestionTypeHandler {

    @Override
    public String typeName() {
        return "multiple_select";
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
            throw new IllegalArgumentException("multiple_select exige ao menos 2 opcoes");
        }

        JsonNode correctIds = correctAnswer.path("optionIds");
        if (!correctIds.isArray() || correctIds.isEmpty()) {
            throw new IllegalArgumentException("correctAnswer.optionIds nao pode ser vazio");
        }
        for (JsonNode id : correctIds) {
            if (!QuestionTypeSupport.containsOptionId(options, id.asString(""))) {
                throw new IllegalArgumentException("correctAnswer.optionIds tem um id que nao existe em options");
            }
        }
    }

    @Override
    public boolean score(JsonNode payload, JsonNode correctAnswer, JsonNode submittedAnswer) {
        return toSet(correctAnswer.path("optionIds")).equals(toSet(submittedAnswer.path("optionIds")));
    }

    private static Set<String> toSet(JsonNode array) {
        Set<String> ids = new HashSet<>();
        for (JsonNode id : array) {
            ids.add(id.asString(""));
        }
        return ids;
    }
}
