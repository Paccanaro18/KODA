package com.paccanaro.koda.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Alvo de structured output do SDK da Anthropic — schema derivado
 * automaticamente deste record (usa Jackson <b>2</b> internamente, dependencia
 * propria do SDK; nao confundir com {@code tools.jackson} usado no resto do
 * projeto).
 *
 * <p>{@code payloadJson} e {@code correctAnswerJson} sao o payload/correctAnswer
 * do tipo pedido, como texto JSON — nao um record por tipo. Isso deixa o
 * schema fixo e simples, e reaproveita a validacao especifica de cada tipo
 * que ja existe em {@link com.paccanaro.koda.question.QuestionTypeHandler}
 * (MNT-01: um tipo novo nao exige um record de geracao novo). O prompt
 * (ver {@link QuestionGenerationPrompts}) instrui exatamente qual forma cada
 * campo deve ter, com exemplo, pro tipo pedido.
 */
public record GeneratedQuestion(

        @JsonPropertyDescription("Enunciado e dados da questao como JSON, na forma exata do tipo pedido no prompt.")
        String payloadJson,

        @JsonPropertyDescription("A resposta correta como JSON, na forma exata do tipo pedido no prompt.")
        String correctAnswerJson,

        @JsonPropertyDescription("Explicacao pedagogica de por que a resposta certa esta certa. Nunca vazia.")
        String explanation,

        @JsonPropertyDescription("Por que cada alternativa errada esta errada, como objeto JSON {\"id\": \"motivo\"}. Nulo se o tipo nao tiver alternativas.")
        String distractorRationalesJson,

        @JsonPropertyDescription("Dificuldade real percebida, 1 a 5 — pode divergir da pedida se o conteudo natural da questao pedir.")
        int declaredDifficulty,

        @JsonPropertyDescription("Tempo estimado de resposta em segundos.")
        int estimatedTimeSeconds,

        @JsonPropertyDescription("Confianca do proprio modelo na qualidade e correcao da questao, 0.0 a 1.0.")
        double confidence
) {
}
