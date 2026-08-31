package com.paccanaro.koda.auth;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.paccanaro.koda.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Fluxo de autenticacao")
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String REFRESH_COOKIE = "koda_refresh";
    private static final String VALID_PASSWORD = "uma-senha-bem-longa-123";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("registra, autentica e devolve os proprios dados")
    void registersAndAuthenticates() throws Exception {
        String email = uniqueEmail();

        MvcResult registration = register(email, VALID_PASSWORD, "Artur")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        String accessToken = readAccessToken(registration);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.displayName").value("Artur"));
    }

    @Test
    @DisplayName("o refresh token vem em cookie httpOnly, nunca no corpo da resposta")
    void refreshTokenIsHttpOnlyCookieAndNeverInBody() throws Exception {
        MvcResult result = register(uniqueEmail(), VALID_PASSWORD, "Cookie").andReturn();

        Cookie cookie = result.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getValue()).isNotBlank();

        // Um XSS que leia o corpo da resposta nao pode encontrar o refresh token.
        assertThat(result.getResponse().getContentAsString()).doesNotContain(cookie.getValue());
    }

    @Test
    @DisplayName("nao revela se o e-mail existe quando a senha esta errada")
    void doesNotRevealWhetherEmailExists() throws Exception {
        String email = uniqueEmail();
        register(email, VALID_PASSWORD, "Enum").andExpect(status().isCreated());

        String bodyForExistingEmail = login(email, "senha-errada-mas-longa")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String bodyForUnknownEmail = login(uniqueEmail(), "senha-errada-mas-longa")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(readField(bodyForExistingEmail, "error"))
                .isEqualTo(readField(bodyForUnknownEmail, "error"))
                .isEqualTo("invalid_credentials");
        assertThat(readField(bodyForExistingEmail, "message"))
                .isEqualTo(readField(bodyForUnknownEmail, "message"));
    }

    @Test
    @DisplayName("rotaciona o refresh token e invalida o anterior")
    void rotatesRefreshTokenAndInvalidatesThePrevious() throws Exception {
        MvcResult registration = register(uniqueEmail(), VALID_PASSWORD, "Rotacao").andReturn();
        Cookie firstToken = registration.getResponse().getCookie(REFRESH_COOKIE);

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstToken))
                .andExpect(status().isOk())
                .andReturn();

        Cookie secondToken = refreshed.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(secondToken).isNotNull();
        assertThat(secondToken.getValue()).isNotEqualTo(firstToken.getValue());

        // Reapresentar o token ja rotacionado deve falhar.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("reuso de token rotacionado revoga toda a arvore de sessoes")
    void reuseOfRotatedTokenRevokesEntireSessionTree() throws Exception {
        MvcResult registration = register(uniqueEmail(), VALID_PASSWORD, "Reuso").andReturn();
        Cookie stolenToken = registration.getResponse().getCookie(REFRESH_COOKIE);

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(stolenToken))
                .andExpect(status().isOk())
                .andReturn();
        Cookie legitimateToken = refreshed.getResponse().getCookie(REFRESH_COOKIE);

        // O atacante usa o token antigo: dispara a deteccao de reuso.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(stolenToken))
                .andExpect(status().isUnauthorized());

        // O token legitimo tambem cai — e o comportamento correto, porque a
        // essa altura nao ha como distinguir o dono do atacante.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(legitimateToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout revoga a sessao e limpa o cookie")
    void logoutRevokesSession() throws Exception {
        MvcResult registration = register(uniqueEmail(), VALID_PASSWORD, "Logout").andReturn();
        Cookie token = registration.getResponse().getCookie(REFRESH_COOKIE);
        String accessToken = readAccessToken(registration);

        MockHttpServletResponse logout = mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(token)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        assertThat(logout.getCookie(REFRESH_COOKIE)).isNotNull();
        assertThat(logout.getCookie(REFRESH_COOKIE).getMaxAge()).isZero();

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejeita senha curta demais")
    void rejectsShortPassword() throws Exception {
        register(uniqueEmail(), "curta", "Curta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.fields.password").isNotEmpty());
    }

    @Test
    @DisplayName("trata e-mail como case-insensitive")
    void treatsEmailAsCaseInsensitive() throws Exception {
        String email = uniqueEmail();
        register(email, VALID_PASSWORD, "Case").andExpect(status().isCreated());

        login(email.toUpperCase(java.util.Locale.ROOT), VALID_PASSWORD)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("nao permite cadastrar o mesmo e-mail duas vezes")
    void rejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        register(email, VALID_PASSWORD, "Primeiro").andExpect(status().isCreated());
        register(email, VALID_PASSWORD, "Segundo").andExpect(status().isConflict());
    }

    // --- helpers ------------------------------------------------------------

    private org.springframework.test.web.servlet.ResultActions register(
            String email, String password, String displayName) throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterPayload(email, password, displayName));
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password)
            throws Exception {
        String body = objectMapper.writeValueAsString(new LoginPayload(email, password));
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String readAccessToken(MvcResult result) throws Exception {
        return readField(result.getResponse().getContentAsString(), "accessToken");
    }

    private String readField(String json, String field) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.path(field).asText();
    }

    /** E-mail unico por teste: evita acoplamento entre casos e permite paralelismo. */
    private static String uniqueEmail() {
        return "teste-" + UUID.randomUUID() + "@koda.dev";
    }

    private record RegisterPayload(String email, String password, String displayName) {
    }

    private record LoginPayload(String email, String password) {
    }
}
