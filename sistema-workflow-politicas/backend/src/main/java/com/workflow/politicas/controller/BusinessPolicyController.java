package com.workflow.politicas.controller;

import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.dto.PolicyDetailResponse;
import com.workflow.politicas.dto.PolicySummaryResponse;
import com.workflow.politicas.service.BusinessPolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class BusinessPolicyController {
    private final BusinessPolicyService businessPolicyService;

    public BusinessPolicyController(BusinessPolicyService businessPolicyService) {
        this.businessPolicyService = businessPolicyService;
    }

    @GetMapping
    public List<BusinessPolicy> getAllPolicies(@RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return businessPolicyService.search(search.trim());
        }
        return businessPolicyService.findAll();
    }

    @GetMapping("/summaries")
    public List<PolicySummaryResponse> getPolicySummaries(@RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return businessPolicyService.searchSummaries(search.trim());
        }
        return businessPolicyService.findAllSummaries();
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<PolicyDetailResponse> getPolicyDetail(@PathVariable String id) {
        return businessPolicyService.getDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<BusinessPolicy> searchPolicies(@RequestParam String q) {
        return businessPolicyService.search(q);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessPolicy> getPolicyById(@PathVariable String id) {
        return businessPolicyService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public BusinessPolicy createPolicy(@RequestBody BusinessPolicy policy) {
        return businessPolicyService.create(policy);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessPolicy> updatePolicy(@PathVariable String id, @RequestBody BusinessPolicy policyDetails) {
        try {
            return ResponseEntity.ok(businessPolicyService.update(id, policyDetails));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<BusinessPolicy> activatePolicy(@PathVariable String id) {
        try {
            return ResponseEntity.ok(businessPolicyService.activate(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<BusinessPolicy> deactivatePolicy(@PathVariable String id) {
        try {
            return ResponseEntity.ok(businessPolicyService.deactivate(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable String id) {
        businessPolicyService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
