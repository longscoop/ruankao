package com.longscoop.ruankao.ai.knowledge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.UUID;

@Component
public class LocalKnowledgeFileStore implements KnowledgeFileStore {
    private final Path root;

    public LocalKnowledgeFileStore(
            @Value("${KNOWLEDGE_STORAGE_ROOT:./data/knowledge}") String root) {
        if (root == null || root.isBlank()) throw new IllegalArgumentException("知识库文件目录未配置");
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public void put(UUID documentId, byte[] data) {
        Objects.requireNonNull(documentId, "documentId");
        if (data == null || data.length == 0 || data.length > MaterialExtractor.MAX_BYTES) {
            throw new IllegalArgumentException("原始文件大小无效");
        }
        Path temporary = null;
        try {
            Files.createDirectories(root);
            temporary = Files.createTempFile(root, ".upload-", ".part");
            if (Files.getFileAttributeView(temporary, PosixFileAttributeView.class) != null) {
                Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-------"));
            }
            Files.write(temporary, data);
            Files.move(temporary, path(documentId));
        } catch (IOException e) {
            throw new IllegalStateException("保存资料文件失败，请检查知识库存储配置", e);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* Preserve original error. */ }
            }
        }
    }

    @Override
    public byte[] read(UUID documentId) {
        try {
            Path path = path(documentId);
            if (Files.size(path) > MaterialExtractor.MAX_BYTES) {
                throw new IllegalStateException("原始文件大小无效");
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new IllegalStateException("原始资料文件暂不可用", e);
        }
    }

    @Override
    public void delete(UUID documentId) {
        try {
            Files.deleteIfExists(path(documentId));
        } catch (IOException e) {
            throw new IllegalStateException("清理资料文件失败", e);
        }
    }

    private Path path(UUID documentId) {
        return root.resolve(Objects.requireNonNull(documentId, "documentId") + ".bin");
    }
}
