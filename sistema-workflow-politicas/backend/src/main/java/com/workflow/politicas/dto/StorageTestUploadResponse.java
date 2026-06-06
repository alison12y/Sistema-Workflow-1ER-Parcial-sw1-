package com.workflow.politicas.dto;

public class StorageTestUploadResponse {

    private String key;
    private String bucket;
    private String provider;
    private String contentType;
    private long size;
    private String etag;
    private String presignedDownloadUrl;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getPresignedDownloadUrl() {
        return presignedDownloadUrl;
    }

    public void setPresignedDownloadUrl(String presignedDownloadUrl) {
        this.presignedDownloadUrl = presignedDownloadUrl;
    }
}
