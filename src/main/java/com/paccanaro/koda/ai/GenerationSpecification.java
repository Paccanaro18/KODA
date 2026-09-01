package com.paccanaro.koda.ai;

import java.util.List;
import java.util.UUID;

/**
 * O que gerar — nunca um pedido vago (docs/architecture/03-estrategia-ia.md
 * secao 1). Produzida a partir de um {@code generation_requests} pendente;
 * todo campo vem do curriculo curado ou do banco de questoes, nunca de texto
 * livre de aluno — e por isso que o risco de prompt injection aqui e baixo
 * por construcao, nao por sorte (SEC-02).
 */
public record GenerationSpecification(
        UUID conceptId,
        String conceptTitle,
        String topicTitle,
        int targetDifficulty,
        String questionType,
        List<String> avoidCanonicalHashes
) {
}
