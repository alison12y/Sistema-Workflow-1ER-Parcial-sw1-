package com.workflow.politicas.controller;

import com.workflow.politicas.dto.FormSubmissionFileResponse;
import com.workflow.politicas.dto.FormSubmissionRequest;
import com.workflow.politicas.dto.FormSubmissionResponse;
import com.workflow.politicas.service.FormSubmissionFileService;
import com.workflow.politicas.service.FormSubmissionService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/form-submissions")
public class FormSubmissionController {

    private final FormSubmissionService formSubmissionService;
    private final FormSubmissionFileService formSubmissionFileService;

    public FormSubmissionController(
            FormSubmissionService formSubmissionService,
            FormSubmissionFileService formSubmissionFileService
    ) {
        this.formSubmissionService = formSubmissionService;
        this.formSubmissionFileService = formSubmissionFileService;
    }

    @PostMapping
    public FormSubmissionResponse save(@RequestBody FormSubmissionRequest request, Authentication authentication) {
        return formSubmissionService.save(request, resolveUsername(authentication));
    }

    @PostMapping("/files")
    public FormSubmissionFileResponse uploadFile(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        return formSubmissionFileService.store(file, resolveUsername(authentication));
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        FormSubmissionFileService.StoredFile stored = formSubmissionFileService.load(fileId);
        String fileName = stored.metadata().getOriginalFileName();
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType = MediaType.parseMediaType(stored.metadata().getContentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName
                )
                .body(stored.resource());
    }

    @GetMapping("/tramite/{tramiteId}")
    public List<FormSubmissionResponse> findByTramite(@PathVariable String tramiteId) {
        return formSubmissionService.findByTramite(tramiteId);
    }

    @GetMapping("/tramite/{tramiteId}/activity")
    public ResponseEntity<FormSubmissionResponse> findByActivity(
            @PathVariable String tramiteId,
            @RequestParam(required = false) String activity,
            @RequestParam(required = false) String workflowActivityId,
            @RequestParam int taskOrder
    ) {
        if (workflowActivityId != null && !workflowActivityId.isBlank()) {
            return formSubmissionService.findForTask(tramiteId, workflowActivityId, activity, taskOrder)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        if (activity != null && !activity.isBlank()) {
            return formSubmissionService.findByTramiteAndActivity(tramiteId, activity, taskOrder)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/task/{taskKey}")
    public ResponseEntity<FormSubmissionResponse> findByTaskKey(
            @PathVariable String taskKey,
            @RequestParam String activityName,
            @RequestParam int taskOrder
    ) {
        String tramiteId = taskKey.contains(":") ? taskKey.substring(0, taskKey.lastIndexOf(':')) : taskKey;
        return formSubmissionService.findByTramiteAndActivity(tramiteId, activityName, taskOrder)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
        return authentication.getName();
    }
}
