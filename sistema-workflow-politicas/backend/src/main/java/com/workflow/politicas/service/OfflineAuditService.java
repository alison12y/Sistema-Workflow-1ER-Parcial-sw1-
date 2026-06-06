package com.workflow.politicas.service;

import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.dto.OfflineNotifyStoredRequest;
import com.workflow.politicas.dto.OfflineNotifySyncCompletedRequest;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class OfflineAuditService {

    private final BitacoraService bitacoraService;

    public OfflineAuditService(BitacoraService bitacoraService) {
        this.bitacoraService = bitacoraService;
    }

    public void notifyStored(String username, OfflineNotifyStoredRequest request) {
        if (request == null || request.getPendingCount() <= 0) {
            return;
        }
        String types = request.getTypes() == null || request.getTypes().isEmpty()
                ? "operaciones"
                : request.getTypes().stream().collect(Collectors.joining(", "));
        bitacoraService.registrar(
                username,
                AuditModules.OFFLINE,
                AuditActions.OFFLINE_DATA_STORED,
                "Datos almacenados localmente: "
                        + request.getPendingCount()
                        + " elemento(s) en cola ("
                        + types
                        + ")",
                "OfflineQueue",
                null
        );
    }

    public void notifySyncCompleted(String username, OfflineNotifySyncCompletedRequest request) {
        if (request == null) {
            return;
        }
        bitacoraService.registrar(
                username,
                AuditModules.OFFLINE,
                AuditActions.OFFLINE_SYNC_COMPLETED,
                "Sincronización offline completada: "
                        + request.getSyncedCount()
                        + " exitosa(s), "
                        + request.getFailedCount()
                        + " fallida(s)",
                "OfflineQueue",
                null
        );
    }
}
