package com.workflow.politicas.repository;

import com.workflow.politicas.model.FormSubmissionFile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormSubmissionFileRepository extends MongoRepository<FormSubmissionFile, String> {
}
