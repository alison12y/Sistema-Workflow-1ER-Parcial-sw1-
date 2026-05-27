package com.workflow.politicas.repository;

import com.workflow.politicas.model.ActivityDiagram;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActivityDiagramRepository extends MongoRepository<ActivityDiagram, String> {
    Optional<ActivityDiagram> findByPolicyId(String policyId);
}
