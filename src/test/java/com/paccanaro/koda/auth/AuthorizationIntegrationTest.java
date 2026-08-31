package com.paccanaro.koda.auth;

import tools.jackson.databind.ObjectMapper;
import com.paccanaro.koda.AbstractIntegrationTest;
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

/**
 * Autorizacao verificada no servidor (SEC-03).
 *
 * <p>O objetivo destes casos e provar que nenhuma protecao depende do frontend:
 * as requisicoes aqui sao feitas diretamente contra a API, sem passar por
 * nenhuma tela.
 */
@DisplayName("Autorizacao")
class AuthorizationIntegrationTest extends AbstractIntegrationTest {

    private static final String VALID_PASSWORD = "uma-senha-bem-longa-123";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("nega acesso a rota protegida sem token")
    void deniesAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("nega token forjado ou corrompido")
    void deniesForgedToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer nao.e.um.jwt.valido"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * O caso classico de IDOR: dois usuarios reais, e um tenta ver os dados do
     * outro. Como o id vem do token e nunca da requisicao, o servidor devolve
     * sempre o dono do token — nao ha parametro para manipular.
     */
    @Test
    @DisplayName("um usuario nunca recebe os dados de outro")
    void userCannotReadAnotherUsersData() throws Exception {
        String emailA = uniqueEmail();
        String emailB = uniqueEmail();

        String tokenA = accessTokenFor(emailA);
        String tokenB = accessTokenFor(emailB);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(emailA));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(emailB));

        assertThat(tokenA).isNotEqualTo(tokenB);
    }

    @Test
    @DisplayName("estudante nao acessa area administrativa")
    void studentCannotReachAdminArea() throws Exception {
        String token = accessTokenFor(uniqueEmail());

        mockMvc.perform(get("/api/v1/admin/questions").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("estudante nao acessa endpoints operacionais do actuator")
    void studentCannotReachActuator() throws Exception {
        String token = accessTokenFor(uniqueEmail());

        mockMvc.perform(get("/actuator/metrics").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("health check permanece publico")
    void healthCheckIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    /** Rota inexistente tambem exige autenticacao: a politica e negar por padrao. */
    @Test
    @DisplayName("rota desconhecida exige autenticacao")
    void unknownRouteRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/rota-que-nao-existe"))
                .andExpect(status().isUnauthorized());
    }

    private String accessTokenFor(String email) throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterPayload(email, VALID_PASSWORD, "Teste"));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("accessToken").asText();
    }

    private static String uniqueEmail() {
        return "authz-" + UUID.randomUUID() + "@koda.dev";
    }

    private record RegisterPayload(String email, String password, String displayName) {
    }
}
