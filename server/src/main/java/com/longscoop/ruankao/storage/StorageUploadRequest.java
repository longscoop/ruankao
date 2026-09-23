package com.longscoop.ruankao.storage;

import java.io.InputStream;

public record StorageUploadRequest(
        String objectKey,
        String contentType,
        long contentLength,
        InputStream content) {

    public StorageUploadRequest {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            throw new IllegalArgumentException("objectKey is required");
        }
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("contentType is required");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException("contentLength cannot be negative");
        }
        if (content == null) {
            throw new IllegalArgumentException("content is required");
        }
        objectKey = objectKey.trim();
        contentType = contentType.trim();
    }
}
