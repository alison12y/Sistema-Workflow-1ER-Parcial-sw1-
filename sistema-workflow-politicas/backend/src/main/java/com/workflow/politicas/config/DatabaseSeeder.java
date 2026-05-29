package com.workflow.politicas.config;

import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.DepartmentRepository;
import com.workflow.politicas.repository.RoleRepository;
import com.workflow.politicas.repository.UserRepository;
import com.workflow.politicas.service.Phase1MigrationService;
import com.workflow.politicas.service.Phase2MigrationService;
import com.workflow.politicas.service.Phase3MigrationService;
import com.workflow.politicas.service.Phase4MigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final BusinessPolicyRepository businessPolicyRepository;
    private final Phase1MigrationService phase1MigrationService;
    private final Phase2MigrationService phase2MigrationService;
    private final Phase3MigrationService phase3MigrationService;
    private final Phase4MigrationService phase4MigrationService;

    public DatabaseSeeder(
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            BusinessPolicyRepository businessPolicyRepository,
            Phase1MigrationService phase1MigrationService,
            Phase2MigrationService phase2MigrationService,
            Phase3MigrationService phase3MigrationService,
            Phase4MigrationService phase4MigrationService
    ) {
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.businessPolicyRepository = businessPolicyRepository;
        this.phase1MigrationService = phase1MigrationService;
        this.phase2MigrationService = phase2MigrationService;
        this.phase3MigrationService = phase3MigrationService;
        this.phase4MigrationService = phase4MigrationService;
    }

    @Override
    public void run(String... args) {
        phase1MigrationService.seedIfEmpty();
        phase2MigrationService.syncRolePermissions();
        phase2MigrationService.seedSamplePolicies();
        phase3MigrationService.seedSampleActivities();
        phase4MigrationService.seedSampleTransitions();
        seedPolicies();
    }

    private void seedPolicies() {
        if (businessPolicyRepository.count() > 0) {
            log.info("DatabaseSeeder: policies already exist, skipping policy seed");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        BusinessPolicy policy = new BusinessPolicy();
        policy.setName("Política de solicitud de vacaciones");
        policy.setDescription(
                "Define el proceso para registrar, revisar y aprobar solicitudes de vacaciones del personal."
        );
        policy.setType("Permiso de Ausencia");
        policy.setStatus("ACTIVE");
        policy.setVersion("1.0");
        policy.setResponsible("Recursos Humanos");
        policy.setCreatedBy("sistema.admin");
        policy.setCreatedAt(now);
        policy.setUpdatedAt(now);
        businessPolicyRepository.save(policy);

        log.info("DatabaseSeeder: created initial business policy '{}'", policy.getName());
    }
}
