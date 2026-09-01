package com.paccanaro.koda.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiGenerationRepository extends JpaRepository<AiGeneration, UUID> {

    /**
     * Soma do custo desde {@code since}, para o orcamento (COST-01). Agregado
     * no banco, nao em Java: ao contrario das tabelas de curriculo/banco de
     * questoes (escala de seed), esta tabela cresce sem teto — trazer tudo
     * pra memoria pra somar nao escala.
     */
    @Query("SELECT COALESCE(SUM(g.costUsd), 0) FROM AiGeneration g WHERE g.createdAt >= :since")
    BigDecimal sumCostSince(@Param("since") Instant since);

    List<AiGeneration> findAllByOrderByCreatedAtDesc();
}
