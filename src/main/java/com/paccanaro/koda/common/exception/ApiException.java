package com.paccanaro.koda.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Erro de dominio com codigo estavel e mensagem segura para o cliente.
 * Nada aqui deve conter detalhe interno, stack trace ou dado de outro usuario.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    // --- fabricas para os casos da Fase 1 -----------------------------------

    /**
     * Mesma resposta para e-mail inexistente e senha incorreta: distingui-las
     * permitiria enumerar quais contas existem na plataforma.
     */
    public static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials",
                "E-mail ou senha incorretos.");
    }

    public static ApiException accountLocked() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "account_locked",
                "Muitas tentativas de login. Tente novamente mais tarde.");
    }

    public static ApiException accountInactive() {
        return new ApiException(HttpStatus.FORBIDDEN, "account_inactive",
                "Esta conta nao esta ativa.");
    }

    public static ApiException emailAlreadyRegistered() {
        return new ApiException(HttpStatus.CONFLICT, "email_already_registered",
                "Nao foi possivel concluir o cadastro com esses dados.");
    }

    public static ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_refresh_token",
                "Sessao expirada. Faca login novamente.");
    }

    public static ApiException notFound(String resource) {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", resource + " nao encontrado.");
    }
}
