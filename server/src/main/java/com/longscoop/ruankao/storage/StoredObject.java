package com.longscoop.ruankao.storage;

public record StoredObject(String objectKey, String contentType, long contentLength) {

    public StoredObject {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            throw new IllegalArgumentException("objectKey is required");
        }
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("contentType is required");
        }
        if (contentLength < 0) {
            throw new IllegalArgumentException("contentLength cannot be negative");
        }
        objectKey = objectKey.trim();
        contentType = contentType.trim();
    }
}
