package com.workflow.politicas.repository;

import com.workflow.politicas.model.Tramite;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TramiteRepository extends MongoRepository<Tramite, String> {
}
