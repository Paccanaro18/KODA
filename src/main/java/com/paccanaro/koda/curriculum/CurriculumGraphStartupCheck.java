package com.paccanaro.koda.curriculum;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Recusa a aplicacao subir se o curriculo seedado tiver ciclo de pre-requisito.
 * Roda no boot, em todo ambiente — inclusive no CI, onde `mvn verify` sobe o
 * contexto para os testes de integracao e falharia junto se o grafo estivesse
 * quebrado.
 */
@Component
public class CurriculumGraphStartupCheck implements ApplicationRunner {

    private final ConceptPrerequisiteRepository prerequisiteRepository;
    private final CurriculumGraphValidator validator;

    public CurriculumGraphStartupCheck(ConceptPrerequisiteRepository prerequisiteRepository,
                                       CurriculumGraphValidator validator) {
        this.prerequisiteRepository = prerequisiteRepository;
        this.validator = validator;
    }

    @Override
    public void run(ApplicationArguments args) {
        validator.validate(prerequisiteRepository.findAll());
    }
}
