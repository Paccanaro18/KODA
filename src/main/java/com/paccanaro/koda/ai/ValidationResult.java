package com.paccanaro.koda.ai;

/**
 * Resultado do {@link ValidationPipeline}. {@code stage} e {@code detail}
 * existem para gravar em {@code ai_generations.validation_results} — sem eles
 * uma rejeicao seria invisivel e nao daria pra corrigir prompt nem medir
 * qualidade por estagio.
 */
public record ValidationResult(
        boolean accepted,
        GenerationOutcome outcome,
        String stage,
        String detail
) {

    static ValidationResult approve() {
        return new ValidationResult(true, GenerationOutcome.ACCEPTED, "todos", "aprovada nos 7 estagios");
    }

    static ValidationResult rejected(GenerationOutcome outcome, String stage, String detail) {
        return new ValidationResult(false, outcome, stage, detail);
    }
}
