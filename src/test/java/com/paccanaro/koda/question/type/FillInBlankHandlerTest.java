package com.paccanaro.koda.question.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FillInBlankHandler")
class FillInBlankHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FillInBlankHandler handler = new FillInBlankHandler();

    private final JsonNode payload = objectMapper.readTree("{\"prompt\":\"?\"}");
    private final JsonNode correctAnswer = objectMapper.readTree("{\"text\":\"Decimal\",\"acceptable\":[\"ponto flutuante\"]}");

    @Test
    @DisplayName("acerta ignorando caixa e espacos nas pontas")
    void scoresCaseAndWhitespaceInsensitively() {
        assertThat(handler.score(payload, correctAnswer, objectMapper.readTree("{\"text\":\"  decimal \"}"))).isTrue();
    }

    @Test
    @DisplayName("acerta com uma alternativa aceitavel")
    void scoresAcceptableAlternative() {
        assertThat(handler.score(payload, correctAnswer, objectMapper.readTree("{\"text\":\"Ponto Flutuante\"}"))).isTrue();
    }

    @Test
    @DisplayName("erra uma resposta sem relacao")
    void scoresUnrelatedTextAsWrong() {
        assertThat(handler.score(payload, correctAnswer, objectMapper.readTree("{\"text\":\"inteiro\"}"))).isFalse();
    }

    @Test
    @DisplayName("rejeita correctAnswer sem texto")
    void rejectsBlankCorrectText() {
        JsonNode bad = objectMapper.readTree("{\"text\":\"\"}");
        assertThatThrownBy(() -> handler.validatePayload(payload, bad))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
