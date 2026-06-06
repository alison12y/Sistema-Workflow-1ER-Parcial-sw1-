package com.workflow.politicas.controller;

import com.workflow.politicas.dto.AnalyticsRecommendationResponse;
import com.workflow.politicas.dto.AnalyticsReportResponse;
import com.workflow.politicas.dto.AnalyticsRequest;
import com.workflow.politicas.dto.AnalyticsRiskResponse;
import com.workflow.politicas.service.IntelligentAnalyticsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/intelligent-analytics")
public class IntelligentAnalyticsController {

    private final IntelligentAnalyticsService intelligentAnalyticsService;

    public IntelligentAnalyticsController(IntelligentAnalyticsService intelligentAnalyticsService) {
        this.intelligentAnalyticsService = intelligentAnalyticsService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "intelligent-analytics-ok";
    }

    @PostMapping("/report")
    public AnalyticsReportResponse report(@RequestBody AnalyticsRequest request, Authentication authentication) {
        return intelligentAnalyticsService.generateReport(request, resolveUsername(authentication));
    }

    @PostMapping("/risks")
    public AnalyticsRiskResponse risks(@RequestBody AnalyticsRequest request, Authentication authentication) {
        return intelligentAnalyticsService.analyzeRisks(request, resolveUsername(authentication));
    }

    @PostMapping("/recommendations")
    public AnalyticsRecommendationResponse recommendations(
            @RequestBody AnalyticsRequest request,
            Authentication authentication
    ) {
        return intelligentAnalyticsService.generateRecommendations(request, resolveUsername(authentication));
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        return authentication.getName();
    }
}
