package com.workflow.politicas.service;

import com.workflow.politicas.dto.WorkflowRoutingResult;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;
import com.workflow.politicas.model.WorkflowActivity;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.FormSubmissionRepository;
import com.workflow.politicas.repository.RoleRepository;
import com.workflow.politicas.repository.TramiteRepository;
import com.workflow.politicas.repository.UserRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import com.workflow.politicas.support.WorkflowTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TramiteServiceIterationLimitTest {

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
                null,
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
    }

    @Test
    void applyRoutingResult_blocksExcessiveIterations() throws Exception {
        Tramite tramite = new Tramite();
        tramite.setId("t1");
        tramite.setPolicyId("p1");
        tramite.setStatus("EN_PROCESO");
        tramite.setTasks(new ArrayList<>());
        tramite.setTrace(new ArrayList<>());
        for (int i = 0; i < 20; i++) {
            TramiteTask task = new TramiteTask();
            task.setWorkflowActivityId("loop");
            task.setStatus("COMPLETADA");
            task.setOrder(i + 1);
            tramite.getTasks().add(task);
        }

        WorkflowActivity loop = WorkflowTestFixtures.activity("loop", "TASK", "Revisión", "Ops");
        WorkflowRoutingResult routing = WorkflowRoutingResult.activate(
                List.of(loop),
                null,
                "iteración"
        );

        var method = TramiteService.class.getDeclaredMethod(
                "applyRoutingResult",
                Tramite.class,
                WorkflowRoutingResult.class,
                java.util.Map.class,
                String.class,
                String.class,
                String.class,
                java.time.LocalDateTime.class
        );
        method.setAccessible(true);

        Exception ex = assertThrows(Exception.class, () -> method.invoke(
                tramiteService,
                tramite,
                routing,
                java.util.Map.of(),
                "ana",
                "Ana",
                "EN_PROCESO",
                java.time.LocalDateTime.now()
        ));
        Throwable cause = ex instanceof java.lang.reflect.InvocationTargetException ite
                ? ite.getCause()
                : ex;
        assertTrue(cause instanceof IllegalStateException);
        assertTrue(cause.getMessage().contains("límite"));
    }
}
