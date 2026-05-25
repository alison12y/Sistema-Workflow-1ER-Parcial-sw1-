package com.workflow.politicas.repository;

import com.workflow.politicas.model.TaskInstance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskInstanceRepository extends MongoRepository<TaskInstance, String> {
    java.util.List<TaskInstance> findByAssignedUserId(String assignedUserId);
    java.util.List<TaskInstance> findByAssignedRoleId(String assignedRoleId);
    java.util.List<TaskInstance> findByProcessInstanceId(String processInstanceId);
}
