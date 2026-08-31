package com.paccanaro.koda.security;

import com.paccanaro.koda.config.KodaSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate limit geral por IP, em janela fixa de um minuto (SEC-06).
 *
 * <p>Janela fixa e um compromisso deliberado: permite ate 2x o limite na virada
 * da janela, mas custa uma unica operacao no Redis. Para o limite geral isso
 * basta; o caminho sensivel de login tem protecao propria e mais estrita em
 * {@link LoginAttemptService}.
 *
 * <p>Se o Redis estiver indisponivel, a requisicao passa. E uma escolha
 * consciente de disponibilidade sobre limitacao: o rate limit e defesa em
 * profundidade, nao o unico controle de acesso.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String KEY_PREFIX = "koda:ratelimit:ip:";

    private final StringRedisTemplate redis;
    private final int requestsPerMinute;

    public RateLimitFilter(StringRedisTemplate redis, KodaSecurityProperties properties) {
        this.redis = redis;
        this.requestsPerMinute = properties.rateLimit().requestsPerMinute();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = KEY_PREFIX + ClientIpResolver.resolve(request);

        long count;
        try {
            Long incremented = redis.opsForValue().increment(key);
            count = incremented == null ? 1L : incremented;
            if (count == 1L) {
                redis.expire(key, Duration.ofMinutes(1));
            }
        } catch (RuntimeException e) {
            log.warn("Rate limit indisponivel (Redis); requisicao liberada: {}", e.getMessage());
            chain.doFilter(request, response);
            return;
        }

        if (count > requestsPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write(
                    "{\"error\":\"rate_limit_exceeded\",\"message\":\"Muitas requisicoes. Tente novamente em instantes.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
