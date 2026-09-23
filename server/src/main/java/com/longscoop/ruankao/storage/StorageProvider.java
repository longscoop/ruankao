package com.longscoop.ruankao.storage;

import java.net.URI;
import java.time.Duration;

public interface StorageProvider {

    StoredObject upload(StorageUploadRequest request);

    void delete(String objectKey);

    URI generateAccessUrl(String objectKey, Duration ttl);
}
