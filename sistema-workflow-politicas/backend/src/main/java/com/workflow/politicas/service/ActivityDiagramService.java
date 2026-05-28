package com.workflow.politicas.service;

import com.workflow.politicas.dto.ActivityDiagramSaveRequest;
import com.workflow.politicas.model.ActivityDiagram;
import com.workflow.politicas.model.DiagramEdge;
import com.workflow.politicas.model.DiagramNode;
import com.workflow.politicas.repository.ActivityDiagramRepository;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ActivityDiagramService {

    private final ActivityDiagramRepository activityDiagramRepository;
    private final BusinessPolicyRepository businessPolicyRepository;
    private final BitacoraService bitacoraService;

    public ActivityDiagramService(
            ActivityDiagramRepository activityDiagramRepository,
            BusinessPolicyRepository businessPolicyRepository,
            BitacoraService bitacoraService
    ) {
        this.activityDiagramRepository = activityDiagramRepository;
        this.businessPolicyRepository = businessPolicyRepository;
        this.bitacoraService = bitacoraService;
    }

    public ActivityDiagram getByPolicyId(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la política es obligatorio");
        }

        return activityDiagramRepository.findByPolicyId(policyId).orElseGet(() -> {
            ActivityDiagram empty = new ActivityDiagram();
            empty.setPolicyId(policyId);
            empty.setName("Diagrama de actividades");
            return empty;
        });
    }

    public ActivityDiagram save(ActivityDiagramSaveRequest request) {
        validate(request);

        ActivityDiagram diagram = activityDiagramRepository
                .findByPolicyId(request.getPolicyId())
                .orElseGet(ActivityDiagram::new);

        if (diagram.getId() == null) {
            diagram.setCreatedAt(LocalDateTime.now());
        }

        diagram.setPolicyId(request.getPolicyId());
        diagram.setName(request.getName() != null && !request.getName().isBlank()
                ? request.getName().trim()
                : "Diagrama de actividades");
        diagram.setLanes(request.getLanes());
        diagram.setNodes(request.getNodes());
        diagram.setEdges(request.getEdges());
        diagram.setUpdatedAt(LocalDateTime.now());

        ActivityDiagram saved = activityDiagramRepository.save(diagram);

        businessPolicyRepository.findById(saved.getPolicyId()).ifPresent(policy -> {
            String actor = bitacoraService.resolveActorDisplay();
            bitacoraService.registrar(
                    "Diagramas UML",
                    "GUARDAR_DIAGRAMA",
                    actor + " guardó el Diagrama de Actividades UML 2.5 de la política " + policy.getName(),
                    "ActivityDiagram",
                    saved.getId()
            );
        });

        return saved;
    }

    public ActivityDiagram update(String id, ActivityDiagramSaveRequest request) {
        ActivityDiagram diagram = activityDiagramRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ActivityDiagram not found with id: " + id));
        request.setPolicyId(diagram.getPolicyId());
        validate(request);

        diagram.setName(request.getName());
        diagram.setLanes(request.getLanes());
        diagram.setNodes(request.getNodes());
        diagram.setEdges(request.getEdges());
        diagram.setUpdatedAt(LocalDateTime.now());
        return activityDiagramRepository.save(diagram);
    }

    public void deleteById(String id) {
        if (!activityDiagramRepository.existsById(id)) {
            throw new RuntimeException("ActivityDiagram not found with id: " + id);
        }
        activityDiagramRepository.deleteById(id);
    }

    private void validate(ActivityDiagramSaveRequest request) {
        if (request.getPolicyId() == null || request.getPolicyId().isBlank()) {
            throw new IllegalArgumentException("El identificador de la política es obligatorio");
        }
        if (!businessPolicyRepository.existsById(request.getPolicyId())) {
            throw new IllegalArgumentException("La política no existe");
        }
        if (request.getNodes() == null || request.getNodes().isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos un nodo al diagrama");
        }

        boolean hasInitial = false;
        boolean hasFinal = false;
        Set<String> nodeIds = new HashSet<>();

        for (DiagramNode node : request.getNodes()) {
            if (node.getId() == null || node.getId().isBlank()) {
                throw new IllegalArgumentException("Todos los nodos deben tener identificador");
            }
            if (!nodeIds.add(node.getId())) {
                throw new IllegalArgumentException("Existen nodos con identificador duplicado");
            }

            String type = normalizeType(node.getType());
            node.setType(type);

            if ("INITIAL".equals(type)) {
                hasInitial = true;
            }
            if ("FINAL".equals(type)) {
                hasFinal = true;
            }

            if (requiresLabel(type)) {
                if (node.getLabel() == null || node.getLabel().trim().isEmpty()) {
                    throw new IllegalArgumentException("Todas las actividades deben tener nombre");
                }
                node.setLabel(node.getLabel().trim());
            }
        }

        if (!hasInitial) {
            throw new IllegalArgumentException("Debe existir un nodo inicial");
        }
        if (!hasFinal) {
            throw new IllegalArgumentException("Debe existir un nodo final");
        }

        if (request.getEdges() != null) {
            for (DiagramEdge edge : request.getEdges()) {
                if (edge.getSourceId() == null || edge.getTargetId() == null) {
                    throw new IllegalArgumentException("Conexión inválida: origen o destino faltante");
                }
                if (edge.getSourceId().equals(edge.getTargetId())) {
                    throw new IllegalArgumentException("Conexión inválida: un nodo no puede conectarse consigo mismo");
                }
                if (!nodeIds.contains(edge.getSourceId()) || !nodeIds.contains(edge.getTargetId())) {
                    throw new IllegalArgumentException("Conexión inválida: nodo origen o destino no existe");
                }
            }
        }
    }

    private boolean requiresLabel(String type) {
        return "ACTION".equals(type) || "DECISION".equals(type) || "MERGE".equals(type) || "FORK_JOIN".equals(type);
    }

    private String normalizeType(String type) {
        if (type == null) {
            return "ACTION";
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }
}
