package com.paccanaro.koda.engine;

import com.paccanaro.koda.curriculum.ProgressState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdaptiveEngine")
class AdaptiveEngineTest {

    private final AdaptiveEngine engine = new AdaptiveEngine();

    private static final UUID TIPOS = UUID.randomUUID();
    private static final UUID CONDICIONAIS = UUID.randomUUID();
    private static final UUID LACOS = UUID.randomUUID();

    private static final List<CurriculumConcept> CURRICULUM = List.of(
            new CurriculumConcept(TIPOS, "Tipos primitivos"),
            new CurriculumConcept(CONDICIONAIS, "Condicionais"),
            new CurriculumConcept(LACOS, "Laços"));

    /** condicionais depende de tipos; lacos depende de condicionais — cadeia linear, igual ao seed real. */
    private static final Map<UUID, List<UUID>> LINEAR_PREREQUISITES = Map.of(
            CONDICIONAIS, List.of(TIPOS),
            LACOS, List.of(CONDICIONAIS));

    private static ConceptKnowledge knowledge(UUID conceptId, int total, int correct, double recentAccuracy,
                                              int consecutiveErrors, int lastDifficulty, int highestCorrect) {
        return new ConceptKnowledge(conceptId, total, correct, recentAccuracy, consecutiveErrors,
                lastDifficulty, highestCorrect, Instant.now());
    }

    @Nested
    @DisplayName("buildSession")
    class BuildSession {

        @Test
        @DisplayName("aluno sem nenhum progresso recebe o primeiro concept do curriculo, na dificuldade minima")
        void introducesFirstConceptForBlankProfile() {
            LearnerProfile profile = new LearnerProfile(UUID.randomUUID(), Map.of(), Map.of());

            List<QuestionSpecification> session = engine.buildSession(profile, CURRICULUM, Map.of(), 5);

            assertThat(session).hasSize(1);
            assertThat(session.get(0).conceptId()).isEqualTo(TIPOS);
            assertThat(session.get(0).targetDifficulty()).isEqualTo(1);
            assertThat(session.get(0).selectionReason()).contains("Novo conceito");
        }

        @Test
        @DisplayName("concept novo so aparece quando todos os pre-requisitos estao completos ou dominados")
        void gatesNewConceptByPrerequisites() {
            LearnerProfile profile = new LearnerProfile(UUID.randomUUID(),
                    Map.of(TIPOS, knowledge(TIPOS, 5, 4, 0.8, 0, 2, 2)),
                    Map.of(TIPOS, ProgressState.ACTIVE));

            List<QuestionSpecification> session = engine.buildSession(profile, CURRICULUM, LINEAR_PREREQUISITES, 5);

            // TIPOS esta ACTIVE (vira foco atual); CONDICIONAIS continua bloqueado por TIPOS nao estar completo/dominado.
            assertThat(session).extracting(QuestionSpecification::conceptId).containsExactly(TIPOS);
        }

        @Test
        @DisplayName("libera o proximo concept assim que o pre-requisito e concluido")
        void unlocksNextConceptOncePrerequisiteCompleted() {
            LearnerProfile profile = new LearnerProfile(UUID.randomUUID(), Map.of(),
                    Map.of(TIPOS, ProgressState.COMPLETED));

            List<QuestionSpecification> session = engine.buildSession(profile, CURRICULUM, LINEAR_PREREQUISITES, 5);

            assertThat(session).hasSize(1);
            assertThat(session.get(0).conceptId()).isEqualTo(CONDICIONAIS);
        }

        @Test
        @DisplayName("revisao vem antes de foco atual e de concept novo")
        void reviewTakesPriorityOverFocusAndNewConcept() {
            LearnerProfile profile = new LearnerProfile(UUID.randomUUID(),
                    Map.of(
                            TIPOS, knowledge(TIPOS, 10, 3, 0.3, 2, 3, 3),
                            CONDICIONAIS, knowledge(CONDICIONAIS, 4, 3, 0.75, 0, 2, 2)),
                    Map.of(
                            TIPOS, ProgressState.NEEDS_REVIEW,
                            CONDICIONAIS, ProgressState.ACTIVE));

            List<QuestionSpecification> session = engine.buildSession(profile, CURRICULUM, LINEAR_PREREQUISITES, 5);

            assertThat(session).extracting(QuestionSpecification::conceptId).containsExactly(TIPOS, CONDICIONAIS);
            assertThat(session.get(0).selectionReason()).contains("Reforço");
        }

        @Test
        @DisplayName("respeita o tamanho da sessao mesmo com mais candidatos disponiveis")
        void respectsSessionSize() {
            LearnerProfile profile = new LearnerProfile(UUID.randomUUID(),
                    Map.of(
                            TIPOS, knowledge(TIPOS, 6, 2, 0.3, 2, 2, 1),
                            CONDICIONAIS, knowledge(CONDICIONAIS, 6, 2, 0.3, 2, 2, 1),
                            LACOS, knowledge(LACOS, 6, 2, 0.3, 2, 2, 1)),
                    Map.of(
                            TIPOS, ProgressState.NEEDS_REVIEW,
                            CONDICIONAIS, ProgressState.NEEDS_REVIEW,
                            LACOS, ProgressState.NEEDS_REVIEW));

            List<QuestionSpecification> session = engine.buildSession(profile, CURRICULUM, LINEAR_PREREQUISITES, 2);

            assertThat(session).hasSize(2);
        }

