package com.paccanaro.koda.security;

import tools.jackson.databind.ObjectMapper;
import com.paccanaro.koda.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Protecao contra brute force (SEC-06). */
@DisplayName("Brute force no login")
class LoginBruteForceIntegrationTest extends AbstractIntegrationTest {

    private static final String VALID_PASSWORD = "uma-senha-bem-longa-123";
    /** Deve refletir koda.security.rate-limit.login-max-attempts. */
    private static final int MAX_ATTEMPTS = 5;

    @Autowired
    private ObjectMapper objectMapper;

    // A limpeza dos contadores no Redis fica em AbstractIntegrationTest.

    @Test
    @DisplayName("bloqueia a conta apos exceder o limite de tentativas")
    void locksAccountAfterTooManyFailures() throws Exception {
        String email = uniqueEmail();
        register(email);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            login(email, "senha-errada-porem-longa")
                    .andExpect(status().isUnauthorized());
        }

        // Excedido o limite, a resposta muda de 401 para 429.
        login(email, "senha-errada-porem-longa")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("account_locked"));
    }

    /**
     * Detalhe que costuma passar despercebido: apos o bloqueio, nem a senha
     * correta deve funcionar. Caso contrario o atacante saberia que acertou.
     */
    @Test
    @DisplayName("a senha correta tambem e bloqueada enquanto durar o lockout")
    void correctPasswordIsAlsoBlockedDuringLockout() throws Exception {
        String email = uniqueEmail();
        register(email);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            login(email, "senha-errada-porem-longa");
        }

        login(email, VALID_PASSWORD)
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("login bem-sucedido zera o contador de falhas da conta")
    void successfulLoginResetsCounter() throws Exception {
        String email = uniqueEmail();
        register(email);

        for (int attempt = 0; attempt < MAX_ATTEMPTS - 1; attempt++) {
            login(email, "senha-errada-porem-longa")
                    .andExpect(status().isUnauthorized());
        }

        login(email, VALID_PASSWORD).andExpect(status().isOk());

        // Contador zerado: uma nova falha nao deve bloquear imediatamente.
        login(email, "senha-errada-porem-longa")
                .andExpect(status().isUnauthorized());
    }

    private void register(String email) throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterPayload(email, VALID_PASSWORD, "BruteForce"));
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password)
            throws Exception {
        String body = objectMapper.writeValueAsString(new LoginPayload(email, password));
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String uniqueEmail() {
        return "brute-" + UUID.randomUUID() + "@koda.dev";
    }

    private record RegisterPayload(String email, String password, String displayName) {
    }

    private record LoginPayload(String email, String password) {
    }
}
