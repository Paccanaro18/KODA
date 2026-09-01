package com.paccanaro.koda.curriculum;

import tools.jackson.databind.ObjectMapper;
import com.paccanaro.koda.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Mapa de aprendizado")
class CurriculumMapIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("usuario novo ve so o primeiro concept disponivel, o resto bloqueado")
    void newUserSeesOnlyFirstConceptAvailable() throws Exception {
        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(get("/api/v1/curriculum/map")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics.length()").value(2))
                .andExpect(jsonPath("$.topics[0].concepts.length()").value(6))
                .andExpect(jsonPath("$.topics[0].concepts[0].title").value("Tipos primitivos"))
                .andExpect(jsonPath("$.topics[0].concepts[0].state").value("available"))
                .andExpect(jsonPath("$.topics[0].concepts[1].state").value("locked"))
                .andExpect(jsonPath("$.topics[0].concepts[5].state").value("locked"))
                .andExpect(jsonPath("$.topics[1].concepts.length()").value(4))
                .andExpect(jsonPath("$.topics[1].concepts[0].state").value("locked"));
    }

    @Test
    @DisplayName("rejeita quem nao tem sessao")
    void rejectsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/curriculum/map"))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndGetAccessToken() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterPayload(uniqueEmail(), "uma-senha-bem-longa-123", "Curriculo"));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("accessToken").asText();
    }

    private static String uniqueEmail() {
        return "curriculo-" + UUID.randomUUID() + "@koda.dev";
    }

    private record RegisterPayload(String email, String password, String displayName) {
    }
}
