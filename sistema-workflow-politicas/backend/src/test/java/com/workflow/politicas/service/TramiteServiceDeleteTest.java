package com.workflow.politicas.service;

import com.workflow.politicas.model.FormSubmission;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.FormSubmissionRepository;
import com.workflow.politicas.repository.RoleRepository;
import com.workflow.politicas.repository.TramiteRepository;
import com.workflow.politicas.repository.UserRepository;
import com.workflow.politicas.repository.WorkflowActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TramiteServiceDeleteTest {

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
    private WorkflowRoutingService workflowRoutingService;
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
        tramiteService = new TramiteService(
                tramiteRepository,
                businessPolicyRepository,
                userRepository,
                roleRepository,
                workflowActivityRepository,
                workflowRoutingService,
                bitacoraService,
                formSubmissionRepository,
                formSubmissionFileService,
                documentRepositoryService
        );
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user()));
        when(formSubmissionRepository.findByTramiteId(anyString())).thenReturn(List.of());
    }

    @Test
    void delete_cancelledTramite_ok() {
        Tramite tramite = tramite("tram-1", "TRM-001", "CANCELADO");
        when(tramiteRepository.findById("tram-1")).thenReturn(Optional.of(tramite));

        tramiteService.delete("tram-1", "admin");

        verify(formSubmissionRepository).deleteByTramiteId("tram-1");
        verify(bitacoraService).deleteByEntity("Tramite", "tram-1");
        verify(bitacoraService).registrar(
                eq("admin"),
                eq("Trámites"),
                eq("ELIMINAR_TRAMITE"),
                anyString(),
                eq("Tramite"),
                eq("tram-1")
        );
        verify(tramiteRepository).deleteById("tram-1");
    }

    @Test
    void delete_cancelledEnglishVariant_ok() {
        Tramite tramite = tramite("tram-1b", "TRM-001B", "CANCELLED");
        when(tramiteRepository.findById("tram-1b")).thenReturn(Optional.of(tramite));

        tramiteService.delete("tram-1b", "admin");

        verify(tramiteRepository).deleteById("tram-1b");
    }

    @Test
    void delete_completedTramite_ok() {
        Tramite tramite = tramite("tram-2", "TRM-002", "COMPLETADO");
        when(tramiteRepository.findById("tram-2")).thenReturn(Optional.of(tramite));

        tramiteService.delete("tram-2", "admin");

        verify(tramiteRepository).deleteById("tram-2");
    }

    @Test
    void delete_inProgressTramite_returns400() {
        Tramite tramite = tramite("tram-3", "TRM-003", "EN_PROCESO");
        when(tramiteRepository.findById("tram-3")).thenReturn(Optional.of(tramite));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> tramiteService.delete("tram-3", "admin")
        );

        assertTrue(ex.getMessage().contains("Solo se pueden eliminar"));
        verify(tramiteRepository, never()).deleteById(anyString());
    }

    @Test
    void delete_initiatedTramite_returns400() {
        Tramite tramite = tramite("tram-4", "TRM-004", "INICIADO");
        when(tramiteRepository.findById("tram-4")).thenReturn(Optional.of(tramite));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> tramiteService.delete("tram-4", "admin")
        );

        assertTrue(ex.getMessage().contains("Solo se pueden eliminar"));
        verify(tramiteRepository, never()).deleteById(anyString());
    }

    @Test
    void delete_removesFormSubmissions() {
        Tramite tramite = tramite("tram-5", "TRM-005", "COMPLETADO");
        FormSubmission submission = new FormSubmission();
        submission.setTramiteId("tram-5");
        when(tramiteRepository.findById("tram-5")).thenReturn(Optional.of(tramite));
        when(formSubmissionRepository.findByTramiteId("tram-5")).thenReturn(List.of(submission));

        tramiteService.delete("tram-5", "admin");

        verify(formSubmissionRepository).deleteByTramiteId("tram-5");
    }

    private static Tramite tramite(String id, String code, String status) {
        Tramite tramite = new Tramite();
        tramite.setId(id);
        tramite.setCode(code);
        tramite.setStatus(status);
        return tramite;
    }

    private static User user() {
        User user = new User();
        user.setUsername("admin");
        user.setFullName("Administrador");
        return user;
    }
}
