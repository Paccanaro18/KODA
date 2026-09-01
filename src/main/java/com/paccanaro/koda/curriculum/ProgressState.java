package com.paccanaro.koda.curriculum;

/**
 * Estados com evidencia real de pratica. {@code locked} e {@code available}
 * nao estao aqui de proposito: eles nunca sao persistidos, sao sempre
 * calculados a partir da ausencia de linha em {@code user_concept_progress} e
 * do grafo de pre-requisitos (ver {@link CurriculumService}).
 */
public enum ProgressState {
    ACTIVE,
    COMPLETED,
    MASTERED,
    NEEDS_REVIEW
}
