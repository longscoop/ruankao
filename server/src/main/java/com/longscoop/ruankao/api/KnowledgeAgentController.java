package com.longscoop.ruankao.api;

import com.longscoop.ruankao.ai.knowledge.KnowledgeChatService;
import com.longscoop.ruankao.ai.knowledge.KnowledgeService;
import com.longscoop.ruankao.auth.RuankaoPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import static com.longscoop.ruankao.ai.knowledge.KnowledgeModels.*;

@RestController
@RequestMapping("/api/v1/ai/agents")
public class KnowledgeAgentController {
    private final KnowledgeService knowledge;
    private final KnowledgeChatService chat;
    public KnowledgeAgentController(KnowledgeService knowledge, KnowledgeChatService chat) {
        this.knowledge = knowledge; this.chat = chat;
    }
    @GetMapping public List<PublicAgent> agents() { return knowledge.availableAgents(); }
    @PostMapping("/{id}/sessions") public Session start(@AuthenticationPrincipal RuankaoPrincipal principal, @PathVariable UUID id) {
        return chat.start(principal.userId(), id);
    }
    @GetMapping("/{id}/sessions") public List<Session> sessions(@AuthenticationPrincipal RuankaoPrincipal principal, @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "50") int limit) {
        return chat.sessions(principal.userId(), id, offset, limit);
    }
    @GetMapping("/sessions/{id}/turns") public List<Turn> history(@AuthenticationPrincipal RuankaoPrincipal principal, @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "50") int limit) {
        return chat.history(principal.userId(), id, offset, limit);
    }
    @PostMapping("/sessions/{id}/turns") public Turn ask(@AuthenticationPrincipal RuankaoPrincipal principal, @PathVariable UUID id, @RequestBody Ask input) {
        return chat.ask(principal.userId(), id, input.requestId(), input.message());
    }
    @GetMapping("/{id}/sources/{chunkId}") public Citation source(@PathVariable UUID id, @PathVariable long chunkId) {
        return knowledge.source(id, chunkId);
    }
    public record Ask(UUID requestId, String message) {}
}
