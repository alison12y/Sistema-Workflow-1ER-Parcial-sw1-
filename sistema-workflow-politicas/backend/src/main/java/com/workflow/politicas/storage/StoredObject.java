package com.workflow.politicas.storage;

import java.io.InputStream;

/**
 * Representa un objeto almacenado en S3 (metadatos y, opcionalmente, contenido para descarga).
 */
public class StoredObject {

    private final String key;
    private final String bucket;
    private final String contentType;
    private final long contentLength;
    private final String etag;
    private final InputStream inputStream;

    public StoredObject(
            String key,
            String bucket,
            String contentType,
            long contentLength,
            String etag,
            InputStream inputStream
    ) {
        this.key = key;
        this.bucket = bucket;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.etag = etag;
        this.inputStream = inputStream;
    }

    public static StoredObject metadataOnly(
            String key,
            String bucket,
            String contentType,
            long contentLength,
            String etag
    ) {
        return new StoredObject(key, bucket, contentType, contentLength, etag, null);
    }

    public String getKey() {
        return key;
    }

    public String getBucket() {
        return bucket;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getEtag() {
        return etag;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public boolean hasContent() {
        return inputStream != null;
    }
}
