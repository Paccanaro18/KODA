package com.paccanaro.koda.ai;

import com.paccanaro.koda.AbstractIntegrationTest;
import com.paccanaro.koda.curriculum.Concept;
import com.paccanaro.koda.curriculum.ConceptRepository;
import com.paccanaro.koda.question.Question;
import com.paccanaro.koda.question.QuestionRepository;
import com.paccanaro.koda.question.QuestionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fluxo ponta a ponta da geracao, com um gateway falso — nenhuma chamada real
 * de LLM, nenhum custo, e o teste roda offline e deterministico.
 */
@DisplayName("Geracao de questoes por IA")
@Import(AiGenerationIntegrationTest.FakeGatewayConfig.class)
class AiGenerationIntegrationTest extends AbstractIntegrationTest {

    /** Gateway controlavel: o teste decide o que o "modelo" devolve, ou faz ele falhar. */
    static class FakeAiGatewayClient implements AiGatewayClient {

        GeneratedQuestion nextResponse;
        boolean shouldFail;

        @Override
        public GatewayResponse generate(GenerationSpecification specification) {
            if (shouldFail) {
                throw new IllegalStateException("provedor indisponivel (simulado)");
            }
            return new GatewayResponse(nextResponse, "fake-model", 1200, 400,
                    new BigDecimal("0.016000"), 950L);
        }

        @Override
        public String modelId() {
            return "fake-model";
        }
    }

    @TestConfiguration
    static class FakeGatewayConfig {
        @Bean
        @Primary
        FakeAiGatewayClient fakeAiGatewayClient() {
            return new FakeAiGatewayClient();
        }
    }

    @Autowired
    private FakeAiGatewayClient gateway;

    @Autowired
    private GenerationRequestRepository requestRepository;

    @Autowired
    private AiGenerationRepository generationRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private GenerationWorker worker;

    private UUID condicionaisId;

    @BeforeEach
    void setUpConcept() {
        gateway.shouldFail = false;
        condicionaisId = conceptRepository.findAll().stream()
                .filter(concept -> "condicionais".equals(concept.getSlug()))
                .map(Concept::getId)
                .findFirst()
                .orElseThrow();
    }

    private GeneratedQuestion goodCandidate() {
        return new GeneratedQuestion(
                "{\"prompt\":\"Numa estrutura de condicionais, o que acontece quando a condicao e falsa? " + UUID.randomUUID() + "\",\"options\":[{\"id\":\"a\",\"label\":\"o bloco executa\"},{\"id\":\"b\",\"label\":\"o bloco e pulado\"}]}",
                "{\"optionId\":\"b\"}",
                "Quando a condicao e falsa o bloco do if nao roda e a execucao segue depois dele.",
                "{\"a\":\"O bloco so roda quando a condicao e verdadeira.\"}",
                2, 30, 0.92);
    }

    @Test
    @DisplayName("questao aprovada e persistida em revisao, nunca publicada direto (AI-01)")
    void acceptedQuestionLandsInReviewNotPublished() {
        gateway.nextResponse = goodCandidate();
        requestRepository.save(GenerationRequest.enqueue(condicionaisId, "single_choice", 2));

        assertThat(worker.drainOne()).isTrue();

        // Toda questao de origem IA (slug "-ia-") precisa estar em revisao. Afirmar
        // sobre o conjunto inteiro, e nao sobre uma contagem, mantem o teste valido
        // independente do que os outros testes da classe ja deixaram no banco.
        List<Question> generated = questionRepository.findAll().stream()
                .filter(question -> question.getSlug().contains("-ia-"))
                .toList();

        assertThat(generated).isNotEmpty();
        assertThat(generated).allMatch(question -> question.getStatus() == QuestionStatus.IN_REVIEW);
    }

    @Test
    @DisplayName("toda geracao grava rastreabilidade: modelo, versao do prompt, tokens e custo")
    void everyGenerationIsTraceable() {
        gateway.nextResponse = goodCandidate();
        requestRepository.save(GenerationRequest.enqueue(condicionaisId, "single_choice", 2));

        worker.drainOne();

        List<AiGeneration> generations = generationRepository.findAllByOrderByCreatedAtDesc();
        assertThat(generations).isNotEmpty();

        AiGeneration latest = generations.get(0);
        assertThat(latest.getOutcome()).isEqualTo(GenerationOutcome.ACCEPTED);
        assertThat(latest.getCostUsd()).isEqualByComparingTo("0.016000");
        assertThat(latest.getQuestionId()).isNotNull();
    }

    @Test
    @DisplayName("candidato rejeitado nao vira questao, mas o motivo fica registrado")
    void rejectedCandidateIsRecordedButNotPersistedAsQuestion() {
        long questionsBefore = questionRepository.count();

        // Fora do conceito pedido: reprova no estagio 4.
        gateway.nextResponse = new GeneratedQuestion(
                "{\"prompt\":\"Qual a capital da Franca?\",\"options\":[{\"id\":\"a\",\"label\":\"Paris\"},{\"id\":\"b\",\"label\":\"Roma\"}]}",
                "{\"optionId\":\"a\"}", "Paris e a capital da Franca ha bastante tempo.", null, 2, 30, 0.95);
        requestRepository.save(GenerationRequest.enqueue(condicionaisId, "single_choice", 2));

        worker.drainOne();

        assertThat(questionRepository.count()).isEqualTo(questionsBefore);

        AiGeneration latest = generationRepository.findAllByOrderByCreatedAtDesc().get(0);
        assertThat(latest.getOutcome()).isEqualTo(GenerationOutcome.REJECTED_CURRICULUM);
        assertThat(latest.getQuestionId()).isNull();
    }

    @Test
    @DisplayName("provedor fora do ar nao afeta o aluno: a sessao de pratica continua servindo")
    void providerOutageDoesNotAffectStudents() throws Exception {
        gateway.shouldFail = true;
        requestRepository.save(GenerationRequest.enqueue(condicionaisId, "single_choice", 2));

        worker.drainOne();

        AiGeneration latest = generationRepository.findAllByOrderByCreatedAtDesc().get(0);
        assertThat(latest.getOutcome()).isEqualTo(GenerationOutcome.REJECTED_PROVIDER_ERROR);

        // O que importa: o aluno segue recebendo questoes do banco existente.
        String token = registerAndGetAccessToken();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/questions/practice-session")
                        .queryParam("types", "single_choice")
                        .queryParam("limit", "5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$[0].questionVersionId").exists());
    }

    @Test
    @DisplayName("questao gerada e em revisao nao aparece na sessao de pratica do aluno")
    void generatedQuestionIsNotServedBeforeReview() throws Exception {
        gateway.nextResponse = goodCandidate();
        requestRepository.save(GenerationRequest.enqueue(condicionaisId, "single_choice", 2));
        worker.drainOne();

        UUID generatedId = questionRepository.findAll().stream()
                .filter(question -> question.getStatus() == QuestionStatus.IN_REVIEW)
                .map(Question::getId)
                .findFirst()
                .orElseThrow();

        String token = registerAndGetAccessToken();
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/questions/practice-session")
                        .queryParam("types", "single_choice")
                        .queryParam("limit", "25")
                        .header("Authorization", "Bearer " + token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(generatedId.toString());
    }

    private String registerAndGetAccessToken() throws Exception {
        String email = "geracao-" + UUID.randomUUID() + "@koda.dev";
        String payload = """
                {"email":"%s","password":"uma-senha-bem-longa-123","displayName":"Gerador"}
                """.formatted(email);

        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/register")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return new tools.jackson.databind.ObjectMapper().readTree(response).path("accessToken").asString();
    }
}
