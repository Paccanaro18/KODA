package com.paccanaro.koda.auth.token;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Revogacao de emergencia de sessoes, em transacao propria.
 *
 * <p>Existe separado de {@link RefreshTokenService} por um motivo especifico e
 * nao obvio. A deteccao de reuso de refresh token acontece dentro de um fluxo
 * que termina lancando uma excecao para negar a requisicao. Como essa excecao e
 * unchecked, ela dispara rollback da transacao corrente — e o rollback desfaria
 * justamente a revogacao que acabou de ser feita. O resultado seria uma defesa
 * que nunca chega a persistir: o atacante recebe 401, mas as sessoes roubadas
 * continuam ativas.
 *
 * <p>{@code REQUIRES_NEW} garante que a revogacao seja commitada em transacao
 * independente, sobrevivendo ao rollback da transacao que a chamou. A chamada
 * precisa atravessar o proxy do Spring, e por isso mora em outro bean.
 */
@Service
public class SessionRevocationService {

    private final RefreshTokenRepository repository;

    public SessionRevocationService(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int revokeAllForUserImmediately(UUID userId) {
        return repository.revokeAllForUser(userId, Instant.now());
    }
}
