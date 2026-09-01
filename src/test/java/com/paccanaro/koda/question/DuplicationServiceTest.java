package com.paccanaro.koda.question;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DuplicationService")
class DuplicationServiceTest {

    private final DuplicationService service = new DuplicationService();

    @Test
    @DisplayName("o mesmo payload produz o mesmo hash")
    void samePayloadProducesSameHash() {
        byte[] first = service.canonicalHash("{\"prompt\":\"2+2?\"}");
        byte[] second = service.canonicalHash("{\"prompt\":\"2+2?\"}");
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("espacos extras e caixa diferente ainda produzem o mesmo hash")
    void whitespaceAndCaseDoNotAffectHash() {
        byte[] first = service.canonicalHash("{\"prompt\":\"2+2?\"}");
        byte[] second = service.canonicalHash("  {\"PROMPT\":\"2+2?\"}  ");
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("payloads diferentes produzem hashes diferentes")
    void differentPayloadsProduceDifferentHashes() {
        byte[] first = service.canonicalHash("{\"prompt\":\"2+2?\"}");
        byte[] second = service.canonicalHash("{\"prompt\":\"3+3?\"}");
        assertThat(first).isNotEqualTo(second);
    }
}
