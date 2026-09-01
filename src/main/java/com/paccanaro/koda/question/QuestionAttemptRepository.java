package com.paccanaro.koda.question;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, UUID> {
}
