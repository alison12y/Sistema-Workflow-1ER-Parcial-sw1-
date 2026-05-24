package com.workflow.politicas.repository;

import com.workflow.politicas.model.AiRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiRequestRepository extends MongoRepository<AiRequest, String> {
}
