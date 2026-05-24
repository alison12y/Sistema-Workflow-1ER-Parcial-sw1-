package com.workflow.politicas.repository;

import com.workflow.politicas.model.KpiReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KpiReportRepository extends MongoRepository<KpiReport, String> {
}
