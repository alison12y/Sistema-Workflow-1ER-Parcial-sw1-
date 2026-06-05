package com.workflow.politicas.service;

import com.workflow.politicas.audit.AuditActions;
import com.workflow.politicas.audit.AuditModules;
import com.workflow.politicas.model.Department;
import com.workflow.politicas.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceAuditTest {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private BitacoraService bitacoraService;

    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentService(departmentRepository, bitacoraService);
        when(bitacoraService.resolveActorDisplay()).thenReturn("Admin Test");
    }

    @Test
    void create_registersAuditEvent() {
        Department department = new Department();
        department.setName("Legal");

        Department saved = new Department();
        saved.setId("dep-1");
        saved.setName("Legal");
        when(departmentRepository.save(any(Department.class))).thenReturn(saved);

        departmentService.create(department);

        verify(bitacoraService).registrar(
                eq(AuditModules.DEPARTAMENTOS),
                eq(AuditActions.CREAR_DEPARTAMENTO),
                eq("Admin Test creó el departamento Legal"),
                eq("Department"),
                eq("dep-1")
        );
    }
}
