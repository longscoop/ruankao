package com.longscoop.ruankao.api;

import com.longscoop.ruankao.auth.RuankaoPrincipal;
import com.longscoop.ruankao.course.LearnerContentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final LearnerContentService contentService;

    public KnowledgeController(LearnerContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/{knowledgeId}")
    public LearnerContentService.KnowledgeDetail get(
            @AuthenticationPrincipal RuankaoPrincipal principal,
            @PathVariable long knowledgeId) {
        return contentService.getKnowledge(principal.userId(), knowledgeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "knowledge point not found"));
    }

    @GetMapping("/tree")
    public List<LearnerContentService.KnowledgeNode> tree(
            @AuthenticationPrincipal RuankaoPrincipal principal) {
        return contentService.knowledgeTree(principal.userId());
    }
}
