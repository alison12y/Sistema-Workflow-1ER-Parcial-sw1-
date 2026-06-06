package com.workflow.politicas.storage;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

/**
 * Abstracción de almacenamiento de objetos (S3 / MinIO).
 */
public interface StorageService {

    StoredObject upload(
            String key,
            InputStream inputStream,
            long contentLength,
            String contentType,
            Map<String, String> metadata
    );

    StoredObject download(String key);

    void delete(String key);

    boolean exists(String key);

    URL generatePresignedDownloadUrl(String key, Duration expiration);

    String getProviderName();
}
