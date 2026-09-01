package com.paccanaro.koda.engine;

import com.paccanaro.koda.curriculum.ProgressState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O coracao do KODA (docs/architecture/01-arquitetura.md): decide o que o
 * aluno pratica a seguir e avalia o progresso apos cada tentativa.
 * <b>Deterministico, puro, sem I/O</b> — nunca consulta banco, relogio sequer
 * (recebe tudo pronto), nem chama IA. E o que o torna exaustivamente testavel
 * (requisito explicito da Fase 4) e o que garante que a decisao pedagogica
 * nunca depende de um LLM (ARC-01, fronteira 1 da arquitetura).
 *
 * <p>Quem monta {@link LearnerProfile} a partir de {@code question_attempts} e
 * resolve {@link QuestionSpecification} numa questao real do banco e o
 * {@code QuestionService} — o unico lugar com I/O nesse fluxo.
 */
@Component
public class AdaptiveEngine {

    static final int MIN_DIFFICULTY = 1;
    static final int MAX_DIFFICULTY = 5;

    /** Abaixo disso nao ha amostra suficiente para subir/descer o nivel — evita oscilar com 1 ou 2 tentativas. */
    private static final int MIN_SAMPLES_FOR_ADJUSTMENT = 3;
    private static final double RAISE_ACCURACY = 0.8;
    private static final double LOWER_ACCURACY = 0.5;

    private static final double COMPLETED_ACCURACY = 0.7;
    private static final int COMPLETED_MIN_ATTEMPTS = 3;
    private static final int COMPLETED_MIN_DIFFICULTY = 2;

    private static final double MASTERED_ACCURACY = 0.85;
    private static final int MASTERED_MIN_ATTEMPTS = 5;
    private static final int MASTERED_MIN_DIFFICULTY = 4;

    /** Regressao (COMPLETED/MASTERED -> NEEDS_REVIEW) dispara mais facil que a progressao original — perder o que já foi conquistado pesa mais que ainda não ter chegado lá. */
    private static final int REGRESSION_CONSECUTIVE_ERRORS = 2;
    private static final double REGRESSION_ACCURACY = 0.5;

    /**
     * Monta ate {@code sessionSize} especificacoes, em ordem de prioridade:
     * revisao (evidencia de esquecimento) antes de foco atual, antes de
     * conceito novo — perder o que já foi aprendido custa mais caro que
     * atrasar o próximo. {@code curriculumOrder} decide o desempate dentro de
     * cada prioridade, sempre a mesma ordem — e o que torna isto testavel.
     */
    public List<QuestionSpecification> buildSession(LearnerProfile profile, List<CurriculumConcept> curriculumOrder,
                                                     Map<UUID, List<UUID>> prerequisitesByConcept, int sessionSize) {
        List<QuestionSpecification> specs = new ArrayList<>();

        for (CurriculumConcept concept : curriculumOrder) {
            if (specs.size() >= sessionSize) {
                return specs;
            }
            if (profile.progressStateByConcept().get(concept.id()) == ProgressState.NEEDS_REVIEW) {
                ConceptKnowledge knowledge = profile.knowledgeByConcept().get(concept.id());
                specs.add(new QuestionSpecification(concept.id(), reviewDifficulty(knowledge),
                        "Reforço: revisão de \"" + concept.title() + "\""));
            }
        }

        if (specs.size() < sessionSize) {
            findCurrentFocus(profile, curriculumOrder).ifPresent(specs::add);
        }

        if (specs.size() < sessionSize) {
            findNewConcept(profile, curriculumOrder, prerequisitesByConcept).ifPresent(specs::add);
        }

        return specs;
    }

