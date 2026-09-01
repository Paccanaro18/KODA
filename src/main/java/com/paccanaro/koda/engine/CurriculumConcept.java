package com.paccanaro.koda.engine;

import java.util.UUID;

/**
 * Vocabulario proprio do engine para um concept — nao {@code curriculum.Concept}
 * (entidade JPA, construtor {@code protected}, so o Hibernate instancia). O
 * engine nao pode depender de um tipo que so existe construido por
 * persistencia: quebraria "puro, sem I/O" no nivel do tipo, nao so por
 * convencao, e tornaria os testes impossiveis sem um banco de verdade.
 * {@code QuestionService} mapeia {@code Concept -> CurriculumConcept} antes de
 * chamar o engine.
 */
public record CurriculumConcept(UUID id, String title) {
}
