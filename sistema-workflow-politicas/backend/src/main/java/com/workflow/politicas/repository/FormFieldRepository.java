package com.workflow.politicas.repository;

import com.workflow.politicas.model.FormField;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormFieldRepository extends MongoRepository<FormField, String> {
    java.util.List<FormField> findByFormId(String formId);
    void deleteByFormId(String formId);
}
