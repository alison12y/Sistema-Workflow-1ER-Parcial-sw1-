package com.workflow.politicas.controller;

import com.workflow.politicas.dto.TramiteAdvanceRequest;
import com.workflow.politicas.dto.TramiteCancelRequest;
import com.workflow.politicas.dto.TramiteCreateRequest;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.service.TramiteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tramites")
public class TramiteController {

    private final TramiteService tramiteService;

    public TramiteController(TramiteService tramiteService) {
        this.tramiteService = tramiteService;
    }

    @GetMapping
    public List<Tramite> getAll() {
        return tramiteService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tramite> getById(@PathVariable String id) {
        return tramiteService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Tramite create(@RequestBody TramiteCreateRequest request, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return tramiteService.create(request, username);
    }

    @PutMapping("/{id}/advance")
    public Tramite advance(
            @PathVariable String id,
            @RequestBody(required = false) TramiteAdvanceRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        return tramiteService.advance(id, request, username);
    }

    @PutMapping("/{id}/cancel")
    public Tramite cancel(
            @PathVariable String id,
            @RequestBody(required = false) TramiteCancelRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        return tramiteService.cancel(id, request, username);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        tramiteService.delete(id, username);
        return ResponseEntity.noContent().build();
    }
}
