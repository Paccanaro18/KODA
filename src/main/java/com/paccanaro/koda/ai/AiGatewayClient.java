package com.paccanaro.koda.ai;

/**
 * Unica porta de saida para o LLM (docs/architecture/01-arquitetura.md —
 * AI Gateway). Nenhuma outra parte do codigo fala com um provedor
 * diretamente; e o que permite trocar de provedor sem tocar no resto.
 *
 * <p>Implementacao real: {@link AnthropicAiGatewayClient}, decorada com
 * timeout, retry, backoff e circuit breaker. Falha (rede, indisponibilidade,
 * schema invalido do provedor) e sempre uma excecao — quem chama decide o
 * que fazer (ver {@link AiGenerationService}); o gateway em si nao aceita
 * nem rejeita conteudo, so fala com o provedor.
 */
public interface AiGatewayClient {

    GatewayResponse generate(GenerationSpecification specification);

    /** Identifica o modelo em uso, gravado em toda geracao (rastreabilidade). */
    String modelId();
}
