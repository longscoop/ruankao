package com.longscoop.ruankao.ai.knowledge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class LocalKnowledgeFileStoreTest {
    @TempDir Path root;

    @Test
    void persistsOriginalBytesAcrossInstancesAndDeletesOnlyThatDocument() {
        var id = UUID.randomUUID();
        var bytes = "授权的软考资料".getBytes(StandardCharsets.UTF_8);
        new LocalKnowledgeFileStore(root.toString()).put(id, bytes);
        var restarted = new LocalKnowledgeFileStore(root.toString());
        assertThat(restarted.read(id)).isEqualTo(bytes);
        restarted.delete(id);
        assertThat(root.resolve(id + ".bin")).doesNotExist();
    }
}
