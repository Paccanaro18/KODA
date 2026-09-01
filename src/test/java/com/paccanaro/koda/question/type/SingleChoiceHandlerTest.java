package com.paccanaro.koda.question.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SingleChoiceHandler")
class SingleChoiceHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SingleChoiceHandler handler = new SingleChoiceHandler();

    private final JsonNode payload = objectMapper.readTree(
            "{\"prompt\":\"2+2?\",\"options\":[{\"id\":\"a\",\"label\":\"3\"},{\"id\":\"b\",\"label\":\"4\"}]}");
    private final JsonNode correctAnswer = objectMapper.readTree("{\"optionId\":\"b\"}");

    @Test
    @DisplayName("acerta quando a opcao submetida bate com a correta")
    void scoresCorrectAnswer() {
        assertThat(handler.score(payload, correctAnswer, objectMapper.readTree("{\"optionId\":\"b\"}"))).isTrue();
    }

    @Test
    @DisplayName("erra quando a opcao submetida difere")
    void scoresWrongAnswer() {
        assertThat(handler.score(payload, correctAnswer, objectMapper.readTree("{\"optionId\":\"a\"}"))).isFalse();
    }

    @Test
    @DisplayName("valida payload e correctAnswer coerentes")
    void validatesWellFormedContent() {
        handler.validatePayload(payload, correctAnswer);
    }

    @Test
    @DisplayName("rejeita menos de 2 opcoes")
    void rejectsTooFewOptions() {
        JsonNode badPayload = objectMapper.readTree("{\"prompt\":\"?\",\"options\":[{\"id\":\"a\",\"label\":\"unica\"}]}");
        assertThatThrownBy(() -> handler.validatePayload(badPayload, correctAnswer))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejeita correctAnswer apontando pra opcao inexistente")
    void rejectsDanglingCorrectOption() {
        JsonNode badAnswer = objectMapper.readTree("{\"optionId\":\"z\"}");
        assertThatThrownBy(() -> handler.validatePayload(payload, badAnswer))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
