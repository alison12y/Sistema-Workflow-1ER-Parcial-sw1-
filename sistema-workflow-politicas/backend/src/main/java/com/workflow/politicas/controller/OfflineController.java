package com.workflow.politicas.controller;

import com.workflow.politicas.dto.OfflineNotifyStoredRequest;
import com.workflow.politicas.dto.OfflineNotifySyncCompletedRequest;
import com.workflow.politicas.service.OfflineAuditService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/offline")
public class OfflineController {

    private final OfflineAuditService offlineAuditService;

    public OfflineController(OfflineAuditService offlineAuditService) {
        this.offlineAuditService = offlineAuditService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "offline-sync-ok";
    }

    @PostMapping("/notify-stored")
    public void notifyStored(@RequestBody OfflineNotifyStoredRequest request, Authentication authentication) {
        offlineAuditService.notifyStored(resolveUsername(authentication), request);
    }

    @PostMapping("/notify-sync-completed")
    public void notifySyncCompleted(
            @RequestBody OfflineNotifySyncCompletedRequest request,
            Authentication authentication
    ) {
        offlineAuditService.notifySyncCompleted(resolveUsername(authentication), request);
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        return authentication.getName();
    }
}
