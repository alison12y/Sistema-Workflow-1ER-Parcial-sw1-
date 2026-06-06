package com.workflow.politicas.storage;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@Service
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner, StorageProperties storageProperties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.storageProperties = storageProperties;
    }

    @Override
    public StoredObject upload(
            String key,
            InputStream inputStream,
            long contentLength,
            String contentType,
            Map<String, String> metadata
    ) {
        validateKey(key);
        if (inputStream == null) {
            throw new IllegalArgumentException("El stream del archivo no puede ser nulo");
        }
        if (contentLength <= 0) {
            throw new IllegalArgumentException("El tamaño del archivo debe ser mayor a cero");
        }

        String resolvedContentType = contentType != null && !contentType.isBlank()
                ? contentType
                : "application/octet-stream";

        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(key)
                .contentType(resolvedContentType)
                .contentLength(contentLength);

        Map<String, String> safeMetadata = metadata != null ? metadata : Collections.emptyMap();
        if (!safeMetadata.isEmpty()) {
            requestBuilder.metadata(safeMetadata);
        }

        try {
            s3Client.putObject(requestBuilder.build(), RequestBody.fromInputStream(inputStream, contentLength));
        } catch (S3Exception exception) {
            throw new IllegalStateException("No se pudo subir el objeto a S3: " + exception.getMessage(), exception);
        }

        return headMetadata(key);
    }

    @Override
    public StoredObject download(String key) {
        validateKey(key);
        try {
            var response = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(key)
                    .build());

            String contentType = response.response().contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            return new StoredObject(
                    key,
                    storageProperties.getBucket(),
                    contentType,
                    response.response().contentLength() != null ? response.response().contentLength() : 0L,
                    response.response().eTag(),
                    response
            );
        } catch (NoSuchKeyException exception) {
            throw new IllegalArgumentException("Objeto no encontrado: " + key);
        } catch (S3Exception exception) {
            throw new IllegalStateException("No se pudo descargar el objeto desde S3: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(key)
                    .build());
        } catch (S3Exception exception) {
            throw new IllegalStateException("No se pudo eliminar el objeto en S3: " + exception.getMessage(), exception);
        }
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException exception) {
            return false;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw new IllegalStateException("No se pudo verificar el objeto en S3: " + exception.getMessage(), exception);
        }
    }

    @Override
    public URL generatePresignedDownloadUrl(String key, Duration expiration) {
        validateKey(key);
        Duration effectiveExpiration = expiration != null
                ? expiration
                : Duration.ofMinutes(storageProperties.getPresignedUrlExpirationMinutes());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(effectiveExpiration)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url();
    }

    @Override
    public String getProviderName() {
        return storageProperties.getProviderName();
    }

    private StoredObject headMetadata(String key) {
        var head = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(key)
                .build());

        String contentType = head.contentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return StoredObject.metadataOnly(
                key,
                storageProperties.getBucket(),
                contentType,
                head.contentLength() != null ? head.contentLength() : 0L,
                head.eTag()
        );
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("La clave del objeto (key) es obligatoria");
        }
        if (key.startsWith("/")) {
            throw new IllegalArgumentException("La clave del objeto no debe comenzar con /");
        }
    }
}
