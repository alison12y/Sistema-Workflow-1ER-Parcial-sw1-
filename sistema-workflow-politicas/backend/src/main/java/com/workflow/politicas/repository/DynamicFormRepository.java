package com.workflow.politicas.repository;

import com.workflow.politicas.model.DynamicForm;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DynamicFormRepository extends MongoRepository<DynamicForm, String> {
    Optional<DynamicForm> findByPolicyIdAndActivityName(String policyId, String activityName);
}
