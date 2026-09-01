package com.paccanaro.koda.question.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TrueFalseHandler")
class TrueFalseHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TrueFalseHandler handler = new TrueFalseHandler();

    private final JsonNode payload = objectMapper.readTree("{\"prompt\":\"O ceu e azul.\"}");
    private final JsonNode correctAnswer = objectMapper.readTree("{\"value\":true}");

    @Test
    @DisplayName("acerta quando o booleano bate")
    void scoresMatchingBoolean() {
        assertThat(handler.score(payload, correctAnswer, objectMapper.readTree("{\"value\":true}"))).isTrue();
    }

    @Test
    @DisplayName("erra quando o booleano diverge")
    void scoresDivergingBoolean() {
        assertThat(handler.score(payload, correctAnswer, objectMapper.readTree("{\"value\":false}"))).isFalse();
    }

    @Test
    @DisplayName("rejeita correctAnswer sem campo value booleano")
    void rejectsNonBooleanCorrectAnswer() {
        JsonNode bad = objectMapper.readTree("{\"value\":\"sim\"}");
        assertThatThrownBy(() -> handler.validatePayload(payload, bad))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
