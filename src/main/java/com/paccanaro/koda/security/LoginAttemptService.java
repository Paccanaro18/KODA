package com.paccanaro.koda.security;

import com.paccanaro.koda.config.KodaSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Protecao contra brute force e credential stuffing (SEC-06).
 *
 * <p>Conta tentativas falhas por conta e por IP separadamente. O bloqueio por
 * conta impede adivinhacao de senha de um alvo especifico; o bloqueio por IP
 * impede varredura de muitas contas a partir da mesma origem.
 *
 * <p>O e-mail nunca vira chave em claro no Redis — e guardado como hash, para
 * que um dump do cache nao revele quais contas existem.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final String ACCOUNT_PREFIX = "koda:login:fail:account:";
    private static final String IP_PREFIX = "koda:login:fail:ip:";
    /** O limite por IP e mais folgado: uma rede corporativa compartilha origem. */
    private static final int IP_MULTIPLIER = 4;

    private final StringRedisTemplate redis;
    private final int maxAttempts;
    private final Duration lockout;

    public LoginAttemptService(StringRedisTemplate redis, KodaSecurityProperties properties) {
        this.redis = redis;
        this.maxAttempts = properties.rateLimit().loginMaxAttempts();
        this.lockout = properties.rateLimit().loginLockout();
    }

    public boolean isBlocked(String email, String ip) {
        return count(ACCOUNT_PREFIX + hash(email)) >= maxAttempts
                || count(IP_PREFIX + ip) >= (long) maxAttempts * IP_MULTIPLIER;
    }

    public void recordFailure(String email, String ip) {
        increment(ACCOUNT_PREFIX + hash(email));
        increment(IP_PREFIX + ip);
    }

    /** Login bem-sucedido zera o contador da conta, mas nao o do IP. */
    public void recordSuccess(String email) {
        try {
            redis.delete(ACCOUNT_PREFIX + hash(email));
        } catch (RuntimeException e) {
            log.warn("Falha ao limpar contador de login: {}", e.getMessage());
        }
    }

    private long count(String key) {
        try {
            String value = redis.opsForValue().get(key);
            return value == null ? 0L : Long.parseLong(value);
        } catch (RuntimeException e) {
            log.warn("Contador de login indisponivel: {}", e.getMessage());
            return 0L;
        }
    }

    private void increment(String key) {
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, lockout);
            }
        } catch (RuntimeException e) {
            log.warn("Falha ao registrar tentativa de login: {}", e.getMessage());
        }
    }

    private static String hash(String email) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(email.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", e);
        }
    }
}
