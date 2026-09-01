package com.paccanaro.koda.question;

import tools.jackson.databind.JsonNode;

/**
 * Contrato de um tipo de questao: renderizar, validar, corrigir. Um tipo novo
 * e um {@code @Component} novo implementando esta interface — nada no nucleo
 * (registry, service, controller) muda (MNT-01).
 *
 * <p>{@code payload} nunca contem a resposta certa; ela vive separada em
 * {@code correctAnswer} e so e revelada depois da submissao.
 */
public interface QuestionTypeHandler {

    /** Casa com {@code questions.question_type} no banco. */
    String typeName();

    /** Vista segura pro cliente. Identidade na maioria dos tipos — existe pra tipos futuros que precisem esconder algo do payload. */
    JsonNode render(JsonNode payload);

    /** @throws IllegalArgumentException se payload ou correctAnswer estiverem malformados */
    void validatePayload(JsonNode payload, JsonNode correctAnswer);

    boolean score(JsonNode payload, JsonNode correctAnswer, JsonNode submittedAnswer);
}
