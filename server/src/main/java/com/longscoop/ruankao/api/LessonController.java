package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.course.LearnerContentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/lessons")
public class LessonController {

    private final LearnerContentService contentService;

    public LessonController(LearnerContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/{lessonId}")
    public LearnerContentService.LessonDetail get(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @PathVariable long lessonId) {
        return contentService.getLesson(principal.userId(), lessonId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "lesson not found"));
    }
}
