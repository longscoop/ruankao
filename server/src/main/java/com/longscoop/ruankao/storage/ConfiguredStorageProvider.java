package com.longscoop.ruankao.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

@Component
@ConditionalOnMissingBean(StorageProvider.class)
public class ConfiguredStorageProvider implements StorageProvider {

    private final String publicBaseUrl;

    public ConfiguredStorageProvider(
            @Value("${STORAGE_PUBLIC_BASE_URL:}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
    }

    @Override
    public StoredObject upload(StorageUploadRequest request) {
        throw new UnsupportedOperationException(
                "upload is not configured for the learner storage provider");
    }

    @Override
    public void delete(String objectKey) {
        throw new UnsupportedOperationException(
                "delete is not configured for the learner storage provider");
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
                    "STORAGE_PUBLIC_BASE_URL must be configured for relative video object keys");
        }
        String normalized = key.startsWith("/") ? key.substring(1) : key;
        return UriComponentsBuilder.fromUriString(publicBaseUrl)
                .path("/")
                .path(normalized)
                .build()
                .encode()
                .toUri();
    }
}
