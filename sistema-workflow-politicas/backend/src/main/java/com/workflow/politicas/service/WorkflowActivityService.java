package com.workflow.politicas.service;

import com.workflow.politicas.dto.WorkflowActivityPositionRequest;
import com.workflow.politicas.dto.WorkflowActivityRequest;
import com.workflow.politicas.dto.WorkflowActivityResponse;
import com.workflow.politicas.dto.WorkflowDeleteResponse;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.model.WorkflowTransition;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import com.workflow.politicas.repository.WorkflowTransitionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class WorkflowActivityService {

    private final WorkflowActivityRepository workflowActivityRepository;
    private final BusinessPolicyRepository businessPolicyRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;

    public WorkflowActivityService(
            WorkflowActivityRepository workflowActivityRepository,
            BusinessPolicyRepository businessPolicyRepository,
            WorkflowTransitionRepository workflowTransitionRepository
    ) {
        this.workflowActivityRepository = workflowActivityRepository;
        this.businessPolicyRepository = businessPolicyRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
    }

    public List<WorkflowActivityResponse> findByPolicyId(String policyId) {
        validatePolicyExists(policyId);
        return workflowActivityRepository.findByPolicyIdOrderByOrderIndexAsc(policyId).stream()
                .map(this::toResponse)
                .toList();
    }

    public Optional<WorkflowActivityResponse> findById(String id) {
        return workflowActivityRepository.findById(id).map(this::toResponse);
    }

    public WorkflowActivityResponse create(WorkflowActivityRequest request) {
        validateRequest(request, true);
        BusinessPolicy policy = validatePolicyExists(request.getPolicyId());

        WorkflowActivity activity = new WorkflowActivity();
        applyRequest(activity, request);
        activity.setPolicyId(policy.getId());
        activity.setOrderIndex(resolveOrderIndex(request.getPolicyId(), request.getOrderIndex()));
        activity.setCreatedAt(LocalDateTime.now());
        activity.setUpdatedAt(LocalDateTime.now());
        if (activity.getStatus() == null || activity.getStatus().isBlank()) {
            activity.setStatus("BORRADOR");
        }
        activity.setActive(true);

        return toResponse(workflowActivityRepository.save(activity));
    }

    public WorkflowActivityResponse update(String id, WorkflowActivityRequest request) {
        WorkflowActivity activity = workflowActivityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));

        if (request.getPolicyId() != null && !request.getPolicyId().equals(activity.getPolicyId())) {
            validatePolicyExists(request.getPolicyId());
            activity.setPolicyId(request.getPolicyId());
        }

        validateRequest(request, false);
        applyRequest(activity, request);

        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) {
            activity.setOrderIndex(request.getOrderIndex());
        }

        activity.setUpdatedAt(LocalDateTime.now());
        return toResponse(workflowActivityRepository.save(activity));
    }

    public WorkflowDeleteResponse delete(String id) {
        WorkflowActivity activity = workflowActivityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));

        if (!activity.isActive()) {
            WorkflowDeleteResponse alreadyInactive = new WorkflowDeleteResponse();
            alreadyInactive.setLogicalDelete(true);
            alreadyInactive.setMessage("La actividad ya estaba inactiva.");
            return alreadyInactive;
        }

        Set<String> relatedTransitionIds = new HashSet<>();
        workflowTransitionRepository.findByFromActivityId(id)
                .forEach(transition -> relatedTransitionIds.add(transition.getId()));
        workflowTransitionRepository.findByToActivityId(id)
                .forEach(transition -> relatedTransitionIds.add(transition.getId()));

        LocalDateTime now = LocalDateTime.now();
        int affectedConnections = 0;
        for (String transitionId : relatedTransitionIds) {
            Optional<WorkflowTransition> transitionOpt = workflowTransitionRepository.findById(transitionId);
            if (transitionOpt.isEmpty()) {
                continue;
            }
            WorkflowTransition transition = transitionOpt.get();
            if (transition.isActive()) {
                transition.setActive(false);
                transition.setUpdatedAt(now);
                workflowTransitionRepository.save(transition);
                affectedConnections++;
            }
        }

        activity.setActive(false);
        activity.setStatus("INACTIVA");
        activity.setUpdatedAt(now);
        workflowActivityRepository.save(activity);

        WorkflowDeleteResponse response = new WorkflowDeleteResponse();
        response.setLogicalDelete(true);
        response.setAffectedConnections(affectedConnections);
        if (affectedConnections > 0) {
            response.setMessage(
                    "Actividad desactivada correctamente. Se desactivaron "
                            + affectedConnections + " conexión(es) asociada(s).");
        } else {
            response.setMessage("Actividad desactivada correctamente.");
        }
        return response;
    }

    public WorkflowActivityResponse activate(String id) {
        WorkflowActivity activity = workflowActivityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        activity.setActive(true);
        activity.setStatus("ACTIVA");
        activity.setUpdatedAt(LocalDateTime.now());
        return toResponse(workflowActivityRepository.save(activity));
    }

    public WorkflowActivityResponse deactivate(String id) {
        WorkflowActivity activity = workflowActivityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        activity.setActive(false);
        activity.setStatus("INACTIVA");
        activity.setUpdatedAt(LocalDateTime.now());
        return toResponse(workflowActivityRepository.save(activity));
    }

    public WorkflowActivityResponse updatePosition(String id, WorkflowActivityPositionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La posición es obligatoria");
        }
        if (request.getPositionX() == null || request.getPositionY() == null) {
            throw new IllegalArgumentException("positionX y positionY son obligatorios");
        }
        if (request.getPositionX() < 0 || request.getPositionY() < 0) {
            throw new IllegalArgumentException("Las coordenadas deben ser valores positivos");
        }

        WorkflowActivity activity = workflowActivityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));

        activity.setPositionX(request.getPositionX());
        activity.setPositionY(request.getPositionY());
        activity.setUpdatedAt(LocalDateTime.now());
        return toResponse(workflowActivityRepository.save(activity));
    }

    public WorkflowActivityResponse clearPosition(String id) {
        WorkflowActivity activity = workflowActivityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
        activity.setPositionX(null);
        activity.setPositionY(null);
        activity.setUpdatedAt(LocalDateTime.now());
        return toResponse(workflowActivityRepository.save(activity));
    }

    public int countByPolicyId(String policyId) {
        return (int) workflowActivityRepository.countByPolicyId(policyId);
    }

    private BusinessPolicy validatePolicyExists(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("La política de negocio es obligatoria");
        }
        return businessPolicyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("La política seleccionada no existe"));
    }

    private void validateRequest(WorkflowActivityRequest request, boolean creating) {
        if (creating && (request.getPolicyId() == null || request.getPolicyId().isBlank())) {
            throw new IllegalArgumentException("La política de negocio es obligatoria");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la actividad es obligatorio");
        }
        if (request.getName().trim().length() < 3) {
            throw new IllegalArgumentException("El nombre debe tener al menos 3 caracteres");
        }
        if (request.getEstimatedTimeHours() != null && request.getEstimatedTimeHours() < 1) {
            throw new IllegalArgumentException("El tiempo estimado debe ser mayor o igual a 1 hora");
        }
        if (request.getOrderIndex() != null && request.getOrderIndex() < 1) {
            throw new IllegalArgumentException("El orden debe ser un número positivo");
        }
    }

    private int resolveOrderIndex(String policyId, Integer requested) {
        if (requested != null && requested > 0) {
            return requested;
        }
        return workflowActivityRepository.findByPolicyId(policyId).stream()
                .mapToInt(WorkflowActivity::getOrderIndex)
                .max()
                .orElse(0) + 1;
    }

    private void applyRequest(WorkflowActivity activity, WorkflowActivityRequest request) {
        activity.setName(request.getName().trim());
        activity.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        if (request.getResponsibleType() != null) {
            activity.setResponsibleType(request.getResponsibleType().trim().toUpperCase());
        }
        if (request.getResponsibleId() != null) {
            activity.setResponsibleId(request.getResponsibleId().trim());
        }
        if (request.getResponsibleName() != null) {
            activity.setResponsibleName(request.getResponsibleName().trim());
        }
        if (request.getActivityType() != null) {
            activity.setActivityType(request.getActivityType().trim().toUpperCase());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            activity.setStatus(request.getStatus().trim().toUpperCase());
        }
        if (request.getEstimatedTimeHours() != null) {
            activity.setEstimatedTimeHours(request.getEstimatedTimeHours());
        }
        if (request.getFormId() != null) {
            activity.setFormId(request.getFormId().trim());
        }
    }

    private WorkflowActivityResponse toResponse(WorkflowActivity activity) {
        WorkflowActivityResponse response = new WorkflowActivityResponse();
        response.setId(activity.getId());
        response.setPolicyId(activity.getPolicyId());
        response.setName(activity.getName());
        response.setDescription(activity.getDescription());
        response.setResponsibleType(activity.getResponsibleType());
        response.setResponsibleId(activity.getResponsibleId());
        response.setResponsibleName(activity.getResponsibleName());
        response.setActivityType(activity.getActivityType());
        response.setStatus(activity.getStatus());
        response.setOrderIndex(activity.getOrderIndex());
        response.setEstimatedTimeHours(activity.getEstimatedTimeHours());
        response.setPositionX(activity.getPositionX());
        response.setPositionY(activity.getPositionY());
        response.setFormId(activity.getFormId());
        response.setCreatedAt(activity.getCreatedAt());
        response.setUpdatedAt(activity.getUpdatedAt());
        response.setActive(activity.isActive());
        return response;
    }
}
