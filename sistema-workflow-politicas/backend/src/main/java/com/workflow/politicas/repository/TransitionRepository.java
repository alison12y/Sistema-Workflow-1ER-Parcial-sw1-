package com.workflow.politicas.repository;

import com.workflow.politicas.model.Transition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransitionRepository extends MongoRepository<Transition, String> {
}
