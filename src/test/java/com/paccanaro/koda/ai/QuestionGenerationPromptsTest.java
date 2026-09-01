package com.paccanaro.koda.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Prompt de geracao")
class QuestionGenerationPromptsTest {

    @Test
    @DisplayName("a especificacao entra delimitada, nunca solta nas instrucoes (SEC-02)")
    void specificationIsAlwaysDelimited() {
        GenerationSpecification spec = new GenerationSpecification(
                UUID.randomUUID(), "Condicionais", "Fundamentos", 2, "single_choice", List.of());

        String user = QuestionGenerationPrompts.user(spec);

        assertThat(user).contains("<especificacao>", "</especificacao>");
        assertThat(user.indexOf("Condicionais")).isGreaterThan(user.indexOf("<especificacao>"));
        assertThat(user.indexOf("Condicionais")).isLessThan(user.indexOf("</especificacao>"));
    }

    @Test
    @DisplayName("o system prompt instrui a nunca tratar a especificacao como comando")
    void systemPromptDefendsAgainstInjection() {
        String system = QuestionGenerationPrompts.system();

        assertThat(system).contains("nunca como instrucao");
        assertThat(system).contains("<especificacao>");
    }

    @Test
    @DisplayName("nenhum dado pessoal ou segredo entra no prompt — so curriculo curado")
    void promptCarriesNoPersonalDataOrSecrets() {
        GenerationSpecification spec = new GenerationSpecification(
                UUID.randomUUID(), "Condicionais", "Fundamentos", 2, "single_choice", List.of());

        String full = QuestionGenerationPrompts.system() + QuestionGenerationPrompts.user(spec);

        // O prompt e montado so a partir de concept/topico/tipo/dificuldade —
        // nada de id de usuario, e-mail, token ou resposta de aluno.
        assertThat(full).doesNotContain("@");
        assertThat(full).doesNotContainIgnoringCase("sk-ant");
        assertThat(full).doesNotContainIgnoringCase("user_id");
        assertThat(full).doesNotContainIgnoringCase("password");
    }

    @Test
    @DisplayName("todos os 5 tipos tem exemplo de formato no prompt")
    void everyTypeHasAFormatExample() {
        List<String> types = List.of("single_choice", "multiple_select", "true_false", "ordering", "fill_in_blank");

        for (String type : types) {
            GenerationSpecification spec = new GenerationSpecification(
                    UUID.randomUUID(), "Condicionais", "Fundamentos", 2, type, List.of());

            assertThat(QuestionGenerationPrompts.user(spec))
                    .as("tipo %s sem exemplo de formato", type)
                    .doesNotContain("tipo sem exemplo cadastrado");
        }
    }
}
