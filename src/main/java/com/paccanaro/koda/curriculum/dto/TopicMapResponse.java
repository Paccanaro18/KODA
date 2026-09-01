package com.paccanaro.koda.curriculum.dto;

import java.util.List;
import java.util.UUID;

public record TopicMapResponse(UUID id, String name, String description, List<ConceptMapResponse> concepts) {
}
