package com.paccanaro.koda.curriculum;

import com.paccanaro.koda.curriculum.dto.CurriculumMapResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/curriculum")
public class CurriculumController {

    private final CurriculumService curriculumService;

    public CurriculumController(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    /**
     * O id do usuario vem do token, nunca de parametro da requisicao — mesma
     * defesa anti-IDOR do {@code /auth/me} (SEC-03).
     */
    @GetMapping("/map")
    public CurriculumMapResponse map(@AuthenticationPrincipal Jwt jwt) {
        return curriculumService.buildMap(UUID.fromString(jwt.getSubject()));
    }
}
