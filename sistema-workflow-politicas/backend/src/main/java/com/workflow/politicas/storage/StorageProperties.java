package com.workflow.politicas.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String bucket = "workflow-docs";
    private String region = "us-east-1";
    private String accessKeyId;
    private String secretAccessKey;
    private String endpoint = "";
    private boolean pathStyleAccess = false;
    private String providerName = "AWS_S3";
    private int presignedUrlExpirationMinutes = 15;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public boolean isPathStyleAccess() {
        return pathStyleAccess;
    }

    public void setPathStyleAccess(boolean pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public int getPresignedUrlExpirationMinutes() {
        return presignedUrlExpirationMinutes;
    }

    public void setPresignedUrlExpirationMinutes(int presignedUrlExpirationMinutes) {
        this.presignedUrlExpirationMinutes = presignedUrlExpirationMinutes;
    }

    public boolean hasCustomEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }
}
