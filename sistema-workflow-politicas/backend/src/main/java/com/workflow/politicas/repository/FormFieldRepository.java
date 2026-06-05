package com.workflow.politicas.repository;

import com.workflow.politicas.model.FormField;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormFieldRepository extends MongoRepository<FormField, String> {
    List<FormField> findByFormId(String formId);

    List<FormField> findByFormIdOrderByOrderAsc(String formId);

    void deleteByFormId(String formId);
}
