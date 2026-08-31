package com.paccanaro.koda.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Configuracao de seguranca. Nao ha default para o segredo: a aplicacao nao
 * sobe sem que ele seja fornecido pelo ambiente (SEC-05).
 */
@Validated
@ConfigurationProperties(prefix = "koda.security")
public record KodaSecurityProperties(

        @NotNull @Valid Jwt jwt,
        @NotNull @Valid RateLimit rateLimit
) {

    /** Tamanho minimo do segredo HS256, em bytes. */
    private static final int MIN_SECRET_BYTES = 32;

    public record Jwt(
            @NotBlank String secret,
            @NotBlank String issuer,
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl
    ) {
        public Jwt {
            if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
                throw new IllegalStateException(
                        "KODA_JWT_SECRET precisa ter no minimo " + MIN_SECRET_BYTES
                                + " bytes para HS256. Gere um com: openssl rand -base64 48");
            }
        }
    }

    public record RateLimit(
            @Min(1) int requestsPerMinute,
            @Min(1) int loginMaxAttempts,
            @NotNull Duration loginLockout
    ) {
    }
}
