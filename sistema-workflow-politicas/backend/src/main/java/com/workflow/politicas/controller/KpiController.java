package com.workflow.politicas.controller;

import com.workflow.politicas.dto.KpiBottleneckDto;
import com.workflow.politicas.dto.KpiDashboardFullResponse;
import com.workflow.politicas.dto.KpiFilter;
import com.workflow.politicas.dto.KpiSummaryResponse;
import com.workflow.politicas.service.KpiService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/kpis")
public class KpiController {

    private final KpiService kpiService;

    public KpiController(KpiService kpiService) {
        this.kpiService = kpiService;
    }

    @GetMapping("/dashboard")
    public KpiDashboardFullResponse getDashboard(
            @RequestParam(required = false) String policyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return kpiService.getDashboard(buildFilter(policyId, status, from, to));
    }

    @GetMapping("/summary")
    public KpiSummaryResponse getSummary(
            @RequestParam(required = false) String policyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return kpiService.getSummary(buildFilter(policyId, status, from, to));
    }

    @GetMapping("/bottlenecks")
    public List<KpiBottleneckDto> getBottlenecks(
            @RequestParam(required = false) String policyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return kpiService.getBottlenecks(buildFilter(policyId, status, from, to));
    }

    private KpiFilter buildFilter(String policyId, String status, LocalDate from, LocalDate to) {
        KpiFilter filter = new KpiFilter();
        filter.setPolicyId(policyId);
        filter.setStatus(status);
        filter.setFromDate(from);
        filter.setToDate(to);
        return filter;
    }
}
