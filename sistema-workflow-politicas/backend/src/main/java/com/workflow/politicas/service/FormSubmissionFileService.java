package com.workflow.politicas.service;

import com.workflow.politicas.dto.FormSubmissionFileResponse;
import com.workflow.politicas.model.FormSubmissionFile;
import com.workflow.politicas.repository.FormSubmissionFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class FormSubmissionFileService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    private final FormSubmissionFileRepository formSubmissionFileRepository;
    private final Path uploadRoot;

    public FormSubmissionFileService(
            FormSubmissionFileRepository formSubmissionFileRepository,
            @Value("${app.uploads.form-submissions-dir:uploads/form-submissions}") String uploadDir
    ) {
        this.formSubmissionFileRepository = formSubmissionFileRepository;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear la carpeta de archivos adjuntos", exception);
        }
    }

    public FormSubmissionFileResponse store(MultipartFile file, String username) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un archivo");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("El archivo no puede superar 10 MB");
        }

        String originalFileName = sanitizeOriginalName(file.getOriginalFilename());
        String storageFileName = UUID.randomUUID() + "_" + originalFileName;
        Path destination = uploadRoot.resolve(storageFileName).normalize();

        if (!destination.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Nombre de archivo no válido");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalArgumentException("No se pudo guardar el archivo adjunto", exception);
        }

        FormSubmissionFile entity = new FormSubmissionFile();
        entity.setOriginalFileName(originalFileName);
        entity.setContentType(resolveContentType(file, originalFileName));
        entity.setSize(file.getSize());
        entity.setStorageFileName(storageFileName);
        entity.setUploadedBy(username);
        entity.setCreatedAt(LocalDateTime.now());

        FormSubmissionFile saved = formSubmissionFileRepository.save(entity);
        return toResponse(saved);
    }

    public StoredFile load(String fileId) {
        FormSubmissionFile metadata = formSubmissionFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado"));

        Path path = uploadRoot.resolve(metadata.getStorageFileName()).normalize();
        if (!path.startsWith(uploadRoot) || !Files.exists(path)) {
            throw new IllegalArgumentException("Archivo no disponible");
        }

        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("Archivo no disponible");
            }
            return new StoredFile(resource, metadata);
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Archivo no disponible", exception);
        }
    }

    public boolean exists(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return false;
        }
        return formSubmissionFileRepository.findById(fileId.trim())
                .map(metadata -> Files.exists(uploadRoot.resolve(metadata.getStorageFileName()).normalize()))
                .orElse(false);
    }

    public void deleteIfExists(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return;
        }
        formSubmissionFileRepository.findById(fileId.trim()).ifPresent(metadata -> {
            Path path = uploadRoot.resolve(metadata.getStorageFileName()).normalize();
            if (path.startsWith(uploadRoot)) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // El registro en BD se elimina aunque falle el archivo en disco
                }
            }
            formSubmissionFileRepository.deleteById(metadata.getId());
        });
    }

    private FormSubmissionFileResponse toResponse(FormSubmissionFile file) {
        FormSubmissionFileResponse response = new FormSubmissionFileResponse();
        response.setFileId(file.getId());
        response.setFileName(file.getOriginalFileName());
        response.setContentType(file.getContentType());
        response.setSize(file.getSize());
        return response;
    }

    private String sanitizeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "archivo";
        }

        String normalized = Paths.get(originalName).getFileName().toString().trim();
        normalized = normalized.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (normalized.isBlank()) {
            return "archivo";
        }
        return normalized;
    }

    private String resolveContentType(MultipartFile file, String originalFileName) {
        if (file.getContentType() != null && !file.getContentType().isBlank()) {
            return file.getContentType();
        }

        String lowerName = originalFileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lowerName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerName.endsWith(".doc")) {
            return "application/msword";
        }
        if (lowerName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "application/octet-stream";
    }

    public record StoredFile(Resource resource, FormSubmissionFile metadata) {
    }
}
