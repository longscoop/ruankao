package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.course.LearnerContentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final LearnerContentService contentService;

    public CourseController(LearnerContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public List<LearnerContentService.CourseSummary> list(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @RequestParam(required = false) Long examId) {
        return contentService.listCourses(principal.userId(), examId);
    }

    @GetMapping("/{courseId}")
    public LearnerContentService.CourseDetail get(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @PathVariable long courseId) {
        return contentService.getCourse(principal.userId(), courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "course not found"));
    }
}
