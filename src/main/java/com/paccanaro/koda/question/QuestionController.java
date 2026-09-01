package com.paccanaro.koda.question;

import com.paccanaro.koda.question.dto.AttemptResultResponse;
import com.paccanaro.koda.question.dto.PracticeQuestionResponse;
import com.paccanaro.koda.question.dto.SubmitAttemptRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private static final int MAX_SESSION_SIZE = 25;

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /** O id do usuario vem do token — mesma defesa anti-IDOR de /auth/me e /curriculum/map (SEC-03). */
    @GetMapping("/practice-session")
    public List<PracticeQuestionResponse> practiceSession(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Set<String> types,
            @RequestParam(defaultValue = "8") int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), MAX_SESSION_SIZE);
        return questionService.practiceSession(UUID.fromString(jwt.getSubject()), types, boundedLimit);
    }

    @PostMapping("/attempts")
    public AttemptResultResponse submitAttempt(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SubmitAttemptRequest request) {
        return questionService.submitAttempt(
                UUID.fromString(jwt.getSubject()), request.questionVersionId(), request.submittedAnswer(), request.responseTimeMs());
    }
}
