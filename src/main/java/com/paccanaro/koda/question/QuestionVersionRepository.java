package com.paccanaro.koda.question;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuestionVersionRepository extends JpaRepository<QuestionVersion, UUID> {

    /**
     * Nesta fase toda questao tem uma unica versao — sem fluxo de revisao que
     * crie uma segunda. "Versao atual" e simplesmente a mais alta, calculada
     * na consulta em vez de um ponteiro {@code current_version_id} na questao:
     * evita a dependencia circular questions<->question_versions no seed.
     */
    Optional<QuestionVersion> findFirstByQuestionIdOrderByVersionDesc(UUID questionId);
}
