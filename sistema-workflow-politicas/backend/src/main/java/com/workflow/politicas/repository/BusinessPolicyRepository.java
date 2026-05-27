package com.workflow.politicas.repository;

import com.workflow.politicas.model.BusinessPolicy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessPolicyRepository extends MongoRepository<BusinessPolicy, String> {
    List<BusinessPolicy> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name, String description);
}
