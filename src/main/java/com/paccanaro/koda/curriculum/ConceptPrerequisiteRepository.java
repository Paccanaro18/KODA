package com.paccanaro.koda.curriculum;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConceptPrerequisiteRepository extends JpaRepository<ConceptPrerequisite, UUID> {
}
