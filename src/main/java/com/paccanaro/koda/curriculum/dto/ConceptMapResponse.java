package com.paccanaro.koda.curriculum.dto;

import java.util.UUID;

/**
 * {@code state} usa exatamente os nomes que o frontend ja consome em
 * {@code SkillNode.tsx}: locked, available, active, completed, mastered,
 * needsReview — nao ha traducao de vocabulario entre backend e frontend.
 */
public record ConceptMapResponse(UUID id, String title, String state, int progress) {
}
