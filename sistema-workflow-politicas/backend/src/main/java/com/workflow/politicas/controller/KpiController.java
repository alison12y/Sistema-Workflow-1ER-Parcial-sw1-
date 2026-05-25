package com.workflow.politicas.controller;

import com.workflow.politicas.dto.KpiBottlenecksResponse;
import com.workflow.politicas.dto.KpiDashboardResponse;
import com.workflow.politicas.service.KpiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kpi")
public class KpiController {

    private final KpiService kpiService;

    public KpiController(KpiService kpiService) {
        this.kpiService = kpiService;
    }

    @GetMapping("/dashboard")
    public KpiDashboardResponse getDashboard() {
        return kpiService.getDashboard();
    }

    @GetMapping("/bottlenecks")
    public KpiBottlenecksResponse getBottlenecks() {
        return kpiService.getBottlenecks();
    }
}
