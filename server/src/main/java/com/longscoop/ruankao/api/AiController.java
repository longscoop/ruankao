package com.longscoop.ruankao.api;

import com.longscoop.ruankao.ai.AiQuotaExceededException;
import com.longscoop.ruankao.ai.AiService;
import com.longscoop.ruankao.auth.RuankaoPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public AiResponse chat(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @RequestBody ChatRequest request) {
        try {
            return AiResponse.from(aiService.chat(principal.userId(), request.message()));
        } catch (AiQuotaExceededException e) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI service unavailable", e);
        }
    }

    @PostMapping("/question-explain")
    public AiResponse explainQuestion(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @RequestBody QuestionExplainRequest request) {
        try {
            return AiResponse.from(aiService.explainQuestion(principal.userId(), request.questionId()));
        } catch (AiQuotaExceededException e) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI service unavailable", e);
        }
    }

    public record ChatRequest(String message) {
    }

    public record QuestionExplainRequest(long questionId) {
    }

    public record AiResponse(String content, UUID requestId) {
        static AiResponse from(AiService.AiResult result) {
            return new AiResponse(result.content(), result.requestId());
        }
    }
}
