package com.workflow.politicas.repository;

import com.workflow.politicas.model.Tramite;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface TramiteRepository extends MongoRepository<Tramite, String> {
    long countByPolicyId(String policyId);

    List<Tramite> findByPolicyId(String policyId);

    List<Tramite> findByStatusIn(Collection<String> statuses);

    List<Tramite> findByStatusNot(String status);

    List<Tramite> findByPolicyIdAndStatusIn(String policyId, Collection<String> statuses);

    List<Tramite> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<Tramite> findByCreatedAtGreaterThanEqual(LocalDateTime from);
}
