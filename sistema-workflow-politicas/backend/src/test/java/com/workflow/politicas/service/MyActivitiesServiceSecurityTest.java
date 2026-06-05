package com.workflow.politicas.service;

import com.workflow.politicas.dto.CompleteActivityRequest;
import com.workflow.politicas.model.*;
import com.workflow.politicas.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MyActivitiesServiceSecurityTest {

    @Mock
    private TramiteRepository tramiteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private WorkflowActivityRepository workflowActivityRepository;
    @Mock
    private FormSubmissionService formSubmissionService;
    @Mock
    private TramiteService tramiteService;
    @Mock
    private WorkflowRoutingService workflowRoutingService;

    private MyActivitiesService service;

    @BeforeEach
    void setUp() {
        service = new MyActivitiesService(
                tramiteRepository,
                userRepository,
                roleRepository,
                departmentRepository,
                workflowActivityRepository,
                formSubmissionService,
                tramiteService,
                workflowRoutingService
        );
        when(workflowRoutingService.resolveCanonicalActivityId(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    String id = invocation.getArgument(1);
                    return id != null ? id : "act-1";
                });
    }

    @Test
    void completeActivity_foreignUserForbidden() {
        User user = user("ana", "role-func");
        Tramite tramite = tramiteWithTask("t-other", "EN_CURSO");
        WorkflowActivity activity = activityForOtherUser();

        when(userRepository.findByUsername("ana")).thenReturn(Optional.of(user));
        when(tramiteRepository.findById("tram-1")).thenReturn(Optional.of(tramite));
        when(workflowActivityRepository.findById("act-1")).thenReturn(Optional.of(activity));
        when(roleRepository.findById("role-func")).thenReturn(Optional.of(role("Funcionario")));

        CompleteActivityRequest request = new CompleteActivityRequest();
        request.setTaskOrder(1);
        request.setResponses(List.of());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.completeActivity("tram-1", request, "ana")
        );
        assertTrue(ex.getMessage().contains("permisos"));
        verify(formSubmissionService, never()).save(any(), anyString(), anyBoolean());
    }

    @Test
    void takeTask_foreignUserForbidden() {
        User user = user("ana", "role-func");
        Tramite tramite = tramiteWithTask("t-other", "PENDIENTE");
        WorkflowActivity activity = activityForOtherUser();

        when(userRepository.findByUsername("ana")).thenReturn(Optional.of(user));
        when(tramiteRepository.findById("tram-1")).thenReturn(Optional.of(tramite));
        when(workflowActivityRepository.findById("act-1")).thenReturn(Optional.of(activity));
        when(roleRepository.findById("role-func")).thenReturn(Optional.of(role("Funcionario")));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.takeTask("tram-1", 1, "ana")
        );
        assertTrue(ex.getMessage().contains("permisos"));
    }

    private User user(String username, String roleId) {
        User u = new User();
        u.setId("u1");
        u.setUsername(username);
        u.setRoleIds(Set.of(roleId));
        return u;
    }

    private Role role(String name) {
        Role r = new Role();
        r.setId("role-func");
        r.setName(name);
        return r;
    }

    private WorkflowActivity activityForOtherUser() {
        WorkflowActivity a = new WorkflowActivity();
        a.setId("act-1");
        a.setResponsibleType("USER");
        a.setResponsibleId("carlos.mendoza");
        a.setResponsibleName("carlos.mendoza");
        return a;
    }

    private Tramite tramiteWithTask(String responsible, String taskStatus) {
        Tramite tramite = new Tramite();
        tramite.setId("tram-1");
        tramite.setPolicyId("p1");
        tramite.setStatus("EN_PROCESO");
        TramiteTask task = new TramiteTask();
        task.setWorkflowActivityId("act-1");
        task.setName("Tarea");
        task.setResponsible(responsible);
        task.setStatus(taskStatus);
        task.setOrder(1);
        tramite.setTasks(List.of(task));
        return tramite;
    }
}
