package com.workflow.politicas.repository;

import com.workflow.politicas.model.BusinessPolicy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessPolicyRepository extends MongoRepository<BusinessPolicy, String> {
}
