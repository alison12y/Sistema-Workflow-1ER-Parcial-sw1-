package com.workflow.politicas.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Crea el bucket en MinIO/local si no existe (solo perfiles de desarrollo).
 */
@Component
@Profile({"local", "dev"})
public class S3BucketInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(S3BucketInitializer.class);

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public S3BucketInitializer(S3Client s3Client, StorageProperties storageProperties) {
        this.s3Client = s3Client;
        this.storageProperties = storageProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String bucket = storageProperties.getBucket();
        if (bucketExists(bucket)) {
            log.info("Bucket S3 listo: {} (proveedor: {})", bucket, storageProperties.getProviderName());
            return;
        }

        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Bucket S3 creado: {} (proveedor: {})", bucket, storageProperties.getProviderName());
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException exception) {
            log.info("Bucket S3 ya existía: {}", bucket);
        } catch (S3Exception exception) {
            log.warn("No se pudo crear el bucket {} automáticamente: {}", bucket, exception.getMessage());
        }
    }

    private boolean bucketExists(String bucket) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        } catch (NoSuchBucketException exception) {
            return false;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            log.warn("No se pudo verificar el bucket {}: {}", bucket, exception.getMessage());
            return false;
        }
    }
}
