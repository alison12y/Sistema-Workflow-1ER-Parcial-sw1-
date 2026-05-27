package com.workflow.politicas.service;

import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BusinessPolicyService {
    private final BusinessPolicyRepository businessPolicyRepository;
    private final AuditLogService auditLogService;

    public BusinessPolicyService(BusinessPolicyRepository businessPolicyRepository, AuditLogService auditLogService) {
        this.businessPolicyRepository = businessPolicyRepository;
        this.auditLogService = auditLogService;
    }

    public List<BusinessPolicy> findAll() {
        return businessPolicyRepository.findAll();
    }

    public List<BusinessPolicy> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        String term = query.trim();
        return businessPolicyRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(term, term);
    }

    public Optional<BusinessPolicy> findById(String id) {
        return businessPolicyRepository.findById(id);
    }

    public BusinessPolicy create(BusinessPolicy policy) {
        if (policy.getName() == null || policy.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (policy.getName().trim().length() < 3) {
            throw new IllegalArgumentException("El nombre debe tener al menos 3 caracteres");
        }
        if (policy.getDescription() == null || policy.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }
        if (policy.getCreatedBy() == null || policy.getCreatedBy().trim().isEmpty()) {
            policy.setCreatedBy("system");
        }
        if (policy.getStatus() == null || policy.getStatus().trim().isEmpty()) {
            policy.setStatus("DRAFT");
        }
        if (policy.getVersion() == null || policy.getVersion().trim().isEmpty()) {
            policy.setVersion("1.0");
        }
        if (policy.getType() == null || policy.getType().trim().isEmpty()) {
            policy.setType("GENERAL_REQUEST");
        }
        policy.setName(policy.getName().trim());
        policy.setDescription(policy.getDescription().trim());
        if (policy.getResponsible() != null) {
            policy.setResponsible(policy.getResponsible().trim());
        }
        policy.setCreatedAt(LocalDateTime.now());
        policy.setUpdatedAt(LocalDateTime.now());
        BusinessPolicy saved = businessPolicyRepository.save(policy);
        auditLogService.register(
                "BusinessPolicy",
                saved.getId(),
                "CREATE_POLICY",
                saved.getCreatedBy(),
                null,
                "DRAFT",
                "Policy created: " + saved.getName()
        );
        return saved;
    }

    public BusinessPolicy update(String id, BusinessPolicy policyDetails) {
        BusinessPolicy policy = businessPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("BusinessPolicy not found with id: " + id));
        
        policy.setName(policyDetails.getName());
        policy.setDescription(policyDetails.getDescription());
        policy.setType(policyDetails.getType());
        policy.setVersion(policyDetails.getVersion());
        policy.setResponsible(policyDetails.getResponsible());
        policy.setStatus(policyDetails.getStatus());
        policy.setUpdatedAt(LocalDateTime.now());
        
        return businessPolicyRepository.save(policy);
    }

    public BusinessPolicy activate(String id) {
        BusinessPolicy policy = businessPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("BusinessPolicy not found with id: " + id));
        String previousStatus = policy.getStatus();
        policy.setStatus("ACTIVE");
        policy.setUpdatedAt(LocalDateTime.now());
        BusinessPolicy saved = businessPolicyRepository.save(policy);
        auditLogService.register(
                "BusinessPolicy",
                saved.getId(),
                "ACTIVATE_POLICY",
                saved.getCreatedBy(),
                previousStatus,
                "ACTIVE",
                "Policy activated: " + saved.getName()
        );
        return saved;
    }

    public BusinessPolicy deactivate(String id) {
        BusinessPolicy policy = businessPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("BusinessPolicy not found with id: " + id));
        String previousStatus = policy.getStatus();
        policy.setStatus("INACTIVE");
        policy.setUpdatedAt(LocalDateTime.now());
        BusinessPolicy saved = businessPolicyRepository.save(policy);
        auditLogService.register(
                "BusinessPolicy",
                saved.getId(),
                "DEACTIVATE_POLICY",
                saved.getCreatedBy(),
                previousStatus,
                "INACTIVE",
                "Policy deactivated: " + saved.getName()
        );
        return saved;
    }

    public void deleteById(String id) {
        businessPolicyRepository.deleteById(id);
    }
}
