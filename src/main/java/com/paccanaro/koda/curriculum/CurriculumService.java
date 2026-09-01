package com.paccanaro.koda.curriculum;

import com.paccanaro.koda.curriculum.dto.ConceptMapResponse;
import com.paccanaro.koda.curriculum.dto.CurriculumMapResponse;
import com.paccanaro.koda.curriculum.dto.TopicMapResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Service
public class CurriculumService {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final ConceptRepository conceptRepository;
    private final ConceptPrerequisiteRepository prerequisiteRepository;
    private final UserConceptProgressRepository progressRepository;

    public CurriculumService(SubjectRepository subjectRepository,
                             TopicRepository topicRepository,
                             ConceptRepository conceptRepository,
                             ConceptPrerequisiteRepository prerequisiteRepository,
                             UserConceptProgressRepository progressRepository) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.conceptRepository = conceptRepository;
        this.prerequisiteRepository = prerequisiteRepository;
        this.progressRepository = progressRepository;
    }

    /**
     * Monta o mapa de aprendizado do usuario. "locked" e "available" nunca sao
     * lidos do banco: sao sempre calculados aqui, a partir da ausencia de
     * progresso e do grafo de pre-requisitos.
     */
    public CurriculumMapResponse buildMap(UUID userId) {
        List<Subject> subjects = sortedBy(subjectRepository.findAll(), Subject::getDisplayOrder);
        List<Topic> topics = sortedBy(topicRepository.findAll(), Topic::getDisplayOrder);
        List<Concept> concepts = sortedBy(conceptRepository.findAll(), Concept::getDisplayOrder);

        Map<UUID, List<Topic>> topicsBySubject = groupBy(topics, Topic::getSubjectId);
        Map<UUID, List<Concept>> conceptsByTopic = groupBy(concepts, Concept::getTopicId);

        Map<UUID, List<UUID>> prerequisitesByConcept = prerequisiteRepository.findAll().stream()
                .collect(Collectors.groupingBy(ConceptPrerequisite::getConceptId,
                        Collectors.mapping(ConceptPrerequisite::getPrerequisiteConceptId, Collectors.toList())));

        Map<UUID, UserConceptProgress> progressByConcept = progressRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(UserConceptProgress::getConceptId, Function.identity()));

        List<TopicMapResponse> topicResponses = subjects.stream()
                .flatMap(subject -> topicsBySubject.getOrDefault(subject.getId(), List.of()).stream())
                .map(topic -> toTopicResponse(
                        topic,
                        conceptsByTopic.getOrDefault(topic.getId(), List.of()),
                        prerequisitesByConcept,
                        progressByConcept))
                .toList();

        return new CurriculumMapResponse(topicResponses);
    }

    private TopicMapResponse toTopicResponse(Topic topic, List<Concept> concepts,
                                             Map<UUID, List<UUID>> prerequisitesByConcept,
                                             Map<UUID, UserConceptProgress> progressByConcept) {
        List<ConceptMapResponse> conceptResponses = concepts.stream()
                .map(concept -> toConceptResponse(concept, prerequisitesByConcept, progressByConcept))
                .toList();
        return new TopicMapResponse(topic.getId(), topic.getName(), topic.getDescription(), conceptResponses);
    }

    private ConceptMapResponse toConceptResponse(Concept concept,
                                                 Map<UUID, List<UUID>> prerequisitesByConcept,
                                                 Map<UUID, UserConceptProgress> progressByConcept) {
        UserConceptProgress progress = progressByConcept.get(concept.getId());

        if (progress != null) {
            return new ConceptMapResponse(
                    concept.getId(), concept.getTitle(), toResponseState(progress.getState()), progress.getProgressPercent());
        }

        List<UUID> prerequisites = prerequisitesByConcept.getOrDefault(concept.getId(), List.of());
        boolean unlocked = prerequisites.stream().allMatch(prerequisiteId -> isMastered(progressByConcept.get(prerequisiteId)));
        String state = unlocked ? "available" : "locked";

        return new ConceptMapResponse(concept.getId(), concept.getTitle(), state, 0);
    }

    private static boolean isMastered(UserConceptProgress progress) {
        return progress != null
                && (progress.getState() == ProgressState.COMPLETED || progress.getState() == ProgressState.MASTERED);
    }

    private static String toResponseState(ProgressState state) {
        return switch (state) {
            case ACTIVE -> "active";
            case COMPLETED -> "completed";
            case MASTERED -> "mastered";
            case NEEDS_REVIEW -> "needsReview";
        };
    }

    private static <T> List<T> sortedBy(List<T> items, ToIntFunction<T> key) {
        return items.stream().sorted(Comparator.comparingInt(key)).toList();
    }

    private static <T> Map<UUID, List<T>> groupBy(List<T> items, Function<T, UUID> key) {
        return items.stream().collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
    }
}
