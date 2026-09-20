package com.longscoop.ruankao.api;

import com.longscoop.ruankao.assessment.AssessmentAnswerResult;
import com.longscoop.ruankao.assessment.AssessmentResult;
import com.longscoop.ruankao.assessment.AssessmentService;
import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.learning.model.AnswerConfidence;
import com.longscoop.ruankao.question.persistence.QuestionEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @PostMapping
    public ResponseEntity<StartAssessmentResponse> start(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @RequestBody StartAssessmentRequest request) {
        UUID sessionId = assessmentService.start(
                principal.userId(),
                request.examId(),
                request.questionCount());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new StartAssessmentResponse(sessionId));
    }

    @GetMapping("/{sessionId}/questions")
    public List<AssessmentQuestionResponse> questions(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @PathVariable UUID sessionId) {
        requireOwnedSession(principal.userId(), sessionId);
        return assessmentService.listQuestions(sessionId).stream()
                .map(AssessmentQuestionResponse::from)
                .toList();
    }

    @PostMapping("/{sessionId}/answers")
    public AssessmentAnswerResponse answer(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @PathVariable UUID sessionId,
            @RequestBody AssessmentAnswerRequest request) {
        requireOwnedSession(principal.userId(), sessionId);
        AssessmentAnswerResult result = assessmentService.answer(
                sessionId,
                request.questionId(),
                request.answer(),
                request.durationSeconds(),
                request.confidence());
        return new AssessmentAnswerResponse(
                result.questionId(),
                result.correct(),
                result.answerRecordId());
    }

    @PostMapping("/{sessionId}/submit")
    public AssessmentSubmitResponse submit(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @PathVariable UUID sessionId) {
        requireOwnedSession(principal.userId(), sessionId);
        AssessmentResult result = assessmentService.submit(sessionId);
        return new AssessmentSubmitResponse(
                result.totalQuestions(),
                result.correctQuestions());
    }

    private void requireOwnedSession(long userId, UUID sessionId) {
        var session = assessmentService.findSession(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "assessment session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "assessment session belongs to another user");
        }
    }

    public record StartAssessmentRequest(long examId, int questionCount) {
    }

    public record StartAssessmentResponse(UUID sessionId) {
    }

    public record AssessmentQuestionResponse(
            long id,
            String type,
            String difficulty,
            String content) {

        static AssessmentQuestionResponse from(QuestionEntity question) {
            return new AssessmentQuestionResponse(
                    question.getId(),
                    question.getType().name(),
                    question.getDifficulty().name(),
                    question.getContent());
        }
    }

    public record AssessmentAnswerRequest(
            long questionId,
            String answer,
            Integer durationSeconds,
            AnswerConfidence confidence) {
    }

    public record AssessmentAnswerResponse(
            long questionId,
            boolean correct,
            UUID answerRecordId) {
    }

    public record AssessmentSubmitResponse(
            int totalQuestions,
            int correctQuestions) {
    }
}
