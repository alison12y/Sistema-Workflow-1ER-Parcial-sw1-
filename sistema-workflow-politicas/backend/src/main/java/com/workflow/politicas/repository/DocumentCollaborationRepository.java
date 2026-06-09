package com.workflow.politicas.repository;

import com.workflow.politicas.model.DocumentCollaborationMeta;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentCollaborationRepository extends MongoRepository<DocumentCollaborationMeta, String> {
}
