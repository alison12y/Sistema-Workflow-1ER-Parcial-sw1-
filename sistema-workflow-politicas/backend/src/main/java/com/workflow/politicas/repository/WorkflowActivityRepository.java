package com.workflow.politicas.repository;

import com.workflow.politicas.model.WorkflowActivity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowActivityRepository extends MongoRepository<WorkflowActivity, String> {
    List<WorkflowActivity> findByPolicyIdOrderByOrderIndexAsc(String policyId);

    long countByPolicyId(String policyId);

    List<WorkflowActivity> findByPolicyId(String policyId);
}
