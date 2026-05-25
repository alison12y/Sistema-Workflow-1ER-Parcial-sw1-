package com.workflow.politicas.repository;

import com.workflow.politicas.model.ProcessInstance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessInstanceRepository extends MongoRepository<ProcessInstance, String> {
    java.util.List<ProcessInstance> findByStatus(String status);
}
