package com.paccanaro.koda.ai;

import com.paccanaro.koda.curriculum.Concept;
import com.paccanaro.koda.curriculum.ConceptRepository;
import com.paccanaro.koda.curriculum.Topic;
import com.paccanaro.koda.curriculum.TopicRepository;
import com.paccanaro.koda.question.DuplicationService;
import com.paccanaro.koda.question.Question;
import com.paccanaro.koda.question.QuestionConcept;
import com.paccanaro.koda.question.QuestionConceptRepository;
import com.paccanaro.koda.question.QuestionRepository;
import com.paccanaro.koda.question.QuestionVersion;
import com.paccanaro.koda.question.QuestionVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orquestra uma geracao: especificacao -> {@link AiGatewayClient} ->
 * {@link ValidationPipeline} -> banco de questoes. Toda tentativa, aceita ou
 * rejeitada, vira uma linha em {@code ai_generations} com o motivo.
 *
 * <p>Nunca e chamado no caminho do request do aluno — so pelo
 * {@link GenerationWorker} (ARC-01).
 */
@Service
public class AiGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AiGenerationService.class);

    private final AiGatewayClient gatewayClient;
    private final ValidationPipeline validationPipeline;
    private final AiBudgetGuard budgetGuard;
    private final AiGenerationRepository generationRepository;
    private final ConceptRepository conceptRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionConceptRepository questionConceptRepository;
    private final DuplicationService duplicationService;
    private final ObjectMapper objectMapper;

    public AiGenerationService(AiGatewayClient gatewayClient, ValidationPipeline validationPipeline,
                               AiBudgetGuard budgetGuard, AiGenerationRepository generationRepository,
                               ConceptRepository conceptRepository, TopicRepository topicRepository,
                               QuestionRepository questionRepository, QuestionVersionRepository questionVersionRepository,
                               QuestionConceptRepository questionConceptRepository, DuplicationService duplicationService,
                               ObjectMapper objectMapper) {
        this.gatewayClient = gatewayClient;
        this.validationPipeline = validationPipeline;
        this.budgetGuard = budgetGuard;
        this.generationRepository = generationRepository;
        this.conceptRepository = conceptRepository;
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.questionVersionRepository = questionVersionRepository;
        this.questionConceptRepository = questionConceptRepository;
        this.duplicationService = duplicationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ValidationResult process(GenerationRequest request) {
        Concept concept = conceptRepository.findById(request.getConceptId()).orElseThrow();
        Topic topic = topicRepository.findById(concept.getTopicId()).orElseThrow();

        GenerationSpecification specification = new GenerationSpecification(
                concept.getId(), concept.getTitle(), topic.getName(),
                request.getTargetDifficulty(), request.getQuestionType(), List.of());

        String specificationJson = objectMapper.writeValueAsString(specification);

        GatewayResponse response;
        try {
            response = gatewayClient.generate(specification);
        } catch (RuntimeException e) {
            // Falha de provedor nao afeta o aluno: o banco ja tem questoes.
            log.warn("Falha do provedor de IA para concept={}: {}", concept.getId(), e.toString());
            ValidationResult failure = ValidationResult.rejected(
                    GenerationOutcome.REJECTED_PROVIDER_ERROR, "0-gateway", e.getClass().getSimpleName());
            generationRepository.save(AiGeneration.record(request.getId(), null, gatewayClient.modelId(),
                    QuestionGenerationPrompts.VERSION, specificationJson, null,
                    objectMapper.writeValueAsString(failure), failure.outcome(), null, null, null, null));
            return failure;
        }

        ValidationResult validation = validationPipeline.validate(
                response.parsed(), specification, existingHashesFor(concept.getId()));

        UUID questionId = validation.accepted()
                ? persistQuestion(response.parsed(), specification, topic.getId())
                : null;

        generationRepository.save(AiGeneration.record(request.getId(), questionId, response.model(),
                QuestionGenerationPrompts.VERSION, specificationJson,
                objectMapper.writeValueAsString(response.parsed()),
                objectMapper.writeValueAsString(validation), validation.outcome(),
                response.inputTokens(), response.outputTokens(), response.costUsd(), (int) response.latencyMs()));

        if (validation.accepted()) {
            log.info("Questao gerada aprovada e em revisao: id={} concept={}", questionId, concept.getId());
        } else {
            log.info("Questao gerada rejeitada no estagio {}: {}", validation.stage(), validation.detail());
        }
        return validation;
    }

    public boolean canGenerate() {
        return budgetGuard.withinBudget();
    }

    private UUID persistQuestion(GeneratedQuestion candidate, GenerationSpecification specification, UUID topicId) {
        Question question = questionRepository.save(Question.generated(
                uniqueSlug(specification), topicId, specification.questionType()));

        QuestionVersion version = questionVersionRepository.save(QuestionVersion.firstVersion(
                question.getId(), candidate.payloadJson(), candidate.correctAnswerJson(), candidate.explanation(),
                candidate.distractorRationalesJson(), candidate.declaredDifficulty(),
                candidate.estimatedTimeSeconds(), duplicationService.canonicalHash(candidate.payloadJson()),
                BigDecimal.valueOf(candidate.confidence())));

        questionConceptRepository.save(QuestionConcept.link(version.getId(), specification.conceptId()));
        return question.getId();
    }

    /** Slug tem indice unico; o sufixo aleatorio evita colisao entre geracoes do mesmo concept. */
    private String uniqueSlug(GenerationSpecification specification) {
        String base = specification.conceptTitle().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return base + "-ia-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Set<String> existingHashesFor(UUID conceptId) {
        return questionConceptRepository.findAllByConceptId(conceptId).stream()
                .map(QuestionConcept::getQuestionVersionId)
                .map(questionVersionRepository::findById)
                .flatMap(Optional::stream)
                .map(version -> HexFormat.of().formatHex(version.getCanonicalHash()))
                .collect(Collectors.toSet());
    }
}
