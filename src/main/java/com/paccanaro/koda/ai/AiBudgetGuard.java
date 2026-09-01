package com.paccanaro.koda.ai;

import com.paccanaro.koda.config.KodaAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Teto de gasto diario (COST-01). Estourar o orcamento pausa a geracao —
 * e uma degradacao aceitavel porque o banco de questoes ja atende o aluno
 * (docs/architecture/03-estrategia-ia.md secao 8).
 */
@Component
public class AiBudgetGuard {

    private static final Logger log = LoggerFactory.getLogger(AiBudgetGuard.class);

    private final AiGenerationRepository generationRepository;
    private final KodaAiProperties properties;

    public AiBudgetGuard(AiGenerationRepository generationRepository, KodaAiProperties properties) {
        this.generationRepository = generationRepository;
        this.properties = properties;
    }

    public boolean withinBudget() {
        BigDecimal spent = spentToday();
        boolean within = spent.compareTo(properties.budget().dailyLimitUsd()) < 0;
        if (!within) {
            log.warn("Orcamento diario de IA estourado: gasto={} limite={}. Geracao pausada.",
                    spent, properties.budget().dailyLimitUsd());
        }
        return within;
    }

    public BigDecimal spentToday() {
        return generationRepository.sumCostSince(Instant.now().truncatedTo(ChronoUnit.DAYS));
    }
}
