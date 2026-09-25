package com.longscoop.ruankao.ai.knowledge;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** API DTOs; no filesystem keys, credentials or persistence entities are exposed. */
public final class KnowledgeModels {
    private KnowledgeModels() {}
    public enum DocumentStatus { REVIEW, PUBLISHED }
    public enum AnswerStatus { GROUNDED, EXTRACT_ONLY, NO_EVIDENCE }
    public record BaseInput(long examId, String name, String description) {}
    public record Base(UUID id, long examId, String name, String description) {}
    public record Document(UUID id, UUID baseId, String filename, String sha256,
                           int pageCount, int chunkCount, DocumentStatus status,
                           List<String> warnings, OffsetDateTime createdAt) {}
    public record ChunkView(long id, UUID documentId, int page, int ordinal, String text) {}
    public record AgentInput(String name, String description, String instructions,
                             List<UUID> baseIds, boolean enabled) {}
    public record Agent(UUID id, String name, String description, String instructions,
                        boolean enabled, List<UUID> baseIds) {}
    public record PublicAgent(UUID id, String name, String description) {}
    public record Session(UUID id, UUID agentId, String title,
                          OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
    public record Citation(int number, UUID documentId, long chunkId, String filename,
                           int page, String text) {}
    public record Turn(long id, UUID requestId, String question, String content,
                       AnswerStatus status, List<Citation> citations, OffsetDateTime createdAt) {}
}
