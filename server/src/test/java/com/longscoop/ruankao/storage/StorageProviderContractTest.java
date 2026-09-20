package com.longscoop.ruankao.storage;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StorageProviderContractTest {

    @Test
    void providerReturnsPersistentObjectKeyAndGeneratesTransientAccessUrl() {
        StorageProvider provider = new FakeStorageProvider();
        StorageUploadRequest request = new StorageUploadRequest(
                "  videos/architecture.mp4  ", "video/mp4", 3,
                new ByteArrayInputStream(new byte[] {1, 2, 3}));

        StoredObject stored = provider.upload(request);
        URI accessUrl = provider.generateAccessUrl(stored.objectKey(), Duration.ofMinutes(5));
        provider.delete(stored.objectKey());

        assertEquals("videos/architecture.mp4", stored.objectKey());
        assertEquals(3, stored.contentLength());
        assertEquals("https://storage.invalid/videos/architecture.mp4?expires=300", accessUrl.toString());
        assertFalse(((FakeStorageProvider) provider).contains(stored.objectKey()));
    }

    private static final class FakeStorageProvider implements StorageProvider {
        private final Map<String, StoredObject> objects = new HashMap<>();

        @Override
        public StoredObject upload(StorageUploadRequest request) {
            StoredObject stored = new StoredObject(request.objectKey(), request.contentType(), request.contentLength());
            objects.put(stored.objectKey(), stored);
            return stored;
        }

        @Override
        public void delete(String objectKey) {
            objects.remove(objectKey);
        }

        @Override
        public URI generateAccessUrl(String objectKey, Duration ttl) {
            return URI.create("https://storage.invalid/" + objectKey + "?expires=" + ttl.toSeconds());
        }

        boolean contains(String objectKey) {
            return objects.containsKey(objectKey);
        }
    }
}
