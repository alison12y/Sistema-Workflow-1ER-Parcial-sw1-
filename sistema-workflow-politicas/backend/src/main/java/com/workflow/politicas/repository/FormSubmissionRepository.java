package com.workflow.politicas.repository;

import com.workflow.politicas.model.FormSubmission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormSubmissionRepository extends MongoRepository<FormSubmission, String> {
    List<FormSubmission> findByTramiteId(String tramiteId);

    Optional<FormSubmission> findByTramiteIdAndActivityNameAndTaskOrder(
            String tramiteId,
            String activityName,
            int taskOrder
    );
}
