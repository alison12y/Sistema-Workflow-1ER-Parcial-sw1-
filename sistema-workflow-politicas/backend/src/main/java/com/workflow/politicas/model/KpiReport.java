package com.workflow.politicas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "kpi_reports")
public class KpiReport {
    @Id
    private String id;
    private String policyId;
    private String reportType; // "AVG_TIME", "BOTTLENECKS", "USER_PERFORMANCE"
    private Map<String, Object> metrics;
    private LocalDateTime generatedAt;

    public KpiReport() {
    }

    public KpiReport(String id, String policyId, String reportType, Map<String, Object> metrics, LocalDateTime generatedAt) {
        this.id = id;
        this.policyId = policyId;
        this.reportType = reportType;
        this.metrics = metrics;
        this.generatedAt = generatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
