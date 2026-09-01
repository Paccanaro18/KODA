package com.paccanaro.koda.question;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    Optional<Question> findByIdAndStatus(UUID id, QuestionStatus status);

    List<Question> findAllByStatusAndQuestionTypeIn(QuestionStatus status, Collection<String> questionTypes);
}
