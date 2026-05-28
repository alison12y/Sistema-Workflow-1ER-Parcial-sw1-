package com.workflow.politicas.controller;

import com.workflow.politicas.dto.KpiBottleneckDto;
import com.workflow.politicas.dto.KpiSummaryResponse;
import com.workflow.politicas.service.KpiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kpis")
public class KpiController {

    private final KpiService kpiService;

    public KpiController(KpiService kpiService) {
        this.kpiService = kpiService;
    }

    @GetMapping("/summary")
    public KpiSummaryResponse getSummary() {
        return kpiService.getSummary();
    }

    @GetMapping("/bottlenecks")
    public List<KpiBottleneckDto> getBottlenecks() {
        return kpiService.getBottlenecks();
    }
}
