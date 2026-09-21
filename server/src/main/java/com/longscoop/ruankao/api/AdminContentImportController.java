package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.content.ContentImportReviewService;
import com.longscoop.ruankao.content.ContentImportService;
import com.longscoop.ruankao.learning.model.QuestionDifficulty;
import com.longscoop.ruankao.question.QuestionKnowledgeLink;
import com.longscoop.ruankao.question.model.QuestionSource;
import com.longscoop.ruankao.storage.StorageProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/imports")
public class AdminContentImportController {

    private final ContentImportService importService;
    private final ContentImportReviewService reviewService;
    private final StorageProvider storageProvider;

    public AdminContentImportController(
            ContentImportService importService,
            ContentImportReviewService reviewService,
            StorageProvider storageProvider) {
        this.importService = importService;
        this.reviewService = reviewService;
        this.storageProvider = storageProvider;
    }

    @GetMapping
    public List<ContentImportReviewService.BatchSummary> list(
            @RequestParam(required = false) Long examId) {
        return reviewService.list(examId);
    }

    @PostMapping(value = "/pdf", consumes = "multipart/form-data")
    public ResponseEntity<ContentImportService.DryRunResult> upload(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @RequestParam long examId,
            @RequestPart("file") MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    importService.dryRun(
                            examId,
                            file.getOriginalFilename(),
                            file.getBytes(),
                            principal.userId()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to read PDF", e);
        }
    }

    @PutMapping("/{batchId}")
    public ResponseEntity<Void> updateBatch(
            @PathVariable UUID batchId,
            @RequestBody UpdateBatchRequest request) {
        importService.updateBatchTitle(batchId, request.title());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{batchId}")
    public ContentImportService.ImportDetail detail(@PathVariable UUID batchId) {
        return importService.get(batchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "import batch not found"));
    }

    @GetMapping("/{batchId}/pages/{pageNumber}/preview-url")
    public Map<String, String> pagePreview(
            @PathVariable UUID batchId,
            @PathVariable int pageNumber) {
        var detail = detail(batchId);
        var page = detail.pages().stream()
                .filter(x -> x.pageNumber() == pageNumber)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "import page not found"));
        return Map.of(
                "url",
                storageProvider.generateAccessUrl(
                        page.imageObjectKey(),
                        Duration.ofMinutes(30)).toString());
    }

    @PostMapping("/{batchId}/approve-all")
    public ResponseEntity<Void> approveAll(@PathVariable UUID batchId) {
        importService.approveAll(batchId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{batchId}/items/{itemId}/approve")
    public ResponseEntity<Void> approve(
            @PathVariable UUID batchId,
            @PathVariable long itemId) {
        reviewService.approveItem(batchId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{batchId}/items/{itemId}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable UUID batchId,
            @PathVariable long itemId) {
        reviewService.rejectItem(batchId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{batchId}/items/{itemId}")
    public ResponseEntity<Void> updateItem(
            @PathVariable UUID batchId,
            @PathVariable long itemId,
            @RequestBody UpdateItemRequest request) {
        reviewService.updateItem(batchId, itemId, request.title(), request.contentJson());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{batchId}/items/{itemId}/ai-suggest")
    public com.longscoop.ruankao.ai.AiService.AiResult aiSuggest(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @PathVariable UUID batchId,
            @PathVariable long itemId) {
        return reviewService.suggestStructure(principal.userId(), batchId, itemId);
    }

    @PostMapping("/{batchId}/issues/{issueId}/resolve")
    public ResponseEntity<Void> resolveIssue(
            @PathVariable UUID batchId,
            @PathVariable long issueId) {
        reviewService.resolveIssue(batchId, issueId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{batchId}/confirm")
    public ContentImportService.ConfirmResult confirm(
            @PathVariable UUID batchId,
            @RequestBody ConfirmRequest request) {
        return importService.confirm(batchId, request.confirmKey());
    }

    @PostMapping("/{batchId}/items/{itemId}/publish-lesson")
    public ResponseEntity<Void> publishLesson(
            @PathVariable UUID batchId,
            @PathVariable long itemId,
            @RequestBody PublishLessonRequest request) {
        reviewService.publishLesson(batchId, itemId, request.knowledgeIds());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{batchId}/items/{itemId}/publish-knowledge")
    public PublishResult publishKnowledge(
            @PathVariable UUID batchId,
            @PathVariable long itemId,
            @RequestBody PublishKnowledgeRequest request) {
        long id = reviewService.publishKnowledge(
                batchId, itemId, request.code(), request.parentId(),
                request.importance(), request.examFrequency(),
                request.estimatedMinutes(), request.sortOrder());
        return new PublishResult(id);
    }

    @PostMapping("/{batchId}/items/{itemId}/publish-question")
    public PublishResult publishQuestion(
            @PathVariable UUID batchId,
            @PathVariable long itemId,
            @RequestBody PublishQuestionRequest request) {
        long id = reviewService.publishQuestion(
                batchId, itemId, request.difficulty(), request.source(), request.knowledgeLinks());
        return new PublishResult(id);
    }

    public record UpdateBatchRequest(String title) {}
    public record ConfirmRequest(String confirmKey) {}
    public record UpdateItemRequest(String title, String contentJson) {}
    public record PublishLessonRequest(List<Long> knowledgeIds) {}
    public record PublishKnowledgeRequest(
            String code, Long parentId, int importance, double examFrequency,
            int estimatedMinutes, int sortOrder) {}
    public record PublishQuestionRequest(
            QuestionDifficulty difficulty,
            QuestionSource source,
            List<QuestionKnowledgeLink> knowledgeLinks) {}
    public record PublishResult(long id) {}
}
