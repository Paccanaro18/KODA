package com.paccanaro.koda.question;

import com.paccanaro.koda.common.exception.ApiException;
import com.paccanaro.koda.curriculum.Concept;
import com.paccanaro.koda.curriculum.ConceptRepository;
import com.paccanaro.koda.question.dto.AttemptResultResponse;
import com.paccanaro.koda.question.dto.PracticeQuestionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionConceptRepository questionConceptRepository;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final UserQuestionExposureRepository exposureRepository;
    private final ConceptRepository conceptRepository;
    private final QuestionTypeRegistry typeRegistry;
    private final ObjectMapper objectMapper;

    public QuestionService(QuestionRepository questionRepository,
                           QuestionVersionRepository questionVersionRepository,
                           QuestionConceptRepository questionConceptRepository,
                           QuestionAttemptRepository questionAttemptRepository,
                           UserQuestionExposureRepository exposureRepository,
                           ConceptRepository conceptRepository,
                           QuestionTypeRegistry typeRegistry,
                           ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.questionVersionRepository = questionVersionRepository;
        this.questionConceptRepository = questionConceptRepository;
        this.questionAttemptRepository = questionAttemptRepository;
        this.exposureRepository = exposureRepository;
        this.conceptRepository = conceptRepository;
        this.typeRegistry = typeRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * Selecao NAO-adaptativa: so prioriza o que o usuario viu menos (camada 4
     * de dedup). A Fase 4 (Adaptive Learning Engine) substitui isso por uma
     * decisao pedagogica de verdade — este metodo existe pra provar que o
     * banco e a correcao funcionam ponta a ponta antes do engine existir.
     */
    public List<PracticeQuestionResponse> practiceSession(UUID userId, Set<String> types, int limit) {
        List<Question> candidates = questionRepository.findAllByStatusAndQuestionTypeIn(QuestionStatus.PUBLISHED, types);

        Map<UUID, Integer> exposureByQuestion = exposureRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(UserQuestionExposure::getQuestionId, UserQuestionExposure::getTimesSeen));

        List<Question> ordered = new ArrayList<>(candidates);
        Collections.shuffle(ordered);
        ordered.sort(Comparator.comparingInt(question -> exposureByQuestion.getOrDefault(question.getId(), 0)));

        return ordered.stream()
                .limit(limit)
                .map(this::toPracticeResponse)
                .toList();
    }

    /**
     * Corrige no servidor via o handler do tipo, grava a tentativa (append-only)
     * e so agora devolve o gabarito — nunca antes da submissao.
     */
    @Transactional
    public AttemptResultResponse submitAttempt(UUID userId, UUID questionVersionId, JsonNode submittedAnswer, int responseTimeMs) {
        QuestionVersion version = questionVersionRepository.findById(questionVersionId)
                .orElseThrow(() -> ApiException.notFound("Questao"));
        Question question = questionRepository.findById(version.getQuestionId())
                .orElseThrow(() -> ApiException.notFound("Questao"));

        QuestionTypeHandler handler = typeRegistry.get(question.getQuestionType());
        JsonNode payload = objectMapper.readTree(version.getPayload());
        JsonNode correctAnswer = objectMapper.readTree(version.getCorrectAnswer());

        boolean correct = handler.score(payload, correctAnswer, submittedAnswer);

        questionAttemptRepository.save(QuestionAttempt.record(
                userId, questionVersionId, submittedAnswer.toString(), correct, responseTimeMs, version.getDeclaredDifficulty()));

        recordExposure(userId, question.getId());

        JsonNode distractorRationales = version.getDistractorRationales() != null
                ? objectMapper.readTree(version.getDistractorRationales())
                : null;

        return new AttemptResultResponse(correct, correctAnswer, version.getExplanation(),
                distractorRationales, resolveConceptTitle(version.getId()));
    }

    private PracticeQuestionResponse toPracticeResponse(Question question) {
        QuestionVersion version = currentVersion(question.getId());
        JsonNode payload = objectMapper.readTree(version.getPayload());
        JsonNode rendered = typeRegistry.get(question.getQuestionType()).render(payload);

        return new PracticeQuestionResponse(
                version.getId(), question.getQuestionType(), resolveConceptTitle(version.getId()),
                rendered, version.getEstimatedTimeSeconds());
    }

    private QuestionVersion currentVersion(UUID questionId) {
        return questionVersionRepository.findFirstByQuestionIdOrderByVersionDesc(questionId)
                .orElseThrow(() -> ApiException.notFound("Versao de questao"));
    }

    private String resolveConceptTitle(UUID questionVersionId) {
        return questionConceptRepository.findFirstByQuestionVersionId(questionVersionId)
                .flatMap(qc -> conceptRepository.findById(qc.getConceptId()))
                .map(Concept::getTitle)
                .orElse(null);
    }

    private void recordExposure(UUID userId, UUID questionId) {
        exposureRepository.findByUserIdAndQuestionId(userId, questionId)
                .ifPresentOrElse(
                        UserQuestionExposure::recordExposure,
                        () -> exposureRepository.save(UserQuestionExposure.firstExposure(userId, questionId)));
    }
}
