package com.workflow.politicas.storage;

import java.util.Locale;
import java.util.Set;

/**
 * Extensiones admitidas explícitamente en CU18/CU19. Otras se almacenan como application/octet-stream.
 */
public final class DocumentMimeTypes {

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "png", "jpg", "jpeg", "txt", "csv", "zip", "rar", "mp4", "mp3"
    );

    private DocumentMimeTypes() {
    }

    public static String resolveContentType(String extension, String providedContentType) {
        if (providedContentType != null && !providedContentType.isBlank()) {
            return providedContentType;
        }
        String ext = extension != null ? extension.toLowerCase(Locale.ROOT) : "";
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "zip" -> "application/zip";
            case "rar" -> "application/vnd.rar";
            case "mp4" -> "video/mp4";
            case "mp3" -> "audio/mpeg";
            default -> "application/octet-stream";
        };
    }

    public static boolean isKnownExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        return ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }
}
