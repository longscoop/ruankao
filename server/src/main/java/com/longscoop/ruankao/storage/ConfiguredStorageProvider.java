package com.longscoop.ruankao.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

@Component
public class ConfiguredStorageProvider implements StorageProvider {

    private final String publicBaseUrl;
    private final Path localRoot;

    public ConfiguredStorageProvider(
            @Value("${STORAGE_PUBLIC_BASE_URL:}") String publicBaseUrl,
            @Value("${STORAGE_LOCAL_ROOT:}") String localRoot) {
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        this.localRoot = localRoot == null || localRoot.isBlank()
                ? null
                : Path.of(localRoot).toAbsolutePath().normalize();
    }

    @Override
    public StoredObject upload(StorageUploadRequest request) {
        if (localRoot == null) {
            throw new UnsupportedOperationException(
                    "STORAGE_LOCAL_ROOT must be configured or replace StorageProvider with an object-storage implementation");
        }
        Path target = resolveLocal(request.objectKey());
        try {
            Files.createDirectories(target.getParent());
            Files.copy(request.content(), target, StandardCopyOption.REPLACE_EXISTING);
            long actualLength = Files.size(target);
            if (actualLength != request.contentLength()) {
                Files.deleteIfExists(target);
                throw new IllegalStateException(
                        "uploaded content length mismatch: expected "
                                + request.contentLength() + " but was " + actualLength);
            }
            return new StoredObject(
                    request.objectKey(),
                    request.contentType(),
                    actualLength);
        } catch (IOException e) {
            throw new IllegalStateException("failed to store object " + request.objectKey(), e);
        }
    }

    @Override
    public void delete(String objectKey) {
        if (localRoot == null) {
            throw new UnsupportedOperationException(
                    "STORAGE_LOCAL_ROOT must be configured or replace StorageProvider with an object-storage implementation");
        }
        try {
            Files.deleteIfExists(resolveLocal(objectKey));
        } catch (IOException e) {
            throw new IllegalStateException("failed to delete object " + objectKey, e);
        }
    }

    @Override
    public URI generateAccessUrl(String objectKey, Duration ttl) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            throw new IllegalArgumentException("objectKey is required");
        }
        String key = objectKey.trim();
        if (key.startsWith("https://") || key.startsWith("http://")) {
            return URI.create(key);
        }
        if (publicBaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "STORAGE_PUBLIC_BASE_URL must be configured for relative object keys");
        }
        String normalized = key.startsWith("/") ? key.substring(1) : key;
        return UriComponentsBuilder.fromUriString(publicBaseUrl)
                .path("/")
                .path(normalized)
                .build()
                .encode()
                .toUri();
    }

    private Path resolveLocal(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey is required");
        }
        String normalizedKey = objectKey.replace('\\', '/');
        if (normalizedKey.startsWith("/") || normalizedKey.contains("../") || normalizedKey.equals("..")) {
            throw new IllegalArgumentException("objectKey contains an unsafe path");
        }
        Path target = localRoot.resolve(normalizedKey).normalize();
        if (!target.startsWith(localRoot)) {
            throw new IllegalArgumentException("objectKey escapes storage root");
        }
        return target;
    }
}
