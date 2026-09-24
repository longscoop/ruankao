package com.longscoop.ruankao.ai.knowledge;

import java.util.UUID;

/** Private originals, never filesystem paths or unsigned public URLs. */
public interface KnowledgeFileStore {
    void put(UUID documentId, byte[] data);
    byte[] read(UUID documentId);
    void delete(UUID documentId);
}
