package com.workflow.politicas.service;

import com.workflow.politicas.dto.AiFormAssistTraceRequest;
import com.workflow.politicas.dto.CompleteActivityRequest;
import com.workflow.politicas.dto.FormSubmissionRequest;
import com.workflow.politicas.dto.MyActivitiesFilter;
import com.workflow.politicas.dto.MyActivityDto;
import com.workflow.politicas.model.Department;
import com.workflow.politicas.model.Role;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;
import com.workflow.politicas.model.User;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.repository.DepartmentRepository;
import com.workflow.politicas.repository.RoleRepository;
import com.workflow.politicas.repository.TramiteRepository;
import com.workflow.politicas.repository.UserRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bandeja del funcionario: todas las tareas asignadas por USER / ROLE / DEPARTMENT.
 */
@Service
public class MyActivitiesService {

    private static final String TASK_PENDIENTE = "PENDIENTE";
    private static final String TASK_EN_CURSO = "EN_CURSO";
    private static final String TASK_COMPLETADA = "COMPLETADA";
    private static final String STATUS_COMPLETADO = "COMPLETADO";
    private static final String STATUS_CANCELADO = "CANCELADO";
    private static final String CATEGORY_NORMAL = "NORMAL";
    private static final String CATEGORY_OBSERVADA = "OBSERVADA";
    private static final String CATEGORY_ERROR = "ERROR";

    private final TramiteRepository tramiteRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final WorkflowActivityRepository workflowActivityRepository;
    private final FormSubmissionService formSubmissionService;
    private final TramiteService tramiteService;
    private final WorkflowRoutingService workflowRoutingService;

    public MyActivitiesService(
            TramiteRepository tramiteRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            WorkflowActivityRepository workflowActivityRepository,
            FormSubmissionService formSubmissionService,
            TramiteService tramiteService,
            WorkflowRoutingService workflowRoutingService
    ) {
        this.tramiteRepository = tramiteRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.workflowActivityRepository = workflowActivityRepository;
        this.formSubmissionService = formSubmissionService;
        this.tramiteService = tramiteService;
        this.workflowRoutingService = workflowRoutingService;
    }

    public List<MyActivityDto> listInbox(String username, MyActivitiesFilter filter) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<Role> roles = resolveRoles(user);
        List<String> roleNames = roles.stream().map(Role::getName).toList();
        Optional<Department> department = resolveDepartment(user);
        boolean admin = isAdmin(roleNames);

        List<MyActivityDto> items = new ArrayList<>();
        for (Tramite tramite : loadTramitesForInbox(filter)) {
            if (tramite.getTasks() == null || tramite.getTasks().isEmpty()) {
                continue;
            }

            boolean tramiteClosed = isClosed(tramite.getStatus());

            for (TramiteTask task : tramite.getTasks()) {
                if (task.getStatus() == null) {
                    continue;
                }

                String taskStatus = task.getStatus().trim().toUpperCase(Locale.ROOT);

                if (tramiteClosed && !TASK_COMPLETADA.equals(taskStatus)) {
                    continue;
                }

                if (!TASK_PENDIENTE.equals(taskStatus)
                        && !TASK_EN_CURSO.equals(taskStatus)
                        && !TASK_COMPLETADA.equals(taskStatus)) {
                    continue;
                }

                WorkflowActivity activity = resolveWorkflowActivity(task);
                if (!admin && !isAssignedToUser(activity, task, user, roles, department, username)) {
                    continue;
                }

                MyActivityDto dto = toActivityDto(tramite, task, activity, user.getUsername());
                if (!matchesFilter(dto, filter)) {
                    continue;
                }
                items.add(dto);
            }
        }

        items.sort(Comparator
                .comparing(MyActivityDto::getAssignedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MyActivityDto::getTaskOrder));

