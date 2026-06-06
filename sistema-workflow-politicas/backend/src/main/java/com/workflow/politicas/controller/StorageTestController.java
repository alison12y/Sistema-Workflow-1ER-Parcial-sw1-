package com.workflow.politicas.controller;

import com.workflow.politicas.dto.StorageTestUploadResponse;
import com.workflow.politicas.storage.StorageService;
import com.workflow.politicas.storage.StoredObject;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints temporales para validar la capa S3/MinIO (Fase 2.1).
 * Serán reemplazados por el módulo documental en CU17–CU18.
 */
@RestController
@RequestMapping("/api/storage")
public class StorageTestController {

    private static final String TEST_KEY_PREFIX = "test/";

    private final StorageService storageService;

    public StorageTestController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/test-upload")
    public StorageTestUploadResponse testUpload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String key,
            Authentication authentication
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Debe enviar un archivo en el campo 'file'");
        }

        String resolvedKey = resolveUploadKey(key, file.getOriginalFilename());
        String username = resolveUsername(authentication);

        try {
            StoredObject stored = storageService.upload(
                    resolvedKey,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType(),
                    Map.of("uploaded-by", username, "source", "storage-test")
            );

            StorageTestUploadResponse response = new StorageTestUploadResponse();
            response.setKey(stored.getKey());
            response.setBucket(stored.getBucket());
            response.setProvider(storageService.getProviderName());
            response.setContentType(stored.getContentType());
            response.setSize(stored.getContentLength());
            response.setEtag(stored.getEtag());
            response.setPresignedDownloadUrl(
                    storageService.generatePresignedDownloadUrl(stored.getKey(), Duration.ofMinutes(15)).toString()
            );
            return response;
        } catch (Exception exception) {
            throw new IllegalArgumentException("No se pudo subir el archivo de prueba: " + exception.getMessage(), exception);
        }
    }

    @GetMapping("/test-download/**")
    public ResponseEntity<InputStreamResource> testDownload(HttpServletRequest request) {
        String key = extractObjectKey(request);
        StoredObject stored = storageService.download(key);

        if (!stored.hasContent()) {
            throw new IllegalStateException("El objeto no tiene contenido descargable");
        }

        String fileName = extractFileName(stored.getKey());
        String encodedFileName = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType = MediaType.parseMediaType(stored.getContentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(stored.getContentLength())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName
                )
                .body(new InputStreamResource(stored.getInputStream()));
    }

    private String resolveUploadKey(String requestedKey, String originalFilename) {
        if (requestedKey != null && !requestedKey.isBlank()) {
            String normalized = requestedKey.trim();
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            return normalized;
        }

        String safeName = sanitizeFileName(originalFilename);
        return TEST_KEY_PREFIX + UUID.randomUUID() + "_" + safeName;
    }

    private String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "archivo.bin";
        }
        String normalized = originalFilename.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        String name = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractObjectKey(HttpServletRequest request) {
        String prefix = request.getContextPath() + "/api/storage/test-download/";
        String uri = request.getRequestURI();
        if (!uri.startsWith(prefix)) {
            throw new IllegalArgumentException("Ruta de descarga no válida");
        }
        String encodedKey = uri.substring(prefix.length());
        if (encodedKey.isBlank()) {
            throw new IllegalArgumentException("Debe indicar la clave del objeto (key)");
        }
        return URLDecoder.decode(encodedKey, StandardCharsets.UTF_8);
    }

    private String extractFileName(String key) {
        int lastSlash = key.lastIndexOf('/');
        String segment = lastSlash >= 0 ? key.substring(lastSlash + 1) : key;
        int underscore = segment.indexOf('_');
        if (underscore >= 0 && underscore < segment.length() - 1) {
            return segment.substring(underscore + 1);
        }
        return segment;
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "anonymous";
        }
        return authentication.getName();
    }
}
