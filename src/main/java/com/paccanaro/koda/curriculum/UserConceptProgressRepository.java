package com.paccanaro.koda.curriculum;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserConceptProgressRepository extends JpaRepository<UserConceptProgress, UUID> {

    List<UserConceptProgress> findAllByUserId(UUID userId);
}
