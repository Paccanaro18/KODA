package com.paccanaro.koda.ai;

/**
 * Gateway ativo quando {@code koda.ai.enabled=false} ou nao ha chave de API.
 * Recusa toda geracao, o que faz o {@link AiGenerationService} registrar
 * {@code REJECTED_PROVIDER_ERROR} e seguir — exatamente o mesmo caminho de um
 * provedor fora do ar.
 *
 * <p>E isso que permite a aplicacao subir e servir alunos normalmente sem
 * nenhuma credencial de IA configurada: geracao para, aprendizado continua.
 */
class DisabledAiGatewayClient implements AiGatewayClient {

    @Override
    public GatewayResponse generate(GenerationSpecification specification) {
        throw new IllegalStateException("Geracao por IA desabilitada (koda.ai.enabled=false ou sem chave de API)");
    }

    @Override
    public String modelId() {
        return "disabled";
    }
}
