package com.paccanaro.koda.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * O unico consumidor da fila de pre-geracao. Roda fora do request do aluno
 * (ARC-01) — e por isso que falha, lentidao ou indisponibilidade do LLM nunca
 * viram latencia ou erro na tela de quem esta estudando.
 *
 * <p>Processa um pedido por ciclo, de proposito: geracao e cara e nao ha
 * urgencia: o banco ja atende. Trocar isto por paralelismo so faz sentido
 * quando houver metrica de cobertura pedindo (Fase 8).
 */
@Component
public class GenerationWorker {

    private static final Logger log = LoggerFactory.getLogger(GenerationWorker.class);

    private final GenerationRequestRepository requestRepository;
    private final AiGenerationService generationService;

    public GenerationWorker(GenerationRequestRepository requestRepository, AiGenerationService generationService) {
        this.requestRepository = requestRepository;
        this.generationService = generationService;
    }

    @Scheduled(fixedDelayString = "${koda.ai.worker-interval:PT1M}")
    public void processNextPending() {
        drainOne();
    }

    /**
     * Visivel para teste: permite exercitar um ciclo sem esperar o agendador.
     *
     * @return true se havia pedido pendente e ele foi processado
     */
    @Transactional
    public boolean drainOne() {
        if (!generationService.canGenerate()) {
            return false;
        }

        List<GenerationRequest> pending =
                requestRepository.findAllByStatusOrderByCreatedAtAsc(GenerationRequestStatus.PENDING);
        if (pending.isEmpty()) {
            return false;
        }

        GenerationRequest request = pending.get(0);
        request.markProcessing();

        try {
            ValidationResult result = generationService.process(request);
            // Rejeicao pelo pipeline nao e falha do pedido: o pedido foi atendido,
            // o candidato e que nao passou. FAILED fica reservado pra erro real.
            if (result.outcome() == GenerationOutcome.REJECTED_PROVIDER_ERROR) {
                request.markFailed();
            } else {
                request.markDone();
            }
        } catch (RuntimeException e) {
            log.error("Erro inesperado ao processar pedido de geracao {}", request.getId(), e);
            request.markFailed();
        }
        return true;
    }
}
