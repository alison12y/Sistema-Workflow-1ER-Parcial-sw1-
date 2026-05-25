package com.workflow.politicas.repository;

import com.workflow.politicas.model.Activity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends MongoRepository<Activity, String> {
    java.util.List<Activity> findByDiagramId(String diagramId);
}
