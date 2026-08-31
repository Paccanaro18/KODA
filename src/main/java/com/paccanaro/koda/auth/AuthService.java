package com.paccanaro.koda.auth;

import com.paccanaro.koda.auth.dto.LoginRequest;
import com.paccanaro.koda.auth.dto.RegisterRequest;
import com.paccanaro.koda.auth.token.JwtService;
import com.paccanaro.koda.auth.token.RefreshTokenService;
import com.paccanaro.koda.common.exception.ApiException;
import com.paccanaro.koda.security.LoginAttemptService;
import com.paccanaro.koda.user.Profile;
import com.paccanaro.koda.user.ProfileRepository;
import com.paccanaro.koda.user.User;
import com.paccanaro.koda.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /**
     * Hash descartavel usado quando o e-mail nao existe. Verificar a senha
     * contra ele custa o mesmo tempo de um login real, o que impede descobrir
     * contas existentes medindo a latencia da resposta.
     *
     * <p>Gerado em runtime a partir de um valor aleatorio: um hash fixo no
     * codigo poderia ficar invalido se os parametros do encoder mudassem, e
     * ai {@code matches()} lancaria excecao em vez de gastar o tempo esperado.
     */
    private final String dummyHash;

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserRepository userRepository,
                       ProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptService = loginAttemptService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthTokens register(RegisterRequest request, String userAgent) {
        String email = normalize(request.email());

        // A verificacao previa e apenas conveniencia; a garantia real e o
        // indice unico no banco, que fecha a corrida entre dois cadastros
        // simultaneos com o mesmo e-mail.
        if (userRepository.existsByEmail(email)) {
            throw ApiException.emailAlreadyRegistered();
        }

        User user = User.create(email, passwordEncoder.encode(request.password()));
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.emailAlreadyRegistered();
        }

        profileRepository.save(Profile.createFor(user, request.displayName()));

        log.info("Novo usuario registrado: {}", user.getId());
        return issueTokens(user, userAgent);
    }

    @Transactional
    public AuthTokens login(LoginRequest request, String clientIp, String userAgent) {
        String email = normalize(request.email());

        if (loginAttemptService.isBlocked(email, clientIp)) {
            throw ApiException.accountLocked();
        }

        Optional<User> found = userRepository.findByEmail(email);

        // Sempre executa uma verificacao de hash, exista o usuario ou nao.
        String hashToCheck = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (found.isEmpty() || !passwordMatches) {
            loginAttemptService.recordFailure(email, clientIp);
            throw ApiException.invalidCredentials();
        }

        User user = found.get();
        if (!user.isActive()) {
            loginAttemptService.recordFailure(email, clientIp);
            throw ApiException.accountInactive();
        }

        loginAttemptService.recordSuccess(email);
        return issueTokens(user, userAgent);
    }

    @Transactional
    public AuthTokens refresh(String rawRefreshToken, String userAgent) {
        return refreshTokenService.rotate(rawRefreshToken, userAgent)
                .map(rotation -> new AuthTokens(
                        jwtService.issueAccessToken(rotation.user()),
                        rotation.rawRefreshToken(),
                        jwtService.accessTokenTtl().toSeconds()))
                .orElseThrow(ApiException::invalidRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    private AuthTokens issueTokens(User user, String userAgent) {
        return new AuthTokens(
                jwtService.issueAccessToken(user),
                refreshTokenService.issue(user, userAgent),
                jwtService.accessTokenTtl().toSeconds());
    }

    /** Normaliza para minusculas: o banco tem CHECK que rejeita qualquer outra forma. */
    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public record AuthTokens(String accessToken, String refreshToken, long expiresInSeconds) {
    }
}
