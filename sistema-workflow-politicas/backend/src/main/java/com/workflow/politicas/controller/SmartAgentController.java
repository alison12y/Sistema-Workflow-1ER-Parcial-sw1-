package com.workflow.politicas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.politicas.dto.SmartAgentAnalyzeRequest;
import com.workflow.politicas.dto.SmartAgentAnalyzeResponse;
import com.workflow.politicas.dto.SmartAgentStartTramiteRequest;
import com.workflow.politicas.dto.SmartAgentStartTramiteResponse;
import com.workflow.politicas.service.SmartAgentService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/smart-agent")
public class SmartAgentController {

    private final SmartAgentService smartAgentService;
    private final ObjectMapper objectMapper;

    public SmartAgentController(SmartAgentService smartAgentService, ObjectMapper objectMapper) {
        this.smartAgentService = smartAgentService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/ping")
    public String ping() {
        return "smart-agent-ok";
    }

    @PostMapping(value = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SmartAgentAnalyzeResponse analyzeJson(
            @RequestBody SmartAgentAnalyzeRequest request,
            Authentication authentication
    ) {
        return smartAgentService.analyze(request, null, resolveUsername(authentication));
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SmartAgentAnalyzeResponse analyzeMultipart(
            @RequestPart(value = "request", required = false) String requestJson,
            @RequestPart(value = "message", required = false) String message,
            @RequestPart(value = "audioText", required = false) String audioText,
            @RequestPart(value = "documentId", required = false) String documentId,
            @RequestPart(value = "requesterName", required = false) String requesterName,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment,
            Authentication authentication
    ) throws Exception {
        SmartAgentAnalyzeRequest request = parseRequest(requestJson, message, audioText, documentId, requesterName);
        return smartAgentService.analyze(request, attachment, resolveUsername(authentication));
    }

    @PostMapping(value = "/start-tramite", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SmartAgentStartTramiteResponse startTramiteJson(
            @RequestBody SmartAgentStartTramiteRequest request,
            Authentication authentication
    ) {
        return smartAgentService.startTramite(request, null, resolveUsername(authentication));
    }

    @PostMapping(value = "/start-tramite", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SmartAgentStartTramiteResponse startTramiteMultipart(
            @RequestPart("request") String requestJson,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment,
            Authentication authentication
    ) throws Exception {
        SmartAgentStartTramiteRequest request = objectMapper.readValue(requestJson, SmartAgentStartTramiteRequest.class);
        return smartAgentService.startTramite(request, attachment, resolveUsername(authentication));
    }

    private SmartAgentAnalyzeRequest parseRequest(
            String requestJson,
            String message,
            String audioText,
            String documentId,
            String requesterName
    ) throws Exception {
        if (requestJson != null && !requestJson.isBlank()) {
            return objectMapper.readValue(requestJson, SmartAgentAnalyzeRequest.class);
        }
        SmartAgentAnalyzeRequest request = new SmartAgentAnalyzeRequest();
        request.setMessage(message);
        request.setAudioText(audioText);
        request.setDocumentId(documentId);
        request.setRequesterName(requesterName);
        return request;
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        return authentication.getName();
    }
}