    /**
     * Reavalia o estado de um concept apos uma tentativa. {@code previousState}
     * e {@code null} na primeira tentativa (nenhuma linha em
     * {@code user_concept_progress} ainda).
     */
    public ProgressAssessment assessProgress(ProgressState previousState, ConceptKnowledge knowledge) {
        boolean wasSolid = previousState == ProgressState.COMPLETED || previousState == ProgressState.MASTERED;
        boolean regressing = wasSolid
                && (knowledge.consecutiveErrors() >= REGRESSION_CONSECUTIVE_ERRORS || knowledge.recentAccuracy() < REGRESSION_ACCURACY);

        ProgressState state;
        if (regressing) {
            state = ProgressState.NEEDS_REVIEW;
        } else if (meetsThreshold(knowledge, MASTERED_ACCURACY, MASTERED_MIN_ATTEMPTS, MASTERED_MIN_DIFFICULTY)) {
            state = ProgressState.MASTERED;
        } else if (meetsThreshold(knowledge, COMPLETED_ACCURACY, COMPLETED_MIN_ATTEMPTS, COMPLETED_MIN_DIFFICULTY)) {
            state = ProgressState.COMPLETED;
        } else {
            state = ProgressState.ACTIVE;
        }

        int percent = switch (state) {
            case COMPLETED, MASTERED -> 100;
            case ACTIVE, NEEDS_REVIEW ->
                    Math.max(10, Math.min(90, knowledge.highestDifficultyCorrect() * 100 / MAX_DIFFICULTY));
        };

        return new ProgressAssessment(state, percent);
    }

    private Optional<QuestionSpecification> findCurrentFocus(LearnerProfile profile, List<CurriculumConcept> curriculumOrder) {
        for (CurriculumConcept concept : curriculumOrder) {
            if (profile.progressStateByConcept().get(concept.id()) == ProgressState.ACTIVE) {
                ConceptKnowledge knowledge = profile.knowledgeByConcept().get(concept.id());
                int target = nextDifficulty(knowledge);
                return Optional.of(new QuestionSpecification(concept.id(), target,
                        focusReason(knowledge, target, concept.title())));
            }
        }
        return Optional.empty();
    }

    private Optional<QuestionSpecification> findNewConcept(LearnerProfile profile, List<CurriculumConcept> curriculumOrder,
                                                            Map<UUID, List<UUID>> prerequisitesByConcept) {
        for (CurriculumConcept concept : curriculumOrder) {
            if (profile.progressStateByConcept().containsKey(concept.id())) {
                continue;
            }
            List<UUID> prerequisites = prerequisitesByConcept.getOrDefault(concept.id(), List.of());
            boolean available = prerequisites.stream()
                    .allMatch(prerequisiteId -> isSolid(profile.progressStateByConcept().get(prerequisiteId)));
            if (available) {
                return Optional.of(new QuestionSpecification(concept.id(), MIN_DIFFICULTY,
                        "Novo conceito: \"" + concept.title() + "\""));
            }
        }
        return Optional.empty();
    }

    private static boolean meetsThreshold(ConceptKnowledge knowledge, double accuracy, int minAttempts, int minDifficulty) {
        return knowledge.recentAccuracy() >= accuracy
                && knowledge.attemptsTotal() >= minAttempts
                && knowledge.highestDifficultyCorrect() >= minDifficulty;
    }

    private static int nextDifficulty(ConceptKnowledge knowledge) {
        if (knowledge == null) {
            return MIN_DIFFICULTY;
        }
        int base = Math.max(MIN_DIFFICULTY, knowledge.lastDifficultyAttempted());
        if (knowledge.attemptsTotal() < MIN_SAMPLES_FOR_ADJUSTMENT) {
            return base;
        }
        if (knowledge.recentAccuracy() >= RAISE_ACCURACY) {
            return Math.min(MAX_DIFFICULTY, base + 1);
        }
        if (knowledge.recentAccuracy() < LOWER_ACCURACY) {
            return Math.max(MIN_DIFFICULTY, base - 1);
        }
        return base;
    }

    /** Revisao mira um degrau abaixo do teto ja dominado — reforca sem repetir exatamente o pico. */
    private static int reviewDifficulty(ConceptKnowledge knowledge) {
        if (knowledge == null) {
            return MIN_DIFFICULTY;
        }
        return Math.max(MIN_DIFFICULTY, knowledge.highestDifficultyCorrect() - 1);
    }

    private static String focusReason(ConceptKnowledge knowledge, int target, String title) {
        int base = knowledge == null ? MIN_DIFFICULTY : Math.max(MIN_DIFFICULTY, knowledge.lastDifficultyAttempted());
        if (target > base) {
            return "Subindo o nível em \"" + title + "\"";
        }
        if (target < base) {
            return "Ajustando o nível em \"" + title + "\"";
        }
        return "Praticando \"" + title + "\"";
    }

    private static boolean isSolid(ProgressState state) {
        return state == ProgressState.COMPLETED || state == ProgressState.MASTERED;
    }
}
