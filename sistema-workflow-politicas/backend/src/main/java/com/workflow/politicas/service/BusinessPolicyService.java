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

    public BusinessPolicyService(BusinessPolicyRepository businessPolicyRepository) {
        this.businessPolicyRepository = businessPolicyRepository;
    }

    public List<BusinessPolicy> findAll() {
        return businessPolicyRepository.findAll();
    }

    public Optional<BusinessPolicy> findById(String id) {
        return businessPolicyRepository.findById(id);
    }

    public BusinessPolicy create(BusinessPolicy policy) {
        if (policy.getName() == null || policy.getName().isEmpty() ||
            policy.getDescription() == null || policy.getDescription().isEmpty() ||
            policy.getCreatedBy() == null || policy.getCreatedBy().isEmpty()) {
            throw new IllegalArgumentException("Name, description and createdBy are mandatory");
        }
        policy.setStatus("DRAFT");
        policy.setCreatedAt(LocalDateTime.now());
        policy.setUpdatedAt(LocalDateTime.now());
        return businessPolicyRepository.save(policy);
    }

    public BusinessPolicy update(String id, BusinessPolicy policyDetails) {
        BusinessPolicy policy = businessPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("BusinessPolicy not found with id: " + id));
        
        policy.setName(policyDetails.getName());
        policy.setDescription(policyDetails.getDescription());
        policy.setType(policyDetails.getType());
        policy.setUpdatedAt(LocalDateTime.now());
        
        return businessPolicyRepository.save(policy);
    }

    public BusinessPolicy activate(String id) {
        BusinessPolicy policy = businessPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("BusinessPolicy not found with id: " + id));
        policy.setStatus("ACTIVE");
        policy.setUpdatedAt(LocalDateTime.now());
        return businessPolicyRepository.save(policy);
    }

    public BusinessPolicy deactivate(String id) {
        BusinessPolicy policy = businessPolicyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("BusinessPolicy not found with id: " + id));
        policy.setStatus("INACTIVE");
        policy.setUpdatedAt(LocalDateTime.now());
        return businessPolicyRepository.save(policy);
    }

    public void deleteById(String id) {
        businessPolicyRepository.deleteById(id);
    }
}
