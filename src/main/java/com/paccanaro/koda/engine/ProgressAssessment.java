package com.paccanaro.koda.engine;

import com.paccanaro.koda.curriculum.ProgressState;

/** Saida de {@link AdaptiveEngine#assessProgress}: o novo estado de um concept apos uma tentativa. */
public record ProgressAssessment(ProgressState state, int progressPercent) {
}
