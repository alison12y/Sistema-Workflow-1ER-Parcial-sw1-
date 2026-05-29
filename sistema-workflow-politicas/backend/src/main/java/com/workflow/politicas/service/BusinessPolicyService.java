package com.workflow.politicas.service;

import com.workflow.politicas.dto.PolicyDetailResponse;
import com.workflow.politicas.dto.PolicySummaryResponse;
import com.workflow.politicas.dto.TramiteSummaryResponse;
import com.workflow.politicas.dto.WorkflowActivityResponse;
import com.workflow.politicas.dto.WorkflowTransitionResponse;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.TramiteRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BusinessPolicyService {
    private final BusinessPolicyRepository businessPolicyRepository;
    private final TramiteRepository tramiteRepository;
    private final WorkflowActivityService workflowActivityService;
    private final WorkflowTransitionService workflowTransitionService;
    private final AuditLogService auditLogService;
    private final BitacoraService bitacoraService;

    public BusinessPolicyService(
            BusinessPolicyRepository businessPolicyRepository,
            TramiteRepository tramiteRepository,
            WorkflowActivityService workflowActivityService,
            WorkflowTransitionService workflowTransitionService,
            AuditLogService auditLogService,
            BitacoraService bitacoraService
    ) {
        this.businessPolicyRepository = businessPolicyRepository;
        this.tramiteRepository = tramiteRepository;
        this.workflowActivityService = workflowActivityService;
        this.workflowTransitionService = workflowTransitionService;
        this.auditLogService = auditLogService;
        this.bitacoraService = bitacoraService;
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

    public List<PolicySummaryResponse> findAllSummaries() {
        return businessPolicyRepository.findAll().stream().map(this::toSummary).toList();
    }

    public List<PolicySummaryResponse> searchSummaries(String query) {
        return search(query).stream().map(this::toSummary).toList();
    }

    public Optional<PolicyDetailResponse> getDetail(String id) {
        return businessPolicyRepository.findById(id).map(policy -> {
            PolicyDetailResponse detail = new PolicyDetailResponse();
            detail.setId(policy.getId());
            detail.setName(policy.getName());
            detail.setDescription(policy.getDescription());
            detail.setType(policy.getType());
            detail.setStatus(policy.getStatus());
            detail.setVersion(policy.getVersion());
            detail.setResponsible(policy.getResponsible());
            detail.setCreatedBy(policy.getCreatedBy());
            detail.setCreatedAt(policy.getCreatedAt());
            detail.setUpdatedAt(policy.getUpdatedAt());

            List<WorkflowActivityResponse> activities = workflowActivityService.findByPolicyId(id);
            detail.setActivities(activities);
            detail.setActivityCount(activities.size());

            List<WorkflowTransitionResponse> transitions = workflowTransitionService.findByPolicyId(id);
            detail.setTransitions(transitions);
            detail.setTransitionCount(transitions.size());
            detail.setFlowPreview(workflowTransitionService.buildFlowPreview(id));

            List<TramiteSummaryResponse> tramites = tramiteRepository.findByPolicyId(id).stream()
                    .map(this::toTramiteSummary)
                    .toList();
            detail.setTramites(tramites);
            detail.setTramiteCount(tramites.size());
            return detail;
        });
    }

    private PolicySummaryResponse toSummary(BusinessPolicy policy) {
        PolicySummaryResponse summary = new PolicySummaryResponse();
        summary.setId(policy.getId());
        summary.setName(policy.getName());
        summary.setDescription(policy.getDescription());
        summary.setType(policy.getType());
        summary.setStatus(policy.getStatus());
        summary.setVersion(policy.getVersion());
        summary.setResponsible(policy.getResponsible());
        summary.setCreatedBy(policy.getCreatedBy());
        summary.setCreatedAt(policy.getCreatedAt());
        summary.setActivityCount(workflowActivityService.countByPolicyId(policy.getId()));
        summary.setTramiteCount((int) tramiteRepository.countByPolicyId(policy.getId()));
        return summary;
    }

    private TramiteSummaryResponse toTramiteSummary(Tramite tramite) {
        TramiteSummaryResponse summary = new TramiteSummaryResponse();
        summary.setId(tramite.getId());
        summary.setCode(tramite.getCode());
        summary.setPolicyName(tramite.getPolicyName());
        summary.setDescription(tramite.getDescription());
        summary.setRequesterName(
                tramite.getRequesterName() != null ? tramite.getRequesterName() : tramite.getRequestedByName()
        );
        summary.setStatus(tramite.getStatus());
        summary.setCurrentActivity(tramite.getCurrentActivity());
        summary.setResponsible(tramite.getResponsible());
        summary.setCreatedAt(tramite.getCreatedAt());
        summary.setUpdatedAt(tramite.getUpdatedAt());
        return summary;
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
        String actor = bitacoraService.resolveActorDisplay();
        bitacoraService.registrar(
                "Políticas",
                "CREAR_POLITICA",
                actor + " creó la política " + saved.getName(),
                "BusinessPolicy",
                saved.getId()
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

        BusinessPolicy saved = businessPolicyRepository.save(policy);
        String actor = bitacoraService.resolveActorDisplay();
        bitacoraService.registrar(
                "Políticas",
                "EDITAR_POLITICA",
                actor + " editó la política " + saved.getName(),
                "BusinessPolicy",
                saved.getId()
        );
        return saved;
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
        businessPolicyRepository.findById(id).ifPresent(policy -> {
            String actor = bitacoraService.resolveActorDisplay();
            bitacoraService.registrar(
                    "Políticas",
                    "ELIMINAR_POLITICA",
                    actor + " eliminó la política " + policy.getName(),
                    "BusinessPolicy",
                    policy.getId()
            );
            businessPolicyRepository.deleteById(id);
        });
    }
}
