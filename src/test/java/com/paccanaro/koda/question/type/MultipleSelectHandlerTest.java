package com.paccanaro.koda.question.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MultipleSelectHandler")
class MultipleSelectHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MultipleSelectHandler handler = new MultipleSelectHandler();

    private final JsonNode payload = objectMapper.readTree(
            "{\"prompt\":\"?\",\"options\":[{\"id\":\"a\",\"label\":\"A\"},{\"id\":\"b\",\"label\":\"B\"},{\"id\":\"c\",\"label\":\"C\"}]}");
    private final JsonNode correctAnswer = objectMapper.readTree("{\"optionIds\":[\"a\",\"c\"]}");

    @Test
    @DisplayName("acerta com o mesmo conjunto, em qualquer ordem")
    void scoresCorrectSetRegardlessOfOrder() {
        assertThat(handler.score(payload, correctAnswer, objectMapper.readTree("{\"optionIds\":[\"c\",\"a\"]}"))).isTrue();
    }

    @Test
    @DisplayName("erra se faltar uma opcao do conjunto")
    void scoresIncompleteSetAsWrong() {
        assertThat(handler.score(payload, correctAnswer, objectMapper.readTree("{\"optionIds\":[\"a\"]}"))).isFalse();
    }

    @Test
    @DisplayName("erra se sobrar uma opcao alem do conjunto certo")
    void scoresExtraOptionAsWrong() {
        assertThat(handler.score(payload, correctAnswer, objectMapper.readTree("{\"optionIds\":[\"a\",\"b\",\"c\"]}"))).isFalse();
    }

    @Test
    @DisplayName("rejeita correctAnswer vazio")
    void rejectsEmptyCorrectAnswer() {
        JsonNode empty = objectMapper.readTree("{\"optionIds\":[]}");
        assertThatThrownBy(() -> handler.validatePayload(payload, empty))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
