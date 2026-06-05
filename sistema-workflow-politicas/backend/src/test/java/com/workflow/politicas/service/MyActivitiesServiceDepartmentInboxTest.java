package com.workflow.politicas.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyActivitiesServiceDepartmentInboxTest {

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
    }

    @Test
    void listInbox_laneNameAsRoleType_matchesDepartmentUser() {
        Department recepcion = department("dept-rec", "Departamento De Recepción");
        User user = user("ana.recep", "dept-rec", "role-func");
        Role role = role("role-func", "Funcionario");
        WorkflowActivity activity = activity("act-1", "ROLE", null, "Departamento De Recepción");
        Tramite tramite = tramiteWithTask("EN_CURSO", null);

        when(userRepository.findByUsername("ana.recep")).thenReturn(Optional.of(user));
        when(tramiteRepository.findByStatusNot("CANCELADO")).thenReturn(List.of(tramite));
        when(workflowActivityRepository.findById("act-1")).thenReturn(Optional.of(activity));
        when(roleRepository.findById("role-func")).thenReturn(Optional.of(role));
        when(departmentRepository.findById("dept-rec")).thenReturn(Optional.of(recepcion));

        List<MyActivityDto> inbox = service.listInbox("ana.recep", null);

        assertEquals(1, inbox.size());
        assertTrue(inbox.get(0).isCanTake());
    }

    private Department department(String id, String name) {
        Department d = new Department();
        d.setId(id);
        d.setName(name);
        return d;
    }

    private User user(String username, String deptId, String roleId) {
        User u = new User();
        u.setId("u1");
        u.setUsername(username);
        u.setDepartmentId(deptId);
        u.setRoleIds(Set.of(roleId));
        return u;
    }

    private Role role(String id, String name) {
        Role r = new Role();
        r.setId(id);
        r.setName(name);
        return r;
    }

    private WorkflowActivity activity(String id, String type, String respId, String respName) {
        WorkflowActivity a = new WorkflowActivity();
        a.setId(id);
        a.setResponsibleType(type);
        a.setResponsibleId(respId);
        a.setResponsibleName(respName);
        return a;
    }

    private Tramite tramiteWithTask(String status, String takenBy) {
        Tramite tramite = new Tramite();
        tramite.setId("tram-1");
        tramite.setPolicyId("p1");
        tramite.setCode("TRM-012");
        tramite.setStatus("EN_PROCESO");
        TramiteTask task = new TramiteTask();
        task.setWorkflowActivityId("act-1");
        task.setName("Recepción de bienes");
        task.setResponsible("Departamento De Recepción");
        task.setStatus(status);
        task.setOrder(1);
        task.setTakenBy(takenBy);
        tramite.setTasks(List.of(task));
        return tramite;
    }
}
