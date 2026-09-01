package com.paccanaro.koda.curriculum.dto;

import java.util.List;

/**
 * Trilhas achatadas na ordem de exibicao (subject.display_order, depois
 * topic.display_order). So existe um subject seedado por enquanto; a
 * hierarquia real fica em {@code CurriculumService}, o cliente so consome
 * a lista pronta.
 */
public record CurriculumMapResponse(List<TopicMapResponse> topics) {
}
