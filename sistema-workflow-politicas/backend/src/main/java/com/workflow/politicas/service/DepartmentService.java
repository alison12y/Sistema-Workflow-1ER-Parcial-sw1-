package com.workflow.politicas.service;

import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.model.Department;
import com.workflow.politicas.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final BitacoraService bitacoraService;

    public DepartmentService(DepartmentRepository departmentRepository, BitacoraService bitacoraService) {
        this.departmentRepository = departmentRepository;
        this.bitacoraService = bitacoraService;
    }

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public Optional<Department> findById(String id) {
        return departmentRepository.findById(id);
    }

    public Department create(Department department) {
        Department saved = departmentRepository.save(department);
        String actor = bitacoraService.resolveActorDisplay();
        bitacoraService.registrar(
                AuditModules.DEPARTAMENTOS,
                AuditActions.CREAR_DEPARTAMENTO,
                actor + " creó el departamento " + saved.getName(),
                "Department",
                saved.getId()
        );
        return saved;
    }

    public Optional<Department> update(String id, Department departmentDetails) {
        return departmentRepository.findById(id).map(department -> {
            department.setName(departmentDetails.getName());
            department.setDescription(departmentDetails.getDescription());
            department.setManagerId(departmentDetails.getManagerId());
            Department saved = departmentRepository.save(department);
            String actor = bitacoraService.resolveActorDisplay();
            bitacoraService.registrar(
                    AuditModules.DEPARTAMENTOS,
                    AuditActions.EDITAR_DEPARTAMENTO,
                    actor + " editó el departamento " + saved.getName(),
                    "Department",
                    saved.getId()
            );
            return saved;
        });
    }

    public void deleteById(String id) {
        departmentRepository.findById(id).ifPresent(department -> {
            String actor = bitacoraService.resolveActorDisplay();
            bitacoraService.registrar(
                    AuditModules.DEPARTAMENTOS,
                    AuditActions.ELIMINAR_DEPARTAMENTO,
                    actor + " eliminó el departamento " + department.getName(),
                    "Department",
                    department.getId()
            );
            departmentRepository.deleteById(id);
        });
    }

    /** Compatibilidad con código existente. */
    public Department save(Department department) {
        if (department.getId() == null || department.getId().isBlank()) {
            return create(department);
        }
        return update(department.getId(), department).orElseGet(() -> departmentRepository.save(department));
    }
}
