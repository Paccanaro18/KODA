package com.paccanaro.koda.question;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionConceptRepository extends JpaRepository<QuestionConcept, UUID> {

    Optional<QuestionConcept> findFirstByQuestionVersionId(UUID questionVersionId);

    List<QuestionConcept> findAllByConceptId(UUID conceptId);
}
