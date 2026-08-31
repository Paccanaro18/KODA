package com.paccanaro.koda.auth.token;

import com.paccanaro.koda.config.KodaSecurityProperties;
import com.paccanaro.koda.user.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Emissao de access tokens (JWT curto) e de refresh tokens opacos.
 *
 * <p>O access token e um JWT de vida curta validado sem consultar o banco. O
 * refresh token e um valor aleatorio opaco — nao carrega claims, e revogavel,
 * e so vale contra o hash guardado em {@code refresh_tokens}.
 */
@Service
public class JwtService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final JwtEncoder jwtEncoder;
    private final KodaSecurityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtService(JwtEncoder jwtEncoder, KodaSecurityProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    /**
     * O subject e o id do usuario, nunca o e-mail: o token circula por logs,
     * proxies e pelo cliente, e nao deve carregar dado pessoal.
     */
    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .issuedAt(now)
                .expiresAt(now.plus(properties.jwt().accessTokenTtl()))
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** Valor opaco de 256 bits, seguro para URL. O hash e o que vai ao banco. */
    public String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public Duration accessTokenTtl() {
        return properties.jwt().accessTokenTtl();
    }

    public Duration refreshTokenTtl() {
        return properties.jwt().refreshTokenTtl();
    }
}
