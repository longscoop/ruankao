package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.question.FavoriteQuestionService;
import com.longscoop.ruankao.question.QuestionSessionService;
import com.longscoop.ruankao.question.persistence.FavoriteQuestionEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/favorites")
public class FavoriteQuestionController {

    private final FavoriteQuestionService favoriteService;
    private final QuestionSessionService questionSessionService;

    public FavoriteQuestionController(
            FavoriteQuestionService favoriteService,
            QuestionSessionService questionSessionService) {
        this.favoriteService = favoriteService;
        this.questionSessionService = questionSessionService;
    }

    @GetMapping
    public List<FavoriteResponse> list(
            @AuthenticationPrincipal RuankaoPrincipal principal) {
        return favoriteService.list(principal.userId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @PutMapping("/{questionId}")
    public ResponseEntity<Void> add(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @PathVariable long questionId) {
        favoriteService.add(principal.userId(), questionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @PathVariable long questionId) {
        favoriteService.remove(principal.userId(), questionId);
        return ResponseEntity.noContent().build();
    }

    private FavoriteResponse toResponse(FavoriteQuestionEntity entity) {
        return new FavoriteResponse(
                questionSessionService.findQuestionView(entity.getQuestionId())
                        .orElseThrow(() -> new IllegalStateException("favorite question not found")),
                entity.getCreatedAt());
    }

    public record FavoriteResponse(
            QuestionSessionService.QuestionView question,
            OffsetDateTime savedAt) {
    }
}
