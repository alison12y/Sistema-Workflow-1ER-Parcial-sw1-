package com.workflow.politicas.service;

import com.workflow.politicas.config.Phase1BootstrapData;
import com.workflow.politicas.config.Phase2BootstrapData;
import com.workflow.politicas.config.Phase2BootstrapData.PolicySeed;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class Phase2MigrationService {

    private static final Logger log = LoggerFactory.getLogger(Phase2MigrationService.class);

    private final BusinessPolicyRepository businessPolicyRepository;
    private final RoleRepository roleRepository;

    public Phase2MigrationService(
            BusinessPolicyRepository businessPolicyRepository,
            RoleRepository roleRepository
    ) {
        this.businessPolicyRepository = businessPolicyRepository;
        this.roleRepository = roleRepository;
    }

    public void syncRolePermissions() {
        for (Phase1BootstrapData.RoleSeed seed : Phase1BootstrapData.ROLES) {
            roleRepository.findByNameIgnoreCase(seed.name()).ifPresent(role -> {
                role.setPermissionIds(new java.util.HashSet<>(seed.permissions()));
                role.setActive(true);
                roleRepository.save(role);
            });
        }
        log.info("Phase2MigrationService: synchronized role permissions for Fase 2");
    }

    public void seedSamplePolicies() {
        int created = 0;
        for (PolicySeed seed : Phase2BootstrapData.SAMPLE_POLICIES) {
            boolean exists = businessPolicyRepository.findAll().stream()
                    .anyMatch(p -> seed.name().equalsIgnoreCase(p.getName()));
            if (exists) {
                continue;
            }
            BusinessPolicy policy = new BusinessPolicy();
            policy.setName(seed.name());
            policy.setDescription(seed.description());
            policy.setType(seed.type());
            policy.setStatus(seed.status());
            policy.setVersion("1.0");
            policy.setResponsible(seed.responsible());
            policy.setCreatedBy(seed.createdBy());
            policy.setCreatedAt(LocalDateTime.now());
            policy.setUpdatedAt(LocalDateTime.now());
            businessPolicyRepository.save(policy);
            created++;
        }
        if (created > 0) {
            log.info("Phase2MigrationService: created {} sample business policies", created);
        }
    }
}
