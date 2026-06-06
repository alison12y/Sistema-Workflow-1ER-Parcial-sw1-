package com.workflow.politicas.repository;

import com.workflow.politicas.model.DocumentRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DocumentRepositoryStore extends MongoRepository<DocumentRepository, String> {

    Optional<DocumentRepository> findByTramiteId(String tramiteId);
}
