package com.workflow.politicas.repository;

import com.workflow.politicas.model.Bitacora;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BitacoraRepository extends MongoRepository<Bitacora, String> {
    List<Bitacora> findByModule(String module);
    List<Bitacora> findByUserId(String userId);
    void deleteByEntityTypeAndEntityId(String entityType, String entityId);
}
