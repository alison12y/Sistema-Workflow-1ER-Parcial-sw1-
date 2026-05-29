package com.workflow.politicas.service;

import com.workflow.politicas.dto.DashboardStatsResponse;
import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.Tramite;
import com.workflow.politicas.model.TramiteTask;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.TramiteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final BusinessPolicyRepository policyRepository;
    private final TramiteRepository tramiteRepository;

    public DashboardService(BusinessPolicyRepository policyRepository, TramiteRepository tramiteRepository) {
        this.policyRepository = policyRepository;
        this.tramiteRepository = tramiteRepository;
    }

    public DashboardStatsResponse getStats() {
        DashboardStatsResponse stats = new DashboardStatsResponse();
        List<BusinessPolicy> policies = policyRepository.findAll();
        List<Tramite> tramites = tramiteRepository.findAll();

        stats.setPoliticasActivas(policies.stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .count());
        stats.setPoliticasBorrador(policies.stream()
                .filter(p -> {
                    String s = p.getStatus() == null ? "" : p.getStatus().toUpperCase();
                    return s.isEmpty() || "DRAFT".equals(s) || "BORRADOR".equals(s);
                })
                .count());

        stats.setTramitesEnProceso(tramites.stream()
                .filter(t -> "EN_PROCESO".equalsIgnoreCase(t.getStatus())
                        || "INICIADO".equalsIgnoreCase(t.getStatus()))
                .count());

        long tareasPendientes = 0;
        long tareasFinalizadas = 0;
        long tareasEnCurso = 0;
        long posiblesCuellos = 0;

        for (Tramite tramite : tramites) {
            if (tramite.getTasks() == null) {
                continue;
            }
            for (TramiteTask task : tramite.getTasks()) {
                String status = task.getStatus() == null ? "" : task.getStatus().toUpperCase();
                if ("PENDIENTE".equals(status)) {
                    tareasPendientes++;
                } else if ("COMPLETADA".equals(status)) {
                    tareasFinalizadas++;
                } else if ("EN_CURSO".equals(status)) {
                    tareasEnCurso++;
                }
            }
            if ("EN_PROCESO".equalsIgnoreCase(tramite.getStatus()) && tramite.getProgress() < 30) {
                posiblesCuellos++;
            }
        }

        stats.setTareasPendientes(tareasPendientes);
        stats.setTareasFinalizadas(tareasFinalizadas);
        stats.setTramitesObservados(tramites.stream()
                .filter(t -> hasObservedTask(t))
                .count());
        stats.setPosiblesCuellosDeBotella(posiblesCuellos + tareasEnCurso);

        return stats;
    }

    private boolean hasObservedTask(Tramite tramite) {
        if (tramite.getTasks() == null) {
            return false;
        }
        return tramite.getTasks().stream()
                .anyMatch(task -> task.getNotes() != null && !task.getNotes().isBlank());
    }
}
