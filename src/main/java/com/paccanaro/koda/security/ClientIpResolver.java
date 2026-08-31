package com.paccanaro.koda.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolve o IP de origem para fins de rate limit.
 *
 * <p>Usa deliberadamente {@code getRemoteAddr()} e <strong>ignora</strong>
 * {@code X-Forwarded-For}. Esse cabecalho e enviado pelo cliente e pode ser
 * forjado: confiar nele permitiria a qualquer atacante burlar todo o rate limit
 * apenas variando o valor.
 *
 * <p>Em producao atras de um proxy reverso, a forma correta e configurar
 * {@code server.forward-headers-strategy=framework} e garantir que apenas o
 * proxy confiavel possa alcancar a aplicacao — assim o proprio container ja
 * entrega o IP real em {@code getRemoteAddr()}.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "unknown" : remoteAddr;
    }
}
