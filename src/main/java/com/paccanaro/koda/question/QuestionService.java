package com.paccanaro.koda.question;

import com.paccanaro.koda.common.exception.ApiException;
import com.paccanaro.koda.curriculum.Concept;
import com.paccanaro.koda.curriculum.ConceptRepository;
import com.paccanaro.koda.curriculum.CurriculumService;
import com.paccanaro.koda.curriculum.ProgressState;
import com.paccanaro.koda.curriculum.UserConceptProgress;
import com.paccanaro.koda.curriculum.UserConceptProgressRepository;
import com.paccanaro.koda.engine.AdaptiveEngine;
import com.paccanaro.koda.engine.ConceptKnowledge;
import com.paccanaro.koda.engine.CurriculumConcept;
import com.paccanaro.koda.engine.LearnerProfile;
import com.paccanaro.koda.engine.ProgressAssessment;
import com.paccanaro.koda.engine.QuestionSpecification;
import com.paccanaro.koda.question.dto.AttemptResultResponse;
import com.paccanaro.koda.question.dto.PracticeQuestionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    /** Janela de tentativas mais recentes usada pelo engine pra medir "precisao recente" (ver AdaptiveEngine). */
    private static final int RECENT_WINDOW = 5;

    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionConceptRepository questionConceptRepository;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final UserQuestionExposureRepository exposureRepository;
    private final ConceptRepository conceptRepository;
    private final UserConceptProgressRepository progressRepository;
    private final CurriculumService curriculumService;
    private final QuestionTypeRegistry typeRegistry;
    private final AdaptiveEngine adaptiveEngine;
    private final ObjectMapper objectMapper;

    public QuestionService(QuestionRepository questionRepository,
                           QuestionVersionRepository questionVersionRepository,
                           QuestionConceptRepository questionConceptRepository,
                           QuestionAttemptRepository questionAttemptRepository,
                           UserQuestionExposureRepository exposureRepository,
                           ConceptRepository conceptRepository,
                           UserConceptProgressRepository progressRepository,
                           CurriculumService curriculumService,
                           QuestionTypeRegistry typeRegistry,
                           AdaptiveEngine adaptiveEngine,
                           ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.questionVersionRepository = questionVersionRepository;
        this.questionConceptRepository = questionConceptRepository;
        this.questionAttemptRepository = questionAttemptRepository;
        this.exposureRepository = exposureRepository;
        this.conceptRepository = conceptRepository;
        this.progressRepository = progressRepository;
        this.curriculumService = curriculumService;
        this.typeRegistry = typeRegistry;
        this.adaptiveEngine = adaptiveEngine;
        this.objectMapper = objectMapper;
    }

    /**
     * Sessao adaptativa: o {@link AdaptiveEngine} decide quais concepts
     * priorizar (revisao, foco atual, concept novo) e em qual dificuldade;
     * aqui resolvemos cada especificacao na melhor questao publicada
     * disponivel. Quando o engine nao tem prioridade suficiente pra encher a
     * sessao inteira (ex.: tudo dominado e nada pra revisar ainda), o restante
     * e preenchido em pratica livre — mesmo criterio de exposicao da Fase 3.
     */
    public List<PracticeQuestionResponse> practiceSession(UUID userId, Set<String> types, int limit) {
        List<CurriculumConcept> curriculumOrder = curriculumService.curriculumOrderedConcepts().stream()
                .map(concept -> new CurriculumConcept(concept.getId(), concept.getTitle()))
                .toList();
        Map<UUID, List<UUID>> prerequisites = curriculumService.prerequisitesByConcept();
        LearnerProfile profile = buildLearnerProfile(userId);

        List<QuestionSpecification> specs = adaptiveEngine.buildSession(profile, curriculumOrder, prerequisites, limit);

        Map<UUID, Integer> exposureByQuestion = exposureRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(UserQuestionExposure::getQuestionId, UserQuestionExposure::getTimesSeen));

        List<PracticeQuestionResponse> session = new ArrayList<>();
        Set<UUID> chosenQuestionIds = new HashSet<>();

        for (QuestionSpecification spec : specs) {
            findBestMatch(spec.conceptId(), spec.targetDifficulty(), types, exposureByQuestion, chosenQuestionIds)
                    .ifPresent(question -> {
                        chosenQuestionIds.add(question.getId());
                        session.add(toPracticeResponse(question, spec.selectionReason()));
                    });
        }

        if (session.size() < limit) {
            fillWithGeneralPractice(session, chosenQuestionIds, types, exposureByQuestion, limit);
        }

        return session;
    }

    /**
     * Corrige no servidor via o handler do tipo, grava a tentativa (append-only),
     * atualiza o progresso do concept e so entao devolve o gabarito — nunca
     * antes da submissao.
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
        resolveConceptId(version.getId()).ifPresent(conceptId -> updateConceptProgress(userId, conceptId));

        JsonNode distractorRationales = version.getDistractorRationales() != null
                ? objectMapper.readTree(version.getDistractorRationales())
                : null;

        return new AttemptResultResponse(correct, correctAnswer, version.getExplanation(),
                distractorRationales, resolveConceptTitle(version.getId()));
    }

    private LearnerProfile buildLearnerProfile(UUID userId) {
        List<QuestionAttempt> attempts = questionAttemptRepository.findAllByUserId(userId);

        Map<UUID, UUID> conceptByVersion = new HashMap<>();
        for (UUID versionId : attempts.stream().map(QuestionAttempt::getQuestionVersionId).collect(Collectors.toSet())) {
            resolveConceptId(versionId).ifPresent(conceptId -> conceptByVersion.put(versionId, conceptId));
        }

        Map<UUID, List<QuestionAttempt>> attemptsByConcept = attempts.stream()
                .filter(attempt -> conceptByVersion.containsKey(attempt.getQuestionVersionId()))
                .collect(Collectors.groupingBy(attempt -> conceptByVersion.get(attempt.getQuestionVersionId())));

        Map<UUID, ConceptKnowledge> knowledgeByConcept = new HashMap<>();
        attemptsByConcept.forEach((conceptId, conceptAttempts) ->
                knowledgeByConcept.put(conceptId, toConceptKnowledge(conceptId, conceptAttempts)));

        Map<UUID, ProgressState> progressStateByConcept = progressRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(UserConceptProgress::getConceptId, UserConceptProgress::getState));

        return new LearnerProfile(userId, knowledgeByConcept, progressStateByConcept);
    }

    private ConceptKnowledge toConceptKnowledge(UUID conceptId, List<QuestionAttempt> attempts) {
        List<QuestionAttempt> byRecency = attempts.stream()
                .sorted(Comparator.comparing(QuestionAttempt::getCreatedAt).reversed())
                .toList();

        int total = byRecency.size();
        int correct = (int) byRecency.stream().filter(QuestionAttempt::isCorrect).count();

        List<QuestionAttempt> recent = byRecency.stream().limit(RECENT_WINDOW).toList();
        double recentAccuracy = recent.stream().filter(QuestionAttempt::isCorrect).count() / (double) recent.size();

        int consecutiveErrors = 0;
        for (QuestionAttempt attempt : byRecency) {
            if (attempt.isCorrect()) {
                break;
            }
            consecutiveErrors++;
        }

        int highestDifficultyCorrect = byRecency.stream()
                .filter(QuestionAttempt::isCorrect)
                .mapToInt(QuestionAttempt::getDifficultyAtTime)
                .max().orElse(0);

        QuestionAttempt mostRecent = byRecency.get(0);
        return new ConceptKnowledge(conceptId, total, correct, recentAccuracy, consecutiveErrors,
                mostRecent.getDifficultyAtTime(), highestDifficultyCorrect, mostRecent.getCreatedAt());
    }

    private void updateConceptProgress(UUID userId, UUID conceptId) {
        List<QuestionAttempt> attemptsForConcept = questionAttemptRepository.findAllByUserId(userId).stream()
                .filter(attempt -> resolveConceptId(attempt.getQuestionVersionId()).filter(conceptId::equals).isPresent())
                .toList();
        if (attemptsForConcept.isEmpty()) {
            return;
        }
        ConceptKnowledge knowledge = toConceptKnowledge(conceptId, attemptsForConcept);

        progressRepository.findByUserIdAndConceptId(userId, conceptId).ifPresentOrElse(
                progress -> {
                    ProgressAssessment assessment = adaptiveEngine.assessProgress(progress.getState(), knowledge);
                    progress.applyAssessment(assessment.state(), assessment.progressPercent());
                },
                () -> {
                    ProgressAssessment assessment = adaptiveEngine.assessProgress(null, knowledge);
                    progressRepository.save(UserConceptProgress.start(
                            userId, conceptId, assessment.state(), assessment.progressPercent()));
                });
    }

    private Optional<UUID> resolveConceptId(UUID questionVersionId) {
        return questionConceptRepository.findFirstByQuestionVersionId(questionVersionId).map(QuestionConcept::getConceptId);
    }

    /** Entre as questoes publicadas que testam o concept, a mais proxima da dificuldade-alvo; empate resolvido pela menor exposicao. */
    private Optional<Question> findBestMatch(UUID conceptId, int targetDifficulty, Set<String> allowedTypes,
                                                        Map<UUID, Integer> exposureByQuestion, Set<UUID> excludedQuestionIds) {
        List<Question> candidates = new ArrayList<>();
        Map<UUID, Integer> difficultyByQuestion = new HashMap<>();

        for (QuestionConcept mapping : questionConceptRepository.findAllByConceptId(conceptId)) {
            QuestionVersion taggedVersion = questionVersionRepository.findById(mapping.getQuestionVersionId()).orElse(null);
            if (taggedVersion == null) {
                continue;
            }
            Question question = questionRepository.findByIdAndStatus(taggedVersion.getQuestionId(), QuestionStatus.PUBLISHED)
                    .orElse(null);
            if (question == null || excludedQuestionIds.contains(question.getId()) || !allowedTypes.contains(question.getQuestionType())) {
                continue;
            }
            candidates.add(question);
            difficultyByQuestion.put(question.getId(), currentVersion(question.getId()).getDeclaredDifficulty());
        }

        return candidates.stream().min(
                Comparator.<Question>comparingInt(q -> Math.abs(difficultyByQuestion.get(q.getId()) - targetDifficulty))
                        .thenComparingInt(q -> exposureByQuestion.getOrDefault(q.getId(), 0))
                        .thenComparing(Question::getId));
    }

    private void fillWithGeneralPractice(List<PracticeQuestionResponse> session, Set<UUID> excludedQuestionIds,
                                         Set<String> types, Map<UUID, Integer> exposureByQuestion, int limit) {
        List<Question> candidates = new ArrayList<>(
                questionRepository.findAllByStatusAndQuestionTypeIn(QuestionStatus.PUBLISHED, types));
        candidates.removeIf(question -> excludedQuestionIds.contains(question.getId()));
        Collections.shuffle(candidates);
        candidates.sort(Comparator.comparingInt(q -> exposureByQuestion.getOrDefault(q.getId(), 0)));

        for (Question question : candidates) {
            if (session.size() >= limit) {
                return;
            }
            session.add(toPracticeResponse(question, "Prática livre"));
        }
    }

    private PracticeQuestionResponse toPracticeResponse(Question question, String selectionReason) {
        QuestionVersion version = currentVersion(question.getId());
        JsonNode payload = objectMapper.readTree(version.getPayload());
        JsonNode rendered = typeRegistry.get(question.getQuestionType()).render(payload);

        return new PracticeQuestionResponse(
                version.getId(), question.getQuestionType(), resolveConceptTitle(version.getId()),
                rendered, version.getEstimatedTimeSeconds(), selectionReason);
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
