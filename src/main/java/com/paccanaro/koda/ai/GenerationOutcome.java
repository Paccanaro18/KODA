package com.paccanaro.koda.ai;

/**
 * Resultado de uma geracao, aceita ou rejeitada com motivo — cada valor
 * corresponde a um dos 7 estagios do pipeline de validacao
 * (docs/architecture/03-estrategia-ia.md secao 2). Gravado em toda geracao,
 * aceita ou nao, para rastreabilidade completa (secao 20).
 */
public enum GenerationOutcome {
    ACCEPTED,
    REJECTED_SCHEMA,
    REJECTED_ANSWER,
    REJECTED_DIFFICULTY,
    REJECTED_CURRICULUM,
    REJECTED_DUPLICATE,
    REJECTED_SAFETY,
    REJECTED_QUALITY,
    /** Falha do provedor (timeout, indisponibilidade) — nao chegou a produzir conteudo pra validar. */
    REJECTED_PROVIDER_ERROR
}
