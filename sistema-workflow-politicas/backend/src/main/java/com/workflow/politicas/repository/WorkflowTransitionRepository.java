package com.workflow.politicas.repository;

import com.workflow.politicas.model.WorkflowTransition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowTransitionRepository extends MongoRepository<WorkflowTransition, String> {
    List<WorkflowTransition> findByPolicyIdOrderByOrderIndexAsc(String policyId);

    List<WorkflowTransition> findByFromActivityId(String fromActivityId);

    List<WorkflowTransition> findByToActivityId(String toActivityId);

    long countByPolicyId(String policyId);

    boolean existsByPolicyIdAndFromActivityIdAndToActivityId(String policyId, String fromActivityId, String toActivityId);

    List<WorkflowTransition> findByPolicyIdAndFromActivityIdAndToActivityId(
            String policyId,
            String fromActivityId,
            String toActivityId
    );
}
