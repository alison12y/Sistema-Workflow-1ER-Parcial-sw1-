package com.workflow.politicas.controller;

import com.workflow.politicas.dto.CompleteActivityRequest;
import com.workflow.politicas.dto.MyActivityDto;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.service.MyActivitiesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/my-activities")
public class MyActivitiesController {

    private final MyActivitiesService myActivitiesService;

    public MyActivitiesController(MyActivitiesService myActivitiesService) {
        this.myActivitiesService = myActivitiesService;
    }

    @GetMapping
    public List<MyActivityDto> list(Authentication authentication) {
        return myActivitiesService.listForUser(resolveUsername(authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MyActivityDto> getById(@PathVariable String id, Authentication authentication) {
        return myActivitiesService.findForUser(id, resolveUsername(authentication))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/complete")
    public Tramite complete(
            @PathVariable String id,
            @RequestBody CompleteActivityRequest request,
            Authentication authentication
    ) {
        return myActivitiesService.completeActivity(id, request, resolveUsername(authentication));
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
        return authentication.getName();
    }
}
