package com.workflow.politicas.exception;

/**
 * Repositorio documental inexistente para un trámite (CU17).
 */
public class DocumentRepositoryNotFoundException extends RuntimeException {

    public DocumentRepositoryNotFoundException(String message) {
        super(message);
    }
}
