package com.longscoop.ruankao.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguredStorageProviderTest {

    @TempDir
    Path root;

    @Test
    void localRootSupportsUploadPreviewAndDelete() throws Exception {
        ConfiguredStorageProvider provider =
                new ConfiguredStorageProvider("https://assets.example.test", root.toString());

        StoredObject stored = provider.upload(new StorageUploadRequest(
                "content-imports/batch/pages/1.png",
                "image/png",
                3,
                new ByteArrayInputStream(new byte[]{1,2,3})));

        assertArrayEquals(new byte[]{1,2,3},
                Files.readAllBytes(root.resolve("content-imports/batch/pages/1.png")));
        assertEquals(
                "https://assets.example.test/content-imports/batch/pages/1.png",
                provider.generateAccessUrl(stored.objectKey(), Duration.ofMinutes(5)).toString());

        provider.delete(stored.objectKey());
        assertFalse(Files.exists(root.resolve("content-imports/batch/pages/1.png")));
    }

    @Test
    void rejectsPathTraversal() {
        ConfiguredStorageProvider provider =
                new ConfiguredStorageProvider("https://assets.example.test", root.toString());

        assertThrows(IllegalArgumentException.class, () ->
                provider.upload(new StorageUploadRequest(
                        "../escape.pdf", "application/pdf", 1,
                        new ByteArrayInputStream(new byte[]{1}))));
    }
}
