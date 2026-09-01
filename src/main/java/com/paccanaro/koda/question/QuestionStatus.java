package com.paccanaro.koda.question;

/** Ciclo de vida de uma questao. Nesta fase todo o seed nasce PUBLISHED — nao ha fluxo de revisao humana ainda. */
public enum QuestionStatus {
    DRAFT,
    IN_REVIEW,
    PUBLISHED,
    DISABLED,
    RETIRED
}
