package com.workflow.politicas.service;

import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.dto.TramiteCreateRequest;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;
import com.workflow.politicas.model.User;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.FormSubmissionRepository;
import com.workflow.politicas.repository.RoleRepository;
import com.workflow.politicas.repository.TramiteRepository;
import com.workflow.politicas.repository.UserRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import com.workflow.politicas.repository.WorkflowTransitionRepository;
import com.workflow.politicas.model.WorkflowTransition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TramiteServiceAuditTest {

    private static final String POLICY_ID = "policy-audit";

    @Mock
    private TramiteRepository tramiteRepository;
    @Mock
    private BusinessPolicyRepository businessPolicyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private WorkflowActivityRepository workflowActivityRepository;
    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;
    @Mock
    private BitacoraService bitacoraService;
    @Mock
    private FormSubmissionRepository formSubmissionRepository;
    @Mock
    private FormSubmissionFileService formSubmissionFileService;
    @Mock
    private DocumentRepositoryService documentRepositoryService;

    private TramiteService tramiteService;

    @BeforeEach
    void setUp() {
        WorkflowRoutingService routingService = new WorkflowRoutingService(
                workflowActivityRepository,
                workflowTransitionRepository,
                new WorkflowConditionEvaluator()
        );
        tramiteService = new TramiteService(
                tramiteRepository,
                businessPolicyRepository,
                userRepository,
                roleRepository,
                workflowActivityRepository,
                routingService,
                bitacoraService,
                formSubmissionRepository,
                formSubmissionFileService,
                documentRepositoryService
        );
        stubPolicy();
        when(tramiteRepository.save(any(Tramite.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByUsername("ana.user")).thenReturn(Optional.of(requester()));
    }

    @Test
    void create_registersIniciarTramiteAuditEvent() {
        TramiteCreateRequest request = new TramiteCreateRequest();
        request.setPolicyId(POLICY_ID);
        request.setDescription("Solicitud de prueba");
        request.setPriority("NORMAL");
        request.setRequestedBy("ana.user");

        tramiteService.create(request, "ana.user");

        verify(bitacoraService).registrar(
                eq("ana.user"),
                eq("Trámites"),
                eq(AuditActions.INICIAR_TRAMITE),
                org.mockito.ArgumentMatchers.contains("creó el trámite"),
                eq("Tramite"),
                any()
        );
    }

    @Test
    void recordFormSubmitted_registersCompletarActividadAuditEvent() {
        Tramite tramite = tramiteWithTaskA();
        when(tramiteRepository.findById("tram-audit")).thenReturn(Optional.of(tramite));

        tramiteService.recordFormSubmittedTrace(
                "tram-audit",
                "ana.user",
                "act-a",
                "Actividad A",
                1
        );

        verify(bitacoraService).registrar(
                eq("ana.user"),
                eq("Mis actividades"),
                eq(AuditActions.COMPLETAR_ACTIVIDAD),
                org.mockito.ArgumentMatchers.contains("envió el formulario"),
                eq("Tramite"),
                eq("tram-audit")
        );
    }

    private void stubPolicy() {
        BusinessPolicy policy = new BusinessPolicy();
        policy.setId(POLICY_ID);
        policy.setName("Política prueba");
        policy.setStatus("ACTIVE");
        when(businessPolicyRepository.findById(POLICY_ID)).thenReturn(Optional.of(policy));

        List<WorkflowActivity> activities = List.of(
                activity("start", "START", "Inicio", null),
                activity("act-a", "TASK", "Actividad A", "Operaciones"),
                activity("act-b", "TASK", "Actividad B", "Operaciones"),
                activity("end", "END", "Fin", null)
        );
        List<WorkflowTransition> transitions = List.of(
                transition("tr1", "SEQUENTIAL", "start", "act-a"),
                transition("tr2", "SEQUENTIAL", "act-a", "act-b"),
                transition("tr3", "SEQUENTIAL", "act-b", "end")
        );
        when(workflowActivityRepository.findByPolicyIdOrderByOrderIndexAsc(POLICY_ID)).thenReturn(activities);
        when(workflowTransitionRepository.findByPolicyIdOrderByOrderIndexAsc(POLICY_ID)).thenReturn(transitions);
        for (WorkflowActivity act : activities) {
            when(workflowActivityRepository.findById(act.getId())).thenReturn(Optional.of(act));
        }
    }

    private static WorkflowActivity activity(String id, String type, String name, String responsible) {
        WorkflowActivity a = new WorkflowActivity();
        a.setId(id);
        a.setPolicyId(POLICY_ID);
        a.setActivityType(type);
        a.setName(name);
        a.setResponsibleName(responsible);
        a.setActive(true);
        a.setOrderIndex(1);
        return a;
    }

    private static WorkflowTransition transition(String id, String type, String from, String to) {
        WorkflowTransition t = new WorkflowTransition();
        t.setId(id);
        t.setPolicyId(POLICY_ID);
        t.setTransitionType(type);
        t.setFromActivityId(from);
        t.setToActivityId(to);
        t.setActive(true);
        t.setOrderIndex(1);
        return t;
    }

    private static User requester() {
        User user = new User();
        user.setId("user-ana");
        user.setUsername("ana.user");
        user.setFullName("Ana Usuario");
        user.setActive(true);
        return user;
    }

    private static Tramite tramiteWithTaskA() {
        Tramite tramite = new Tramite();
        tramite.setId("tram-audit");
        tramite.setCode("TRM-AUDIT");
        tramite.setPolicyId(POLICY_ID);
        tramite.setStatus("EN_PROCESO");
        tramite.setTasks(new ArrayList<>());
        tramite.setTrace(new ArrayList<>());

        TramiteTask task = new TramiteTask();
        task.setWorkflowActivityId("act-a");
        task.setName("Actividad A");
        task.setOrder(1);
        task.setStatus("EN_CURSO");
        task.setTakenBy("ana.user");
        tramite.getTasks().add(task);
        tramite.setCurrentWorkflowActivityId("act-a");
        tramite.setCurrentActivity("Actividad A");
        return tramite;
    }
}
