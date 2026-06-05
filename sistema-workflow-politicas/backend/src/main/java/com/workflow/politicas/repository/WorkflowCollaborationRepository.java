package com.workflow.politicas.repository;

import com.workflow.politicas.model.WorkflowCollaborationMeta;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WorkflowCollaborationRepository extends MongoRepository<WorkflowCollaborationMeta, String> {
}
