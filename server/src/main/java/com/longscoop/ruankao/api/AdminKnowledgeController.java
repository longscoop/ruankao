package com.longscoop.ruankao.api;

import com.longscoop.ruankao.ai.knowledge.KnowledgeService;
import com.longscoop.ruankao.auth.RuankaoPrincipal;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import static com.longscoop.ruankao.ai.knowledge.KnowledgeModels.*;

@RestController
@RequestMapping("/api/v1/admin/knowledge")
public class AdminKnowledgeController {
    private final KnowledgeService knowledge;
    public AdminKnowledgeController(KnowledgeService knowledge) { this.knowledge = knowledge; }
    @GetMapping("/bases") public List<Base> bases() { return knowledge.bases(); }
    @PostMapping("/bases") public Base createBase(@RequestBody BaseInput input) { return knowledge.createBase(input); }
    @PutMapping("/bases/{id}") public Base updateBase(@PathVariable UUID id, @RequestBody BaseInput input) { return knowledge.updateBase(id, input); }
    @DeleteMapping("/bases/{id}") public ResponseEntity<Void> deleteBase(@PathVariable UUID id) {
        knowledge.deleteBase(id); return ResponseEntity.noContent().build();
    }
    @GetMapping("/bases/{id}/documents") public List<Document> documents(@PathVariable UUID id,
            @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "50") int limit) {
        return knowledge.documents(id, offset, limit);
    }
    @PostMapping(value = "/bases/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Document upload(@PathVariable UUID id, @RequestPart("file") MultipartFile file,
                           @AuthenticationPrincipal RuankaoPrincipal principal) {
        try { return knowledge.upload(id, file.getOriginalFilename(), file.getBytes(), principal.userId()); }
        catch (IOException e) { throw new IllegalArgumentException("文件读取失败", e); }
    }
    @GetMapping("/documents/{id}") public Document document(@PathVariable UUID id) { return knowledge.document(id); }
    @GetMapping("/documents/{id}/chunks") public List<ChunkView> chunks(@PathVariable UUID id,
            @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "50") int limit) {
        return knowledge.chunks(id, offset, limit);
    }
    @PutMapping("/documents/{id}/publication") public Document publish(@PathVariable UUID id, @RequestBody Publication input) {
        return knowledge.setPublished(id, input.published());
    }
    @DeleteMapping("/documents/{id}") public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        knowledge.deleteDocument(id); return ResponseEntity.noContent().build();
    }
    @GetMapping("/documents/{id}/original") public ResponseEntity<byte[]> original(@PathVariable UUID id) {
        Document document = knowledge.document(id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(document.filename(), StandardCharsets.UTF_8).build().toString())
                .body(knowledge.original(id));
    }
    @GetMapping("/agents") public List<Agent> agents() { return knowledge.agents(); }
    @PostMapping("/agents") public Agent createAgent(@RequestBody AgentInput input) { return knowledge.createAgent(input); }
    @PutMapping("/agents/{id}") public Agent updateAgent(@PathVariable UUID id, @RequestBody AgentInput input) { return knowledge.updateAgent(id, input); }
    @DeleteMapping("/agents/{id}") public ResponseEntity<Void> deleteAgent(@PathVariable UUID id) {
        knowledge.deleteAgent(id); return ResponseEntity.noContent().build();
    }
    public record Publication(boolean published) {}
}
