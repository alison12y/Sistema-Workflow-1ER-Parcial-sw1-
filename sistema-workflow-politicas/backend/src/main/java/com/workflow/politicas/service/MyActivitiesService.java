package com.workflow.politicas.service;

import com.workflow.politicas.dto.CompleteActivityRequest;
import com.workflow.politicas.dto.FormSubmissionRequest;
import com.workflow.politicas.dto.MyActivityDto;
import com.workflow.politicas.dto.TramiteAdvanceRequest;
import com.workflow.politicas.model.Department;
import com.workflow.politicas.model.Role;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;
import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.DepartmentRepository;
import com.workflow.politicas.repository.RoleRepository;
import com.workflow.politicas.repository.TramiteRepository;
import com.workflow.politicas.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class MyActivitiesService {

    private static final String TASK_EN_CURSO = "EN_CURSO";
    private static final String STATUS_COMPLETADO = "COMPLETADO";
    private static final String STATUS_CANCELADO = "CANCELADO";

    private final TramiteRepository tramiteRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final FormSubmissionService formSubmissionService;
    private final TramiteService tramiteService;

    public MyActivitiesService(
            TramiteRepository tramiteRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            FormSubmissionService formSubmissionService,
            TramiteService tramiteService
    ) {
        this.tramiteRepository = tramiteRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.formSubmissionService = formSubmissionService;
        this.tramiteService = tramiteService;
    }

    public List<MyActivityDto> listForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<String> roleNames = resolveRoleNames(user);
        Optional<Department> department = resolveDepartment(user);
        boolean admin = isAdmin(roleNames);

        List<MyActivityDto> activities = new ArrayList<>();
        for (Tramite tramite : tramiteRepository.findAll()) {
            if (isClosed(tramite.getStatus())) {
                continue;
            }

            TramiteTask currentTask = findCurrentTask(tramite);
            if (currentTask == null) {
                continue;
            }

            if (!admin && !isAssignedToUser(currentTask.getResponsible(), user, roleNames, department)) {
                continue;
            }

            activities.add(toActivityDto(tramite, currentTask));
        }

        activities.sort(Comparator.comparing(
                MyActivityDto::getAssignedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return activities;
    }

    public Optional<MyActivityDto> findForUser(String tramiteId, String username) {
        return listForUser(username).stream()
                .filter(item -> tramiteId.equals(item.getTramiteId()))
                .findFirst();
    }

    public Tramite completeActivity(String tramiteId, CompleteActivityRequest request, String username) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Tramite tramite = tramiteRepository.findById(tramiteId)
                .orElseThrow(() -> new IllegalArgumentException("Trámite no encontrado"));

        if (isClosed(tramite.getStatus())) {
            throw new IllegalArgumentException("El trámite ya está finalizado o cancelado");
        }

        TramiteTask currentTask = findCurrentTask(tramite);
        if (currentTask == null) {
            throw new IllegalArgumentException("No hay actividad en curso para completar");
        }

        if (!currentTask.getName().equalsIgnoreCase(request.getActivityName().trim())) {
            throw new IllegalArgumentException("La actividad ya fue completada o no corresponde al trámite");
        }

        if (currentTask.getOrder() != request.getTaskOrder()) {
            throw new IllegalArgumentException("La tarea indicada no está en curso");
        }

        List<String> roleNames = resolveRoleNames(user);
        Optional<Department> department = resolveDepartment(user);
        if (!isAdmin(roleNames) && !isAssignedToUser(currentTask.getResponsible(), user, roleNames, department)) {
            throw new IllegalArgumentException("No tiene permisos para completar esta actividad");
        }

        formSubmissionService.validateRequiredResponses(
                tramite.getPolicyId(),
                request.getActivityName(),
                request.getResponses()
        );

        FormSubmissionRequest submissionRequest = new FormSubmissionRequest();
        submissionRequest.setTramiteId(tramiteId);
        submissionRequest.setPolicyId(tramite.getPolicyId());
        submissionRequest.setActivityName(request.getActivityName());
        submissionRequest.setTaskOrder(request.getTaskOrder());
        submissionRequest.setResponses(request.getResponses());
        formSubmissionService.save(submissionRequest, username);

        TramiteAdvanceRequest advanceRequest = new TramiteAdvanceRequest();
        advanceRequest.setComment(formSubmissionService.buildCompletionComment(
                request.getActivityName(),
                request.getResponses()
        ));

        return tramiteService.advance(tramiteId, advanceRequest, username);
    }

    private MyActivityDto toActivityDto(Tramite tramite, TramiteTask task) {
        MyActivityDto dto = new MyActivityDto();
        dto.setTramiteId(tramite.getId());
        dto.setPolicyId(tramite.getPolicyId());
        dto.setTaskOrder(task.getOrder());
        dto.setCode(tramite.getCode());
        dto.setPolicyName(tramite.getPolicyName());
        dto.setActivityName(task.getName());
        dto.setStatus(task.getStatus());
        dto.setResponsible(task.getResponsible());
        dto.setPriority(tramite.getPriority());
        dto.setAssignedAt(task.getStartedAt() != null ? task.getStartedAt() : tramite.getCreatedAt());
        return dto;
    }

    private TramiteTask findCurrentTask(Tramite tramite) {
        if (tramite.getTasks() == null || tramite.getTasks().isEmpty()) {
            return null;
        }

        Optional<TramiteTask> inProgress = tramite.getTasks().stream()
                .filter(task -> TASK_EN_CURSO.equals(task.getStatus()))
                .findFirst();
        if (inProgress.isPresent()) {
            return inProgress.get();
        }

        return tramite.getTasks().stream()
                .filter(task -> tramite.getCurrentActivity() != null
                        && tramite.getCurrentActivity().equalsIgnoreCase(task.getName()))
                .findFirst()
                .orElse(null);
    }

    private boolean isClosed(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return STATUS_COMPLETADO.equals(normalized)
                || STATUS_CANCELADO.equals(normalized)
                || "FINALIZADO".equals(normalized)
                || "COMPLETED".equals(normalized)
                || "CANCELLED".equals(normalized);
    }

    private boolean isAdmin(List<String> roleNames) {
        return roleNames.stream().anyMatch(name -> normalize(name).contains("ADMIN"));
    }

    private boolean isAssignedToUser(
            String responsible,
            User user,
            List<String> roleNames,
            Optional<Department> department
    ) {
        if (responsible == null || responsible.isBlank()) {
            return false;
        }

        String normalizedResponsible = normalize(responsible);

        if (user.getFullName() != null && normalizedResponsible.equals(normalize(user.getFullName()))) {
            return true;
        }
        if (normalizedResponsible.equals(normalize(user.getUsername()))) {
            return true;
        }

        for (String roleName : roleNames) {
            if (matchesResponsibleToRole(normalizedResponsible, roleName)) {
                return true;
            }
        }

        if (department.isPresent()) {
            String deptName = normalize(department.get().getName());
            if (normalizedResponsible.contains("recursos") && deptName.contains("recurso")) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesResponsibleToRole(String responsible, String roleName) {
        String role = normalize(roleName);
        if (responsible.equals(role)) {
            return true;
        }
        if (responsible.contains("funcionario")
                && (role.contains("FUNCIONARIO") || role.contains("USER") || role.contains("USUARIO"))) {
            return true;
        }
        if (responsible.contains("supervisor") && role.contains("SUPERVISOR")) {
            return true;
        }
        if (responsible.contains("recursos")
                && (role.contains("RECURSO") || role.contains("RH") || role.contains("ANALISTA"))) {
            return true;
        }
        return false;
    }

    private List<String> resolveRoleNames(User user) {
        List<String> names = new ArrayList<>();
        Set<String> roleIds = user.getRoleIds();
        if (roleIds == null) {
            return names;
        }
        for (String roleId : roleIds) {
            roleRepository.findById(roleId).map(Role::getName).ifPresent(names::add);
            roleRepository.findByNameIgnoreCase(roleId).map(Role::getName).ifPresent(names::add);
        }
        return names;
    }

    private Optional<Department> resolveDepartment(User user) {
        if (user.getDepartmentId() == null || user.getDepartmentId().isBlank()) {
            return Optional.empty();
        }
        return departmentRepository.findById(user.getDepartmentId());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("Ñ", "N");
    }
}
