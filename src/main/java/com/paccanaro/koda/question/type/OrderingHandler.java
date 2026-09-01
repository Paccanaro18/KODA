package com.paccanaro.koda.question.type;

import com.paccanaro.koda.question.QuestionTypeHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ordenar passos. payload: {@code {prompt, items:[{id,label}]}}.
 * correctAnswer / submittedAnswer: {@code {order:[id,...]}} — a sequencia importa, ao contrario de multiple_select.
 */
@Component
public class OrderingHandler implements QuestionTypeHandler {

    @Override
    public String typeName() {
        return "ordering";
    }

    @Override
    public JsonNode render(JsonNode payload) {
        return payload;
    }

    @Override
    public void validatePayload(JsonNode payload, JsonNode correctAnswer) {
        QuestionTypeSupport.requirePrompt(payload);

        JsonNode items = payload.path("items");
        if (!items.isArray() || items.size() < 2) {
            throw new IllegalArgumentException("ordering exige ao menos 2 items");
        }

        Set<String> itemIds = new HashSet<>();
        for (JsonNode item : items) {
            itemIds.add(item.path("id").asString(""));
        }

        JsonNode order = correctAnswer.path("order");
        if (!order.isArray() || order.size() != items.size()) {
            throw new IllegalArgumentException("correctAnswer.order precisa ter o mesmo tamanho de items");
        }
        Set<String> orderIds = new HashSet<>();
        for (JsonNode id : order) {
            orderIds.add(id.asString(""));
        }
        if (!orderIds.equals(itemIds)) {
            throw new IllegalArgumentException("correctAnswer.order precisa ser uma permutacao dos ids de items");
        }
    }

    @Override
    public boolean score(JsonNode payload, JsonNode correctAnswer, JsonNode submittedAnswer) {
        return toList(correctAnswer.path("order")).equals(toList(submittedAnswer.path("order")));
    }

    private static List<String> toList(JsonNode array) {
        List<String> ids = new ArrayList<>();
        for (JsonNode id : array) {
            ids.add(id.asString(""));
        }
        return ids;
    }
}
