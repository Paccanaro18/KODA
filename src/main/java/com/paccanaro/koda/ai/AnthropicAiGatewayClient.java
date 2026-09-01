package com.paccanaro.koda.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.paccanaro.koda.config.KodaAiProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Supplier;

/**
 * Implementacao real do {@link AiGatewayClient}, sobre a SDK oficial da
 * Anthropic. Concentra timeout, retry com backoff, circuit breaker e rate
 * limit — as quatro defesas que o ADR-0001 previu para esta fronteira.
 *
 * <p>O circuit breaker abrindo e a propriedade que torna falha de IA
 * toleravel: geracao pausa, o banco de questoes continua servindo, e o aluno
 * nao percebe (docs/architecture/03-estrategia-ia.md secao 8).
 *
 * <p>Nada do conteudo gerado e validado aqui — o gateway so fala com o
 * provedor. Validacao e {@link ValidationPipeline}.
 */
class AnthropicAiGatewayClient implements AiGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicAiGatewayClient.class);
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);

    private final AnthropicClient client;
    private final KodaAiProperties properties;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RateLimiter rateLimiter;

    AnthropicAiGatewayClient(AnthropicClient client, KodaAiProperties properties,
                             CircuitBreaker circuitBreaker, Retry retry, RateLimiter rateLimiter) {
        this.client = client;
        this.properties = properties;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public String modelId() {
        return properties.model();
    }

    /**
     * Ordem das defesas (de fora pra dentro): circuit breaker envolve o retry,
     * que envolve o rate limiter. Assim as tentativas de retry contam como UMA
     * chamada para o breaker — o contrario abriria o circuito por um unico
     * pedido que falhou tres vezes.
     */
    @Override
    public GatewayResponse generate(GenerationSpecification specification) {
        Supplier<GatewayResponse> call = () -> callProvider(specification);
        Supplier<GatewayResponse> limited = RateLimiter.decorateSupplier(rateLimiter, call);
        Supplier<GatewayResponse> retried = Retry.decorateSupplier(retry, limited);
        Supplier<GatewayResponse> guarded = CircuitBreaker.decorateSupplier(circuitBreaker, retried);
        return guarded.get();
    }

    private GatewayResponse callProvider(GenerationSpecification specification) {
        long startedAt = System.nanoTime();

        StructuredMessageCreateParams<GeneratedQuestion> params = MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(8000L)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .system(QuestionGenerationPrompts.system())
                .addUserMessage(QuestionGenerationPrompts.user(specification))
                .outputConfig(GeneratedQuestion.class)
                .build();

        var response = client.messages().create(params);
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;

        GeneratedQuestion parsed = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Provedor nao retornou bloco de texto estruturado"));

        int inputTokens = (int) response.usage().inputTokens();
        int outputTokens = (int) response.usage().outputTokens();

        log.info("Geracao concluida: concept={} tipo={} tokens_in={} tokens_out={} latencia_ms={}",
                specification.conceptId(), specification.questionType(), inputTokens, outputTokens, latencyMs);

        return new GatewayResponse(parsed, properties.model(), inputTokens, outputTokens,
                cost(inputTokens, outputTokens), latencyMs);
    }

    private BigDecimal cost(int inputTokens, int outputTokens) {
        BigDecimal input = properties.pricing().inputPerMillionUsd()
                .multiply(BigDecimal.valueOf(inputTokens)).divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
        BigDecimal output = properties.pricing().outputPerMillionUsd()
                .multiply(BigDecimal.valueOf(outputTokens)).divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
        return input.add(output);
    }
}
