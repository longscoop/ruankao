package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.course.LearnerContentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    private final LearnerContentService contentService;

    public VideoController(LearnerContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/{videoId}")
    public LearnerContentService.VideoDetail get(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @PathVariable long videoId) {
        return contentService.getVideo(principal.userId(), videoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "video not found"));
    }

    @PutMapping("/{videoId}/progress")
    public ResponseEntity<Void> updateProgress(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @PathVariable long videoId,
            @RequestBody ProgressRequest request) {
        if (!contentService.updateVideoProgress(
                principal.userId(), videoId, request.progressSeconds())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "video not found");
        }
        return ResponseEntity.noContent().build();
    }

    public record ProgressRequest(int progressSeconds) {
    }
}
