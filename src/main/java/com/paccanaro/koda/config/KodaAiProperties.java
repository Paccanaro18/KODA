package com.paccanaro.koda.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Configuracao do AI Gateway (Fase 5). A chave de API nao tem default e nunca
 * aparece em log — vem do ambiente (SEC-05). A aplicacao sobe sem ela: quando
 * {@code enabled} e falso, nenhuma geracao acontece e o aluno nao e afetado
 * (o banco de questoes ja esta populado).
 */
@Validated
@ConfigurationProperties(prefix = "koda.ai")
public record KodaAiProperties(

        boolean enabled,
        @NotBlank String model,
        String apiKey,
        @NotNull Duration timeout,
        @Min(1) int maxAttempts,
        @NotNull @Valid Budget budget,
        @NotNull @Valid Pricing pricing,
        @NotNull @Valid Quality quality
) {

    /** Circuit breaker abre ao estourar o orcamento; geracao pausa e o aluno segue servido pelo banco. */
    public record Budget(
            @NotNull BigDecimal dailyLimitUsd
    ) {
    }

    /** Precos por milhao de tokens, para calcular custo por geracao (COST-01). Configuravel: preco muda sem release. */
    public record Pricing(
            @NotNull BigDecimal inputPerMillionUsd,
            @NotNull BigDecimal outputPerMillionUsd
    ) {
    }

    /** Limiares do estagio 7 do pipeline de validacao. */
    public record Quality(
            @NotNull BigDecimal minConfidence
    ) {
    }
}
