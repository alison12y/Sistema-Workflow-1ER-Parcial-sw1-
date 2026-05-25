package com.workflow.politicas.repository;

import com.workflow.politicas.model.WorkflowDiagram;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowDiagramRepository extends MongoRepository<WorkflowDiagram, String> {
    java.util.List<WorkflowDiagram> findByPolicyId(String policyId);
}
