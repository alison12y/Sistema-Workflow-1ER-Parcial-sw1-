package com.workflow.politicas.service;

import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.dto.OfflineNotifyStoredRequest;
import com.workflow.politicas.dto.OfflineNotifySyncCompletedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OfflineAuditServiceTest {

    @Mock
    private BitacoraService bitacoraService;

    private OfflineAuditService offlineAuditService;

    @BeforeEach
    void setUp() {
        offlineAuditService = new OfflineAuditService(bitacoraService);
    }

    @Test
    void notifyStored_registersAuditWhenPendingCountPositive() {
        OfflineNotifyStoredRequest request = new OfflineNotifyStoredRequest();
        request.setPendingCount(3);
        request.setTypes(List.of("TAKE_TASK", "FORM_DRAFT"));

        offlineAuditService.notifyStored("ana.rodriguez", request);

        verify(bitacoraService).registrar(
                eq("ana.rodriguez"),
                eq(AuditModules.OFFLINE),
                eq(AuditActions.OFFLINE_DATA_STORED),
                eq("Datos almacenados localmente: 3 elemento(s) en cola (TAKE_TASK, FORM_DRAFT)"),
                eq("OfflineQueue"),
                eq(null)
        );
    }

    @Test
    void notifyStored_skipsWhenEmpty() {
        OfflineNotifyStoredRequest request = new OfflineNotifyStoredRequest();
        request.setPendingCount(0);

        offlineAuditService.notifyStored("ana.rodriguez", request);

        verifyNoInteractions(bitacoraService);
    }

    @Test
    void notifySyncCompleted_registersAudit() {
        OfflineNotifySyncCompletedRequest request = new OfflineNotifySyncCompletedRequest();
        request.setSyncedCount(2);
        request.setFailedCount(1);

        offlineAuditService.notifySyncCompleted("ana.rodriguez", request);

        verify(bitacoraService).registrar(
                eq("ana.rodriguez"),
                eq(AuditModules.OFFLINE),
                eq(AuditActions.OFFLINE_SYNC_COMPLETED),
                eq("Sincronización offline completada: 2 exitosa(s), 1 fallida(s)"),
                eq("OfflineQueue"),
                eq(null)
        );
    }
}
