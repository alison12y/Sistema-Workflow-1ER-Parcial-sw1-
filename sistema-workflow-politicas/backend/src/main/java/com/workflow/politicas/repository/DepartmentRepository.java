package com.workflow.politicas.repository;

import com.workflow.politicas.model.Department;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends MongoRepository<Department, String> {

    java.util.Optional<Department> findByNameIgnoreCase(String name);
}
