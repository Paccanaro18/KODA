package com.paccanaro.koda.ai;

import java.math.BigDecimal;

/** O que veio do provedor numa chamada bem-sucedida, com o custo ja calculado. */
public record GatewayResponse(
        GeneratedQuestion parsed,
        String model,
        int inputTokens,
        int outputTokens,
        BigDecimal costUsd,
        long latencyMs
) {
}
