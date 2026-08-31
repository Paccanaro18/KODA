package com.paccanaro.koda.auth.token;

import com.paccanaro.koda.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;

/** Ciclo de vida das sessoes: emissao, rotacao, revogacao e deteccao de reuso. */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;
    private final SessionRevocationService sessionRevocation;

    public RefreshTokenService(RefreshTokenRepository repository,
                               JwtService jwtService,
                               SessionRevocationService sessionRevocation) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.sessionRevocation = sessionRevocation;
    }

    /** Emite um refresh token novo e devolve o valor em claro (a unica vez que ele existe). */
    @Transactional
    public String issue(User user, String userAgent) {
        String rawToken = jwtService.generateRefreshToken();
        Instant expiresAt = Instant.now().plus(jwtService.refreshTokenTtl());

        RefreshToken token = RefreshToken.issue(user, hash(rawToken), expiresAt, truncate(userAgent));
        repository.save(token);
        return rawToken;
    }

    /**
     * Troca um refresh token por outro, revogando o anterior.
     *
     * <p>Se o token apresentado ja tiver sido rotacionado, trata-se de reuso —
     * indicio de roubo. A resposta e revogar toda a arvore de sessoes do usuario,
     * derrubando tanto o atacante quanto o dono legitimo.
     *
     * @return o novo token em claro, ou vazio se o token apresentado for invalido
     */
    @Transactional
    public Optional<RotationResult> rotate(String rawToken, String userAgent) {
        Optional<RefreshToken> found = repository.findByTokenHash(hash(rawToken));
        if (found.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken existing = found.get();

        if (existing.getReplacedBy() != null) {
            log.warn("Reuso de refresh token detectado; revogando sessoes do usuario {}",
                    existing.getUser().getId());
            // Transacao propria: quem chama este metodo lanca excecao logo em
            // seguida para negar a requisicao, e o rollback resultante desfaria
            // a revogacao. Ver SessionRevocationService.
            sessionRevocation.revokeAllForUserImmediately(existing.getUser().getId());
            return Optional.empty();
        }

        if (!existing.isUsable(Instant.now())) {
            return Optional.empty();
        }

        User user = existing.getUser();
        String newRawToken = jwtService.generateRefreshToken();
        Instant expiresAt = Instant.now().plus(jwtService.refreshTokenTtl());

        RefreshToken successor =
                repository.save(RefreshToken.issue(user, hash(newRawToken), expiresAt, truncate(userAgent)));
        existing.replaceWith(successor.getId());

        return Optional.of(new RotationResult(user, newRawToken));
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void revokeAllForUser(java.util.UUID userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    private static byte[] hash(String rawToken) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", e);
        }
    }

    private static String truncate(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() <= 255 ? userAgent : userAgent.substring(0, 255);
    }

    /** Resultado de uma rotacao bem-sucedida. */
    public record RotationResult(User user, String rawRefreshToken) {
    }
}
