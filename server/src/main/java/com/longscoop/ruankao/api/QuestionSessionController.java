package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.learning.model.AnswerConfidence;
import com.longscoop.ruankao.question.QuestionSessionService;
import com.longscoop.ruankao.question.model.PracticeSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/question-sessions")
public class QuestionSessionController {

    private final QuestionSessionService sessionService;

    public QuestionSessionController(QuestionSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<StartResponse> start(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @RequestBody StartRequest request) {
        UUID sessionId = sessionService.start(
                principal.userId(),
                request.source(),
                request.knowledgeId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new StartResponse(sessionId));
    }

    @GetMapping("/{sessionId}/questions")
    public List<QuestionSessionService.QuestionView> questions(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @PathVariable UUID sessionId) {
        return sessionService.listQuestions(principal.userId(), sessionId);
    }

    @PostMapping("/{sessionId}/answers")
    public QuestionSessionService.AnswerFeedback answer(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @PathVariable UUID sessionId,
            @RequestBody AnswerRequest request) {
        return sessionService.answer(
                principal.userId(),
                sessionId,
                request.questionId(),
                request.answer(),
                request.durationSeconds(),
                request.confidence());
    }

    public record StartRequest(PracticeSource source, Long knowledgeId) {
    }

    public record StartResponse(UUID sessionId) {
    }

    public record AnswerRequest(
            long questionId,
            String answer,
            Integer durationSeconds,
            AnswerConfidence confidence,
            PracticeSource source) {
    }
}
