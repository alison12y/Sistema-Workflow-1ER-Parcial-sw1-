package com.workflow.politicas.storage;

import java.util.Locale;

/**
 * Construye rutas S3 legibles: cada trámite (código TRM-xxx) es su repositorio documental.
 *
 * <pre>
 * workflow-docs/
 * └── TRM-007/
 *     ├── documento.pdf          (versión 1)
 *     ├── imagen.jpg             (versión 1)
 *     └── versiones/
 *         └── documento_v2.pdf   (versión 2+)
 * </pre>
 */
public final class DocumentStoragePathBuilder {

    public static final String VERSIONS_FOLDER = "versiones";

    private DocumentStoragePathBuilder() {
    }

    public static String buildObjectKey(String tramiteCodigo, String storageFileName, int version) {
        String root = normalizeTramiteCodigo(tramiteCodigo);
        String safeFileName = sanitizeFileName(storageFileName);
        if (version <= 1) {
            return root + "/" + safeFileName;
        }
        return root + "/" + VERSIONS_FOLDER + "/" + safeFileName;
    }

    public static String buildStorageFileName(String originalName, int version) {
        String sanitized = sanitizeFileName(originalName);
        if (version <= 1) {
            return sanitized;
        }
        return appendVersionSuffix(sanitized, version);
    }

    public static String normalizeTramiteCodigo(String tramiteCodigo) {
        if (tramiteCodigo == null || tramiteCodigo.isBlank()) {
            throw new IllegalArgumentException("El código de trámite es obligatorio para la ruta documental");
        }
        return tramiteCodigo.trim().toUpperCase(Locale.ROOT);
    }

    public static String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "documento.bin";
        }
        String normalized = originalFilename.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        String name = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String appendVersionSuffix(String fileName, int version) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return fileName + "_v" + version;
        }
        String base = fileName.substring(0, dot);
        String extension = fileName.substring(dot);
        return base + "_v" + version + extension;
    }
}
