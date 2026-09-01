package com.paccanaro.koda.question;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.paccanaro.koda.AbstractIntegrationTest;
import com.paccanaro.koda.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Fluxo de pratica")
class PracticeFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserQuestionExposureRepository exposureRepository;

    @Test
    @DisplayName("sessao de pratica nunca revela o gabarito antes da submissao")
    void practiceSessionNeverLeaksCorrectAnswer() throws Exception {
        String accessToken = registerAndGetAccessToken();

        MvcResult result = mockMvc.perform(get("/api/v1/questions/practice-session")
                        .queryParam("types", "single_choice", "true_false")
                        .queryParam("limit", "10")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("correctAnswer");

        JsonNode items = objectMapper.readTree(body);
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("corrige certo e errado no servidor, e so entao revela o gabarito")
    void scoresCorrectlyAndOnlyThenRevealsAnswer() throws Exception {
        String email = uniqueEmail();
        String accessToken = registerAndGetAccessToken(email);

        UUID questionVersionId = findTiposPrimitivos1VersionId(accessToken);

        // Resposta certa: {"optionId":"b"} — seed de "tipos-primitivos-1".
        mockMvc.perform(post("/api/v1/questions/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(
                                new AttemptPayload(questionVersionId, objectMapper.readTree("{\"optionId\":\"b\"}"), 5000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.correctAnswer.optionId").value("b"))
                .andExpect(jsonPath("$.concept").value("Tipos primitivos"))
                .andExpect(jsonPath("$.explanation").isNotEmpty());

        // Resposta errada: mesma questao, opcao diferente.
        mockMvc.perform(post("/api/v1/questions/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(objectMapper.writeValueAsString(
                                new AttemptPayload(questionVersionId, objectMapper.readTree("{\"optionId\":\"a\"}"), 3000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false));

        UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
        assertThat(exposureRepository.findAllByUserId(userId)).hasSize(1);
        assertThat(exposureRepository.findAllByUserId(userId).get(0).getTimesSeen()).isEqualTo(2);
    }

    @Test
    @DisplayName("rejeita tentativa sem sessao")
    void rejectsAttemptWithoutSession() throws Exception {
        mockMvc.perform(post("/api/v1/questions/attempts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AttemptPayload(UUID.randomUUID(), objectMapper.readTree("{\"optionId\":\"a\"}"), 1000))))
                .andExpect(status().isUnauthorized());
    }

    private UUID findTiposPrimitivos1VersionId(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/questions/practice-session")
                        .queryParam("types", "single_choice")
                        .queryParam("limit", "30")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode item : items) {
            if ("Qual e o tipo do valor 3.14?".equals(item.path("payload").path("prompt").asString(null))) {
                return UUID.fromString(item.path("questionVersionId").asString());
            }
        }
        throw new IllegalStateException("questao 'tipos-primitivos-1' nao apareceu na sessao de pratica");
    }

    private String registerAndGetAccessToken() throws Exception {
        return registerAndGetAccessToken(uniqueEmail());
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterPayload(email, "uma-senha-bem-longa-123", "Praticante"));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asString();
    }

    private static String uniqueEmail() {
        return "pratica-" + UUID.randomUUID() + "@koda.dev";
    }

    private record RegisterPayload(String email, String password, String displayName) {
    }

    private record AttemptPayload(UUID questionVersionId, JsonNode submittedAnswer, int responseTimeMs) {
    }
}
