package com.paccanaro.koda.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.paccanaro.koda.config.KodaAiProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Monta o AI Gateway. Sem chave de API ou com {@code koda.ai.enabled=false},
 * instala o {@link DisabledAiGatewayClient} — a aplicacao sobe e serve alunos
 * normalmente, so nao gera conteudo novo.
 */
@Configuration
class AiGatewayConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayConfiguration.class);

    @Bean
    AiGatewayClient aiGatewayClient(KodaAiProperties properties) {
        if (!properties.enabled() || properties.apiKey() == null || properties.apiKey().isBlank()) {
            log.info("Geracao por IA desabilitada; o banco de questoes existente continua servindo normalmente.");
            return new DisabledAiGatewayClient();
        }

        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .timeout(properties.timeout())
                .build();

        return new AnthropicAiGatewayClient(client, properties,
                circuitBreaker(), retry(properties), rateLimiter());
    }

    /**
     * Abre depois de metade das chamadas falharem numa janela de 10. Meio
     * minuto aberto e suficiente: nao ha pressa para retomar geracao, e
     * insistir contra um provedor fora do ar so queima orcamento.
     */
    private CircuitBreaker circuitBreaker() {
        return CircuitBreaker.of("ai-gateway", CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofMinutes(5))
                .build());
    }

    private Retry retry(KodaAiProperties properties) {
        return Retry.of("ai-gateway", RetryConfig.custom()
                .maxAttempts(properties.maxAttempts())
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialBackoff(Duration.ofSeconds(2), 2.0))
                .build());
    }

    /** Teto de chamadas por minuto — protege orcamento e o rate limit do provedor. */
    private RateLimiter rateLimiter() {
        return RateLimiter.of("ai-gateway", RateLimiterConfig.custom()
                .limitForPeriod(20)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build());
    }
}
