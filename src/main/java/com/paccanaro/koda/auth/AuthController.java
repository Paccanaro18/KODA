package com.paccanaro.koda.auth;

import com.paccanaro.koda.auth.dto.AuthResponse;
import com.paccanaro.koda.auth.dto.LoginRequest;
import com.paccanaro.koda.auth.dto.MeResponse;
import com.paccanaro.koda.auth.dto.RegisterRequest;
import com.paccanaro.koda.common.exception.ApiException;
import com.paccanaro.koda.security.ClientIpResolver;
import com.paccanaro.koda.user.Profile;
import com.paccanaro.koda.user.ProfileRepository;
import com.paccanaro.koda.user.User;
import com.paccanaro.koda.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "koda_refresh";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final Duration refreshTokenTtl;

    public AuthController(AuthService authService,
                          UserRepository userRepository,
                          ProfileRepository profileRepository,
                          com.paccanaro.koda.auth.token.JwtService jwtService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.refreshTokenTtl = jwtService.refreshTokenTtl();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest) {
        AuthService.AuthTokens tokens = authService.register(request, userAgent(httpRequest));
        return respondWithTokens(tokens, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        AuthService.AuthTokens tokens = authService.login(
                request, ClientIpResolver.resolve(httpRequest), userAgent(httpRequest));
        return respondWithTokens(tokens, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest httpRequest) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw ApiException.invalidRefreshToken();
        }
        AuthService.AuthTokens tokens = authService.refresh(refreshToken, userAgent(httpRequest));
        return respondWithTokens(tokens, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {

        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    /**
     * Dados do proprio usuario. O id vem do token — nunca de parametro da
     * requisicao — o que elimina a possibilidade de IDOR neste endpoint (SEC-03).
     */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Usuario"));
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("Perfil"));

        return MeResponse.of(user, profile);
    }

    private ResponseEntity<AuthResponse> respondWithTokens(AuthService.AuthTokens tokens, HttpStatus status) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken()).toString())
                .body(AuthResponse.bearer(tokens.accessToken(), tokens.expiresInSeconds()));
    }

    /**
     * httpOnly: fora do alcance de JavaScript, entao um XSS nao rouba a sessao.
     * SameSite=Strict: o navegador nao envia o cookie em requisicao cross-site,
     * o que fecha o vetor de CSRF no endpoint de refresh.
     * Path restrito: o cookie so trafega nas rotas que precisam dele.
     */
    private ResponseCookie refreshCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(refreshTokenTtl)
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    private static String userAgent(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }
}
