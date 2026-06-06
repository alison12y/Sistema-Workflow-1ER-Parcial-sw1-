package com.workflow.politicas.service;

import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;
import com.workflow.politicas.model.User;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.model.WorkflowTransition;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.FormSubmissionRepository;
import com.workflow.politicas.repository.RoleRepository;
import com.workflow.politicas.repository.TramiteRepository;
import com.workflow.politicas.repository.UserRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import com.workflow.politicas.repository.WorkflowTransitionRepository;
import com.workflow.politicas.support.WorkflowTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba mínima: START → Actividad A → Actividad B → END (secuencial, sin formulario).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TramiteServiceSequentialAdvanceTest {

    private static final String POLICY_ID = "policy-seq";

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
        stubSequentialPolicy();
        when(tramiteRepository.save(any(Tramite.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user()));
    }

    @Test
    void sequentialFlow_completeA_createsTaskB_completeB_finalizesTramite() {
        Tramite tramite = newTramiteWithTaskA();
        when(tramiteRepository.findById("tram-seq")).thenReturn(Optional.of(tramite));

        Tramite afterA = tramiteService.advanceWithTaskCompletion(
                "tram-seq",
                "act-a",
                1,
                Map.of("observacion_cierre", "Listo A"),
                "funcionario",
                "Actividad A completada"
        );

        assertEquals("EN_PROCESO", afterA.getStatus());
        assertEquals(2, afterA.getTasks().size());
        TramiteTask taskB = afterA.getTasks().stream().filter(t -> t.getOrder() == 2).findFirst().orElseThrow();
        assertEquals("PENDIENTE", taskB.getStatus());
        assertEquals("act-b", taskB.getWorkflowActivityId());
        assertEquals("Actividad B", taskB.getName());

        taskB.setStatus("EN_CURSO");
        taskB.setTakenBy("funcionario");

        Tramite afterB = tramiteService.advanceWithTaskCompletion(
                "tram-seq",
                "act-b",
                2,
                Map.of("observacion_cierre", "Listo B"),
                "funcionario",
                "Actividad B completada"
        );

        assertEquals("COMPLETADO", afterB.getStatus());
        assertEquals("Finalizado", afterB.getCurrentActivity());
        assertTrue(afterB.getTasks().stream().allMatch(t -> "COMPLETADA".equals(t.getStatus())));

        ArgumentCaptor<Tramite> saved = ArgumentCaptor.forClass(Tramite.class);
        verify(tramiteRepository, org.mockito.Mockito.atLeast(2)).save(saved.capture());
        assertTrue(saved.getAllValues().stream().anyMatch(t -> "COMPLETADO".equals(t.getStatus())));
    }

    private void stubSequentialPolicy() {
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
        for (WorkflowActivity activity : activities) {
            when(workflowActivityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        }
    }

    private Tramite newTramiteWithTaskA() {
        Tramite tramite = new Tramite();
        tramite.setId("tram-seq");
        tramite.setCode("TRM-SEQ");
        tramite.setPolicyId(POLICY_ID);
        tramite.setStatus("EN_PROCESO");
        tramite.setCurrentWorkflowActivityId("act-a");
        tramite.setCurrentActivity("Actividad A");
        tramite.setTasks(new ArrayList<>());
        tramite.setTrace(new ArrayList<>());

        TramiteTask taskA = new TramiteTask();
        taskA.setWorkflowActivityId("act-a");
        taskA.setName("Actividad A");
        taskA.setResponsible("Operaciones");
        taskA.setStatus("EN_CURSO");
        taskA.setOrder(1);
        taskA.setTakenBy("funcionario");
        tramite.getTasks().add(taskA);
        return tramite;
    }

    private static WorkflowActivity activity(String id, String type, String name, String responsible) {
        WorkflowActivity a = WorkflowTestFixtures.activity(id, type, name, responsible);
        a.setPolicyId(POLICY_ID);
        if (responsible != null) {
            a.setResponsibleType("USER");
            a.setResponsibleId(responsible);
        }
        return a;
    }

    private static WorkflowTransition transition(String id, String type, String from, String to) {
        return WorkflowTestFixtures.transition(id, type, from, to, null);
    }

    private static User user() {
        User u = new User();
        u.setUsername("funcionario");
        u.setFullName("Funcionario Test");
        u.setActive(true);
        return u;
    }
}
