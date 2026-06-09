package com.workflow.politicas.config;

import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.DepartmentRepository;
import com.workflow.politicas.repository.RoleRepository;
import com.workflow.politicas.repository.UserRepository;
import com.workflow.politicas.service.Phase1MigrationService;
import com.workflow.politicas.service.Phase2MigrationService;
import com.workflow.politicas.service.Phase3MigrationService;
import com.workflow.politicas.service.FormFieldKeyMigrationService;
import com.workflow.politicas.service.WorkflowTransitionConditionMigrationService;
import com.workflow.politicas.service.DocumentRepositoryMigrationService;
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
    private final FormFieldKeyMigrationService formFieldKeyMigrationService;
    private final WorkflowTransitionConditionMigrationService workflowTransitionConditionMigrationService;
    private final DocumentRepositoryMigrationService documentRepositoryMigrationService;

    public DatabaseSeeder(
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            BusinessPolicyRepository businessPolicyRepository,
            Phase1MigrationService phase1MigrationService,
            Phase2MigrationService phase2MigrationService,
            Phase3MigrationService phase3MigrationService,
            Phase4MigrationService phase4MigrationService,
            FormFieldKeyMigrationService formFieldKeyMigrationService,
            WorkflowTransitionConditionMigrationService workflowTransitionConditionMigrationService,
            DocumentRepositoryMigrationService documentRepositoryMigrationService
    ) {
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.businessPolicyRepository = businessPolicyRepository;
        this.phase1MigrationService = phase1MigrationService;
        this.phase2MigrationService = phase2MigrationService;
        this.phase3MigrationService = phase3MigrationService;
        this.phase4MigrationService = phase4MigrationService;
        this.formFieldKeyMigrationService = formFieldKeyMigrationService;
        this.workflowTransitionConditionMigrationService = workflowTransitionConditionMigrationService;
        this.documentRepositoryMigrationService = documentRepositoryMigrationService;
    }

    @Override
    public void run(String... args) {
        phase1MigrationService.seedIfEmpty();
        phase2MigrationService.syncRolePermissions();
        phase2MigrationService.seedSamplePolicies();
        phase3MigrationService.seedSampleActivities();
        phase4MigrationService.seedSampleTransitions();
        formFieldKeyMigrationService.migrateRecepcionValidoField();
        workflowTransitionConditionMigrationService.migrateConditionalExpressionsFromLabels();
        seedPolicies();
        documentRepositoryMigrationService.migrateExistingTramites("system-startup");
    }

    private void seedPolicies() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Solicitud de vacaciones
        if (!businessPolicyRepository.findAll().stream().anyMatch(p -> "Solicitud de vacaciones".equalsIgnoreCase(p.getName()))) {
            BusinessPolicy v = new BusinessPolicy();
            v.setName("Solicitud de vacaciones");
            v.setDescription("Define el proceso para registrar, revisar y aprobar solicitudes de vacaciones del personal.");
            v.setType("Permiso de Ausencia");
            v.setStatus("ACTIVE");
            v.setVersion("1.0");
            v.setResponsible("Recursos Humanos");
            v.setCreatedBy("sistema.admin");
            v.setCreatedAt(now);
            v.setUpdatedAt(now);
            businessPolicyRepository.save(v);
            log.info("DatabaseSeeder: created policy 'Solicitud de vacaciones'");
        }

        // 2. Solicitud de Permiso Laboral
        if (!businessPolicyRepository.findAll().stream().anyMatch(p -> "Solicitud de Permiso Laboral".equalsIgnoreCase(p.getName()))) {
            BusinessPolicy p = new BusinessPolicy();
            p.setName("Solicitud de Permiso Laboral");
            p.setDescription("Trámite general para permisos de corta duración o motivos personales.");
            p.setType("Permiso de Ausencia");
            p.setStatus("ACTIVE");
            p.setVersion("1.0");
            p.setResponsible("Recursos Humanos");
            p.setCreatedBy("sistema.admin");
            p.setCreatedAt(now);
            p.setUpdatedAt(now);
            businessPolicyRepository.save(p);
            log.info("DatabaseSeeder: created policy 'Solicitud de Permiso Laboral'");
        }

        // 3. Solicitud de revisión documental
        if (!businessPolicyRepository.findAll().stream().anyMatch(p -> "Solicitud de revisión documental".equalsIgnoreCase(p.getName()))) {
            BusinessPolicy d = new BusinessPolicy();
            d.setName("Solicitud de revisión documental");
            d.setDescription("Establece el flujo de carga, revisión legal y validación de documentación.");
            d.setType("DOCUMENT_APPROVAL");
            d.setStatus("ACTIVE");
            d.setVersion("1.0");
            d.setResponsible("Legal");
            d.setCreatedBy("sistema.admin");
            d.setCreatedAt(now);
            d.setUpdatedAt(now);
            businessPolicyRepository.save(d);
            log.info("DatabaseSeeder: created policy 'Solicitud de revisión documental'");
        }
    }
}
