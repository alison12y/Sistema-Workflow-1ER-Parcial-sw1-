package com.workflow.politicas.service;

import com.workflow.politicas.config.Phase4BootstrapData;
import com.workflow.politicas.config.Phase4BootstrapData.TransitionSeed;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.model.WorkflowTransition;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import com.workflow.politicas.repository.WorkflowTransitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class Phase4MigrationService {

    private static final Logger log = LoggerFactory.getLogger(Phase4MigrationService.class);

    private final BusinessPolicyRepository businessPolicyRepository;
    private final WorkflowActivityRepository workflowActivityRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;

    public Phase4MigrationService(
            BusinessPolicyRepository businessPolicyRepository,
            WorkflowActivityRepository workflowActivityRepository,
            WorkflowTransitionRepository workflowTransitionRepository
    ) {
        this.businessPolicyRepository = businessPolicyRepository;
        this.workflowActivityRepository = workflowActivityRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
    }

    public void seedSampleTransitions() {
        Optional<BusinessPolicy> policy = businessPolicyRepository.findAll().stream()
                .filter(p -> Phase4BootstrapData.METER_POLICY_NAME.equalsIgnoreCase(p.getName()))
                .findFirst();

        if (policy.isEmpty()) {
            log.info("Phase4MigrationService: policy '{}' not found, skipping transition seed",
                    Phase4BootstrapData.METER_POLICY_NAME);
            return;
        }

        String policyId = policy.get().getId();
        List<WorkflowActivity> activities = workflowActivityRepository.findByPolicyIdOrderByOrderIndexAsc(policyId);
        if (activities.isEmpty()) {
            log.info("Phase4MigrationService: no activities for '{}', skipping transition seed",
                    Phase4BootstrapData.METER_POLICY_NAME);
            return;
        }

        Map<String, WorkflowActivity> byName = activities.stream()
                .collect(Collectors.toMap(a -> a.getName().toLowerCase(), Function.identity(), (a, b) -> a));

        LocalDateTime now = LocalDateTime.now();
        int created = 0;
        int skipped = 0;
        for (TransitionSeed seed : Phase4BootstrapData.METER_INSTALLATION_TRANSITIONS) {
            WorkflowActivity from = byName.get(seed.fromActivityName().toLowerCase());
            WorkflowActivity to = byName.get(seed.toActivityName().toLowerCase());
            if (from == null || to == null) {
                log.warn("Phase4MigrationService: skipping transition {} -> {} (activity not found)",
                        seed.fromActivityName(), seed.toActivityName());
                continue;
            }

            if (workflowTransitionRepository.existsByPolicyIdAndFromActivityIdAndToActivityId(
                    policyId, from.getId(), to.getId())) {
                skipped++;
                continue;
            }

            WorkflowTransition transition = new WorkflowTransition();
            transition.setPolicyId(policyId);
            transition.setFromActivityId(from.getId());
            transition.setFromActivityName(from.getName());
            transition.setToActivityId(to.getId());
            transition.setToActivityName(to.getName());
            transition.setTransitionType(seed.transitionType());
            transition.setConditionLabel(seed.conditionLabel());
            transition.setOrderIndex(seed.orderIndex());
            transition.setActive(true);
            transition.setCreatedAt(now);
            transition.setUpdatedAt(now);
            workflowTransitionRepository.save(transition);
            created++;
        }

        log.info("Phase4MigrationService: seeded {} transitions for '{}' ({} already existed)",
                created, Phase4BootstrapData.METER_POLICY_NAME, skipped);
    }
}
