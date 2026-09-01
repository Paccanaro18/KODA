package com.paccanaro.koda.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GenerationRequestRepository extends JpaRepository<GenerationRequest, UUID> {

    List<GenerationRequest> findAllByStatusOrderByCreatedAtAsc(GenerationRequestStatus status);
}
