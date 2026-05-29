package com.workflow.politicas.service;

import com.workflow.politicas.config.Phase3BootstrapData;
import com.workflow.politicas.config.Phase3BootstrapData.ActivitySeed;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class Phase3MigrationService {

    private static final Logger log = LoggerFactory.getLogger(Phase3MigrationService.class);

    private final BusinessPolicyRepository businessPolicyRepository;
    private final WorkflowActivityRepository workflowActivityRepository;

    public Phase3MigrationService(
            BusinessPolicyRepository businessPolicyRepository,
            WorkflowActivityRepository workflowActivityRepository
    ) {
        this.businessPolicyRepository = businessPolicyRepository;
        this.workflowActivityRepository = workflowActivityRepository;
    }

    public void seedSampleActivities() {
        Optional<BusinessPolicy> policy = businessPolicyRepository.findAll().stream()
                .filter(p -> Phase3BootstrapData.METER_POLICY_NAME.equalsIgnoreCase(p.getName()))
                .findFirst();

        if (policy.isEmpty()) {
            log.info("Phase3MigrationService: policy '{}' not found, skipping activity seed",
                    Phase3BootstrapData.METER_POLICY_NAME);
            return;
        }

        String policyId = policy.get().getId();
        long existing = workflowActivityRepository.countByPolicyId(policyId);
        if (existing > 0) {
            log.info("Phase3MigrationService: policy '{}' already has {} activities, skipping seed",
                    Phase3BootstrapData.METER_POLICY_NAME, existing);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (ActivitySeed seed : Phase3BootstrapData.METER_INSTALLATION_ACTIVITIES) {
            WorkflowActivity activity = new WorkflowActivity();
            activity.setPolicyId(policyId);
            activity.setName(seed.name());
            activity.setDescription(seed.description());
            activity.setResponsibleType(seed.responsibleType());
            activity.setResponsibleName(seed.responsibleName());
            activity.setActivityType(seed.activityType());
            activity.setOrderIndex(seed.orderIndex());
            activity.setEstimatedTimeHours(seed.estimatedTimeHours());
            activity.setStatus(seed.status());
            activity.setActive(true);
            activity.setCreatedAt(now);
            activity.setUpdatedAt(now);
            workflowActivityRepository.save(activity);
        }

        log.info("Phase3MigrationService: seeded {} activities for '{}'",
                Phase3BootstrapData.METER_INSTALLATION_ACTIVITIES.size(),
                Phase3BootstrapData.METER_POLICY_NAME);
    }
}
