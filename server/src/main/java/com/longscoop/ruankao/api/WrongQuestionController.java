package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.question.QuestionSessionService;
import com.longscoop.ruankao.question.WrongQuestionService;
import com.longscoop.ruankao.question.persistence.WrongQuestionEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/wrong-questions")
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;
    private final QuestionSessionService questionSessionService;

    public WrongQuestionController(
            WrongQuestionService wrongQuestionService,
            QuestionSessionService questionSessionService) {
        this.wrongQuestionService = wrongQuestionService;
        this.questionSessionService = questionSessionService;
    }

    @GetMapping
    public List<WrongQuestionResponse> list(
            @AuthenticationPrincipal RuankaoPrincipal principal) {
        return wrongQuestionService.listForUser(principal.userId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private WrongQuestionResponse toResponse(WrongQuestionEntity entity) {
        return new WrongQuestionResponse(
                entity.getId(),
                entity.getQuestionId(),
                entity.getStatus().name(),
                entity.getWrongCount(),
                entity.getConsecutiveCorrect(),
                entity.getLastWrongAt(),
                questionSessionService.findQuestionView(entity.getQuestionId()).orElse(null));
    }

    public record WrongQuestionResponse(
            long id,
            long questionId,
            String status,
            int wrongCount,
            int consecutiveCorrect,
            OffsetDateTime lastWrongAt,
            QuestionSessionService.QuestionView question) {
    }
}
