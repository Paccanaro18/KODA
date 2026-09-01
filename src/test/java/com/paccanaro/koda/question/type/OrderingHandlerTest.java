package com.paccanaro.koda.question.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderingHandler")
class OrderingHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OrderingHandler handler = new OrderingHandler();

    private final JsonNode payload = objectMapper.readTree(
            "{\"prompt\":\"?\",\"items\":[{\"id\":\"1\",\"label\":\"Primeiro\"},{\"id\":\"2\",\"label\":\"Segundo\"},{\"id\":\"3\",\"label\":\"Terceiro\"}]}");
    private final JsonNode correctAnswer = objectMapper.readTree("{\"order\":[\"1\",\"2\",\"3\"]}");

    @Test
    @DisplayName("acerta com a mesma sequencia exata")
    void scoresExactSequence() {
        assertThat(handler.score(payload, correctAnswer, objectMapper.readTree("{\"order\":[\"1\",\"2\",\"3\"]}"))).isTrue();
    }

    @Test
    @DisplayName("erra com os mesmos itens fora de ordem")
    void scoresSameItemsWrongOrderAsWrong() {
        assertThat(handler.score(payload, correctAnswer, objectMapper.readTree("{\"order\":[\"2\",\"1\",\"3\"]}"))).isFalse();
    }

    @Test
    @DisplayName("rejeita correctAnswer.order que nao e permutacao dos items")
    void rejectsOrderThatIsNotAPermutation() {
        JsonNode bad = objectMapper.readTree("{\"order\":[\"1\",\"2\",\"4\"]}");
        assertThatThrownBy(() -> handler.validatePayload(payload, bad))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