        @Test
        @DisplayName("sobe a dificuldade quando a precisao recente e alta e ha amostra suficiente")
        void raisesDifficultyOnHighAccuracy() {
            LearnerProfile profile = new LearnerProfile(UUID.randomUUID(),
                    Map.of(TIPOS, knowledge(TIPOS, 5, 4, 0.9, 0, 2, 2)),
                    Map.of(TIPOS, ProgressState.ACTIVE));

            List<QuestionSpecification> session = engine.buildSession(profile, CURRICULUM, Map.of(), 5);

            assertThat(session.get(0).targetDifficulty()).isEqualTo(3);
            assertThat(session.get(0).selectionReason()).contains("Subindo");
        }

        @Test
        @DisplayName("desce a dificuldade quando a precisao recente e baixa")
        void lowersDifficultyOnLowAccuracy() {
            LearnerProfile profile = new LearnerProfile(UUID.randomUUID(),
                    Map.of(TIPOS, knowledge(TIPOS, 5, 1, 0.2, 2, 3, 1)),
                    Map.of(TIPOS, ProgressState.ACTIVE));

            List<QuestionSpecification> session = engine.buildSession(profile, CURRICULUM, Map.of(), 5);

            assertThat(session.get(0).targetDifficulty()).isEqualTo(2);
            assertThat(session.get(0).selectionReason()).contains("Ajustando");
        }

        @Test
        @DisplayName("nao ajusta dificuldade com menos de tres tentativas, mesmo com precisao extrema")
        void doesNotAdjustBelowMinimumSampleSize() {
            LearnerProfile profile = new LearnerProfile(UUID.randomUUID(),
                    Map.of(TIPOS, knowledge(TIPOS, 2, 2, 1.0, 0, 2, 2)),
                    Map.of(TIPOS, ProgressState.ACTIVE));

            List<QuestionSpecification> session = engine.buildSession(profile, CURRICULUM, Map.of(), 5);

            assertThat(session.get(0).targetDifficulty()).isEqualTo(2);
            assertThat(session.get(0).selectionReason()).contains("Praticando");
        }
    }

    @Nested
    @DisplayName("assessProgress")
    class AssessProgress {

        @Test
        @DisplayName("primeira tentativa, poucos dados: fica ACTIVE")
        void firstAttemptStaysActive() {
            ProgressAssessment assessment = engine.assessProgress(null, knowledge(TIPOS, 1, 1, 1.0, 0, 1, 1));

            assertThat(assessment.state()).isEqualTo(ProgressState.ACTIVE);
        }

        @Test
        @DisplayName("evidencia forte em dificuldade alta promove a MASTERED")
        void promotesToMastered() {
            ProgressAssessment assessment = engine.assessProgress(ProgressState.ACTIVE,
                    knowledge(TIPOS, 8, 7, 0.9, 0, 4, 4));

            assertThat(assessment.state()).isEqualTo(ProgressState.MASTERED);
            assertThat(assessment.progressPercent()).isEqualTo(100);
        }

        @Test
        @DisplayName("muitos acertos faceis, sem evidencia em dificuldade alta, NAO promove a MASTERED")
        void doesNotMasterWithoutHighDifficultyEvidence() {
            ProgressAssessment assessment = engine.assessProgress(ProgressState.ACTIVE,
                    knowledge(TIPOS, 20, 19, 0.95, 0, 1, 1));

            assertThat(assessment.state()).isNotEqualTo(ProgressState.MASTERED);
        }

        @Test
        @DisplayName("dois erros seguidos depois de MASTERED derruba para NEEDS_REVIEW")
        void regressesFromMasteredOnConsecutiveErrors() {
            ProgressAssessment assessment = engine.assessProgress(ProgressState.MASTERED,
                    knowledge(TIPOS, 10, 6, 0.6, 2, 3, 4));

            assertThat(assessment.state()).isEqualTo(ProgressState.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("um unico erro isolado depois de MASTERED nao derruba")
        void singleErrorDoesNotRegressFromMastered() {
            ProgressAssessment assessment = engine.assessProgress(ProgressState.MASTERED,
                    knowledge(TIPOS, 10, 8, 0.9, 1, 4, 4));

            assertThat(assessment.state()).isEqualTo(ProgressState.MASTERED);
        }

        @Test
        @DisplayName("percentual fica entre 10 e 90 enquanto ACTIVE")
        void percentIsBoundedWhileActive() {
            ProgressAssessment neverCorrect = engine.assessProgress(null, knowledge(TIPOS, 3, 0, 0.0, 3, 1, 0));
            ProgressAssessment nearPeak = engine.assessProgress(ProgressState.ACTIVE,
                    knowledge(TIPOS, 3, 2, 0.6, 0, 5, 5));

            assertThat(neverCorrect.progressPercent()).isEqualTo(10);
            assertThat(nearPeak.progressPercent()).isEqualTo(90);
        }
    }
}
