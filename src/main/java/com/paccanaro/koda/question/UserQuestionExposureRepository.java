package com.paccanaro.koda.question;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserQuestionExposureRepository extends JpaRepository<UserQuestionExposure, UUID> {

    List<UserQuestionExposure> findAllByUserId(UUID userId);

    Optional<UserQuestionExposure> findByUserIdAndQuestionId(UUID userId, UUID questionId);
}
