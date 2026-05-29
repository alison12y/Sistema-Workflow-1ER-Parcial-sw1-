package com.workflow.politicas.repository;

import com.workflow.politicas.model.Tramite;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TramiteRepository extends MongoRepository<Tramite, String> {
    long countByPolicyId(String policyId);

    List<Tramite> findByPolicyId(String policyId);
}