        return items;
    }

    public List<MyActivityDto> listForUser(String username) {
        return listInbox(username, null);
    }

    public Optional<MyActivityDto> findForUser(String tramiteId, int taskOrder, String username) {
        return listForUser(username).stream()
                .filter(item -> tramiteId.equals(item.getTramiteId()) && item.getTaskOrder() == taskOrder)
                .findFirst();
    }

    public Optional<MyActivityDto> findForUser(String tramiteId, String username) {
        return listForUser(username).stream()
                .filter(item -> tramiteId.equals(item.getTramiteId()) && item.isCanComplete())
                .findFirst()
                .or(() -> listForUser(username).stream()
                        .filter(item -> tramiteId.equals(item.getTramiteId()))
                        .findFirst());
    }

    public Tramite takeTask(String tramiteId, int taskOrder, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Tramite tramite = tramiteRepository.findById(tramiteId)
                .orElseThrow(() -> new IllegalArgumentException("Trámite no encontrado"));

        if (isClosed(tramite.getStatus())) {
            throw new IllegalArgumentException("El trámite ya está finalizado o cancelado");
        }

        TramiteTask task = findTaskByOrder(tramite, taskOrder)
                .orElseThrow(() -> new IllegalArgumentException("La tarea indicada no existe"));

        if (!TASK_PENDIENTE.equals(task.getStatus())) {
            throw new IllegalArgumentException("La tarea no está pendiente de tomar");
        }

        List<Role> roles = resolveRoles(user);
        List<String> roleNames = roles.stream().map(Role::getName).toList();
        Optional<Department> department = resolveDepartment(user);
        WorkflowActivity activity = resolveWorkflowActivity(task);

        if (!isAdmin(roleNames) && !isAssignedToUser(activity, task, user, roles, department, username)) {
            throw new IllegalArgumentException("No tiene permisos para tomar esta tarea");
        }

        return tramiteService.takeWorkflowTask(tramiteId, taskOrder, username);
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

        int taskOrder = request.getTaskOrder();
        TramiteTask task = findTaskByOrder(tramite, taskOrder)
                .orElseThrow(() -> new IllegalArgumentException("La tarea indicada no existe"));

        if (!TASK_EN_CURSO.equals(task.getStatus())) {
            throw new IllegalArgumentException("Debe tomar la tarea antes de completarla, o la tarea ya fue finalizada");
        }

        if (request.getActivityName() != null
                && !request.getActivityName().trim().isEmpty()
                && !task.getName().equalsIgnoreCase(request.getActivityName().trim())) {
            throw new IllegalArgumentException("La actividad no corresponde al trámite");
        }

        List<Role> roles = resolveRoles(user);
        List<String> roleNames = roles.stream().map(Role::getName).toList();
        Optional<Department> department = resolveDepartment(user);
        WorkflowActivity activity = resolveWorkflowActivity(task);

        if (!isAdmin(roleNames) && !isAssignedToUser(activity, task, user, roles, department, username)) {
            throw new IllegalArgumentException("No tiene permisos para completar esta actividad");
        }

        if (!isAdmin(roleNames)
                && (task.getTakenBy() == null
                || task.getTakenBy().isBlank()
                || !task.getTakenBy().equalsIgnoreCase(username))) {
            throw new IllegalArgumentException("Debe tomar la tarea antes de completarla");
        }

        String workflowActivityId = task.getWorkflowActivityId();
        if (workflowActivityId == null || workflowActivityId.isBlank()) {
            workflowActivityId = workflowRoutingService.resolveCanonicalActivityId(
                    tramite.getPolicyId(), null, task.getName());
        } else {
            workflowActivityId = workflowRoutingService.resolveCanonicalActivityId(
                    tramite.getPolicyId(), workflowActivityId, task.getName());
        }
        if (workflowActivityId == null || workflowActivityId.isBlank()) {
            throw new IllegalStateException(
                    "La tarea no tiene actividad de workflow asociada. Configure el diagrama UML de la política.");
        }
        if (!workflowActivityId.equals(task.getWorkflowActivityId())) {
            task.setWorkflowActivityId(workflowActivityId);
        }

        com.workflow.politicas.util.Cu7WorkflowDebugLog.advance(
                "completeActivity tramiteId={} codigo={} taskOrder={} workflowActivityId={} task.status={} takenBy={} policyId={}",
                tramiteId,
                tramite.getCode(),
                taskOrder,
                workflowActivityId,
                task.getStatus(),
                task.getTakenBy(),
                tramite.getPolicyId()
        );

        String activityName = task.getName();
        formSubmissionService.validateResponsesForWorkflowActivity(
                workflowActivityId,
                tramite.getPolicyId(),
                request.getResponses(),
                true
        );

        FormSubmissionRequest submissionRequest = new FormSubmissionRequest();
        submissionRequest.setTramiteId(tramiteId);
        submissionRequest.setPolicyId(tramite.getPolicyId());
        submissionRequest.setWorkflowActivityId(workflowActivityId);
        submissionRequest.setActivityName(activityName);
        submissionRequest.setTaskOrder(taskOrder);
        submissionRequest.setResponses(request.getResponses());
        formSubmissionService.save(submissionRequest, username, true);

        tramiteService.recordFormSubmittedTrace(
                tramiteId,
                username,
                workflowActivityId,
                activityName,
                taskOrder
        );

        Map<String, Object> stepData = formSubmissionService.buildStepDataForWorkflowActivity(
                workflowActivityId,
                request.getResponses()
        );
        com.workflow.politicas.util.Cu7WorkflowDebugLog.log(
                "completeActivity tramite={} taskOrder={} workflowActivityId={} stepData={}",
                tramiteId,
                taskOrder,
                workflowActivityId,
                com.workflow.politicas.util.Cu7WorkflowDebugLog.stepDataSummary(stepData)
        );
        formSubmissionService.validateStepDataForWorkflowCompletion(
                workflowActivityId,
                tramite.getPolicyId(),
                stepData
        );
        String comment = formSubmissionService.buildCompletionComment(activityName, request.getResponses());

        return tramiteService.advanceWithTaskCompletion(
                tramiteId,
                task.getWorkflowActivityId(),
                taskOrder,
                stepData,
                username,
                comment
        );
    }

    public void recordAiFormAssisted(String tramiteId, AiFormAssistTraceRequest request, String username) {
        if (request.getWorkflowActivityId() == null || request.getWorkflowActivityId().isBlank()) {
            throw new IllegalArgumentException("workflowActivityId es obligatorio");
        }
        int taskOrder = request.getTaskOrder() != null ? request.getTaskOrder() : 0;
        if (taskOrder <= 0) {
            throw new IllegalArgumentException("taskOrder es obligatorio");
        }
        findForUser(tramiteId, taskOrder, username)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no disponible para el usuario"));

        String activityName = request.getActivityName() != null ? request.getActivityName() : "Actividad";
        int suggested = request.getFieldsSuggested() != null ? request.getFieldsSuggested() : 0;
        int applied = request.getFieldsApplied() != null ? request.getFieldsApplied() : 0;

        tramiteService.recordAiFormAssistedTrace(
                tramiteId,
                username,
                request.getWorkflowActivityId(),
                activityName,
                taskOrder,
                suggested,
                applied
        );
    }

    private boolean matchesFilter(MyActivityDto dto, MyActivitiesFilter filter) {
        if (filter == null) {
            return true;
        }
        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            String wanted = filter.getStatus().trim().toUpperCase(Locale.ROOT);
            if ("OBSERVADA".equals(wanted)) {
                if (!CATEGORY_OBSERVADA.equals(dto.getInboxCategory())) {
                    return false;
                }
            } else if ("ERROR".equals(wanted)) {
                if (!CATEGORY_ERROR.equals(dto.getInboxCategory())) {
                    return false;
                }
            } else if (!wanted.equals(dto.getStatus())) {
                return false;
            }
        }
        if (filter.getPolicyId() != null && !filter.getPolicyId().isBlank()
                && !filter.getPolicyId().equals(dto.getPolicyId())) {
            return false;
        }
        if (filter.getTramiteId() != null && !filter.getTramiteId().isBlank()
                && !filter.getTramiteId().equals(dto.getTramiteId())) {
            return false;
        }
        if (filter.getTramiteCode() != null && !filter.getTramiteCode().isBlank()) {
            String code = dto.getCode() != null ? dto.getCode().toUpperCase(Locale.ROOT) : "";
            if (!code.contains(filter.getTramiteCode().trim().toUpperCase(Locale.ROOT))) {
                return false;
            }
        }
        if (filter.getPriority() != null && !filter.getPriority().isBlank()
                && !filter.getPriority().equalsIgnoreCase(dto.getPriority())) {
            return false;
        }
        return true;
    }

    private MyActivityDto toActivityDto(
            Tramite tramite,
            TramiteTask task,
            WorkflowActivity activity,
            String currentUsername
    ) {
        MyActivityDto dto = new MyActivityDto();
        dto.setTramiteId(tramite.getId());
        dto.setPolicyId(tramite.getPolicyId());
        dto.setTaskOrder(task.getOrder());
        dto.setCode(tramite.getCode());
        dto.setPolicyName(tramite.getPolicyName());
        dto.setActivityName(task.getName());
        dto.setWorkflowActivityId(task.getWorkflowActivityId());
        dto.setStatus(task.getStatus());
        dto.setResponsible(task.getResponsible());
        dto.setPriority(tramite.getPriority());
        dto.setTramiteStatus(tramite.getStatus());
        dto.setTakenBy(task.getTakenBy());
        dto.setTakenAt(task.getTakenAt());
        dto.setCompletedAt(task.getCompletedAt());
        dto.setAssignedAt(resolveAssignedAt(task, tramite));
        dto.setWorkflowError(tramite.getWorkflowError());

        String category = resolveCategory(tramite, task);
        dto.setInboxCategory(category);

        boolean closed = isClosed(tramite.getStatus());
        boolean unclaimed = task.getTakenBy() == null || task.getTakenBy().isBlank();
        boolean canClaim = TASK_PENDIENTE.equals(task.getStatus())
                || (TASK_EN_CURSO.equals(task.getStatus()) && unclaimed);
        dto.setCanTake(canClaim && !closed);
        dto.setCanComplete(
                TASK_EN_CURSO.equals(task.getStatus())
                        && !closed
                        && task.getTakenBy() != null
                        && task.getTakenBy().equalsIgnoreCase(currentUsername)
        );

        return dto;
    }

    private LocalDateTime resolveAssignedAt(TramiteTask task, Tramite tramite) {
        if (task.getTakenAt() != null) {
            return task.getTakenAt();
        }
        if (task.getStartedAt() != null) {
            return task.getStartedAt();
        }
        return tramite.getCreatedAt();
    }

    private String resolveCategory(Tramite tramite, TramiteTask task) {
        if (tramite.getWorkflowError() != null && !tramite.getWorkflowError().isBlank()
                && (TASK_PENDIENTE.equals(task.getStatus()) || TASK_EN_CURSO.equals(task.getStatus()))) {
            return CATEGORY_ERROR;
        }
        if (task.getName() != null && task.getName().toLowerCase(Locale.ROOT).contains("observ")) {
            return CATEGORY_OBSERVADA;
        }
        return CATEGORY_NORMAL;
    }

    private WorkflowActivity resolveWorkflowActivity(TramiteTask task) {
        if (task.getWorkflowActivityId() != null && !task.getWorkflowActivityId().isBlank()) {
            return workflowActivityRepository.findById(task.getWorkflowActivityId()).orElse(null);
        }
        return null;
    }

    private boolean isAssignedToUser(
            WorkflowActivity activity,
            TramiteTask task,
            User user,
            List<Role> roles,
            Optional<Department> department,
            String username
    ) {
        if (task.getTakenBy() != null && !task.getTakenBy().isBlank()) {
            return task.getTakenBy().equalsIgnoreCase(username);
        }

        if (activity != null && activity.getResponsibleType() != null && !activity.getResponsibleType().isBlank()) {
            String type = normalize(activity.getResponsibleType());
            boolean typed = switch (type) {
                case "USER" -> matchesUser(activity.getResponsibleId(), activity.getResponsibleName(), user);
                case "ROLE" -> matchesRole(activity.getResponsibleId(), activity.getResponsibleName(), roles);
                case "DEPARTMENT" -> matchesDepartment(activity.getResponsibleId(), activity.getResponsibleName(), department);
                default -> false;
            };
            if (typed) {
                return true;
            }
            if ("ROLE".equals(type) || type.isEmpty()) {
                return matchesDepartmentByResponsibleLabel(activity.getResponsibleName(), department)
                        || matchesDepartmentByResponsibleLabel(task.getResponsible(), department)
                        || matchesLegacyResponsible(task.getResponsible(), user, roles, department);
            }
            return matchesLegacyResponsible(task.getResponsible(), user, roles, department);
        }

        if (activity != null) {
            if (matchesDepartmentByResponsibleLabel(activity.getResponsibleName(), department)) {
                return true;
            }
        }
        return matchesLegacyResponsible(task.getResponsible(), user, roles, department);
    }

    private boolean matchesDepartmentByResponsibleLabel(
            String label,
            Optional<Department> userDepartment
    ) {
        if (label == null || label.isBlank() || userDepartment.isEmpty()) {
            return false;
        }
        return matchesDepartment(null, label, userDepartment)
                || resolveDepartmentByName(label)
                        .map(resolved -> resolved.getId().equals(userDepartment.get().getId()))
                        .orElse(false);
    }

    private Optional<Department> resolveDepartmentByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String trimmed = name.trim();
        Optional<Department> exact = departmentRepository.findByNameIgnoreCase(trimmed);
        if (exact.isPresent()) {
            return exact;
        }
        String norm = normalize(trimmed);
        return departmentRepository.findAll().stream()
                .filter(d -> d.getName() != null)
                .filter(d -> {
                    String dn = normalize(d.getName());
                    return dn.equals(norm) || dn.contains(norm) || norm.contains(dn);
                })
                .findFirst();
    }

    private boolean matchesUser(String responsibleId, String responsibleName, User user) {
        if (responsibleId != null && !responsibleId.isBlank()) {
            if (responsibleId.equals(user.getId()) || responsibleId.equalsIgnoreCase(user.getUsername())) {
                return true;
            }
        }
        if (responsibleName != null && !responsibleName.isBlank()) {
            String norm = normalize(responsibleName);
            if (norm.equals(normalize(user.getUsername()))) {
                return true;
            }
            if (user.getFullName() != null && norm.equals(normalize(user.getFullName()))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesRole(String responsibleId, String responsibleName, List<Role> roles) {
        for (Role role : roles) {
            if (responsibleId != null && !responsibleId.isBlank()) {
                if (responsibleId.equals(role.getId()) || responsibleId.equalsIgnoreCase(role.getName())) {
                    return true;
                }
            }
            if (responsibleName != null && !responsibleName.isBlank()) {
                if (normalize(responsibleName).equals(normalize(role.getName()))) {
                    return true;
                }
                if (matchesResponsibleToRole(normalize(responsibleName), role.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesDepartment(
            String responsibleId,
            String responsibleName,
            Optional<Department> department
    ) {
        if (department.isEmpty()) {
            return false;
        }
        Department dept = department.get();
        if (responsibleId != null && !responsibleId.isBlank() && responsibleId.equals(dept.getId())) {
            return true;
        }
        if (responsibleName != null && !responsibleName.isBlank()) {
            return normalize(responsibleName).equals(normalize(dept.getName()))
                    || normalize(responsibleName).contains(normalize(dept.getName()))
                    || normalize(dept.getName()).contains(normalize(responsibleName));
        }
        return false;
    }

    private boolean matchesLegacyResponsible(
            String responsible,
            User user,
            List<Role> roles,
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
        for (Role role : roles) {
            if (matchesResponsibleToRole(normalizedResponsible, role.getName())) {
                return true;
            }
        }
        if (department.isPresent()) {
            String deptName = normalize(department.get().getName());
            if (normalizedResponsible.contains("recursos") && deptName.contains("recurso")) {
                return true;
            }
            if (normalizedResponsible.equals(deptName) || deptName.contains(normalizedResponsible)) {
                return true;
            }
        }
        return false;
    }

    private Optional<TramiteTask> findTaskByOrder(Tramite tramite, int order) {
        if (tramite.getTasks() == null) {
            return Optional.empty();
        }
        return tramite.getTasks().stream()
                .filter(t -> t.getOrder() == order)
                .findFirst();
    }

    /**
     * F10.2: evita {@code findAll()} — usa índices por policyId/status.
     */
    private List<Tramite> loadTramitesForInbox(MyActivitiesFilter filter) {
        if (filter != null && filter.getTramiteId() != null && !filter.getTramiteId().isBlank()) {
            return tramiteRepository.findById(filter.getTramiteId().trim())
                    .map(List::of)
                    .orElse(List.of());
        }
        if (filter != null && filter.getPolicyId() != null && !filter.getPolicyId().isBlank()) {
            return tramiteRepository.findByPolicyIdAndStatusIn(
                    filter.getPolicyId().trim(),
                    List.of("INICIADO", "EN_PROCESO", STATUS_COMPLETADO, STATUS_CANCELADO)
            );
        }
        return tramiteRepository.findByStatusNot(STATUS_CANCELADO);
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

    private boolean matchesResponsibleToRole(String responsible, String roleName) {
        String role = normalize(roleName);
        if (responsible.equals(role)) {
            return true;
        }
        if (responsible.contains("FUNCIONARIO")
                && (role.contains("FUNCIONARIO") || role.contains("ATENCION") || role.contains("CLIENTE"))) {
            return true;
        }
        if (responsible.contains("ATENCION") && role.contains("ATENCION")) {
            return true;
        }
        if (responsible.contains("TECNICO") && role.contains("TECNICO")) {
            return true;
        }
        if (responsible.contains("LEGAL") && role.contains("LEGAL")) {
            return true;
        }
        if (responsible.contains("SUPERVISOR") && role.contains("SUPERVISOR")) {
            return true;
        }
        if (responsible.contains("DUENO") && role.contains("DUENO")) {
            return true;
        }
        return false;
    }

    private List<Role> resolveRoles(User user) {
        List<Role> roles = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Set<String> roleIds = user.getRoleIds();
        if (roleIds == null) {
            return roles;
        }
        for (String roleId : roleIds) {
            roleRepository.findById(roleId).ifPresent(role -> addRole(roles, seen, role));
            roleRepository.findByNameIgnoreCase(roleId).ifPresent(role -> addRole(roles, seen, role));
        }
        return roles;
    }

    private void addRole(List<Role> roles, Set<String> seen, Role role) {
        if (role.getId() != null && seen.add(role.getId())) {
            roles.add(role);
        }
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
