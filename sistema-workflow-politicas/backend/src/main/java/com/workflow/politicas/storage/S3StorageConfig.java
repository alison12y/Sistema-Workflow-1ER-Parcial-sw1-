package com.workflow.politicas.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3StorageConfig {

    @Bean
    public S3Client s3Client(StorageProperties storageProperties) {
        validateCredentials(storageProperties);
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(storageProperties.getRegion()))
                .credentialsProvider(credentialsProvider(storageProperties));

        applyEndpoint(builder, storageProperties);
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(StorageProperties storageProperties) {
        validateCredentials(storageProperties);
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(storageProperties.getRegion()))
                .credentialsProvider(credentialsProvider(storageProperties));

        if (storageProperties.hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(storageProperties.getEndpoint().trim()));
            builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(storageProperties.isPathStyleAccess())
                    .build());
        }

        return builder.build();
    }

    private StaticCredentialsProvider credentialsProvider(StorageProperties storageProperties) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                storageProperties.getAccessKeyId(),
                storageProperties.getSecretAccessKey()
        ));
    }

    private void applyEndpoint(S3ClientBuilder builder, StorageProperties storageProperties) {
        if (storageProperties.hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(storageProperties.getEndpoint().trim()));
            builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(storageProperties.isPathStyleAccess())
                    .build());
        }
    }

    private void validateCredentials(StorageProperties storageProperties) {
        if (storageProperties.getAccessKeyId() == null || storageProperties.getAccessKeyId().isBlank()) {
            throw new IllegalStateException(
                    "app.storage.access-key-id no configurado. Revise el perfil Spring activo y las variables de entorno."
            );
        }
        if (storageProperties.getSecretAccessKey() == null || storageProperties.getSecretAccessKey().isBlank()) {
            throw new IllegalStateException(
                    "app.storage.secret-access-key no configurado. Revise el perfil Spring activo y las variables de entorno."
            );
        }
    }
}
