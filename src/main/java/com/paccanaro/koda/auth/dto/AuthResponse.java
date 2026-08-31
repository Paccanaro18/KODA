package com.paccanaro.koda.auth.dto;

/**
 * O refresh token NAO aparece aqui: ele viaja em cookie httpOnly, fora do
 * alcance de JavaScript. Devolve-lo no corpo obrigaria o cliente a guarda-lo
 * em localStorage, onde qualquer XSS o roubaria (SEC-07).
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
    public static AuthResponse bearer(String accessToken, long expiresInSeconds) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
