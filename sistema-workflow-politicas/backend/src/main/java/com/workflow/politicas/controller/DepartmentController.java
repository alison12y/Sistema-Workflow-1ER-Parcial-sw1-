package com.workflow.politicas.controller;

import com.workflow.politicas.model.Department;
import com.workflow.politicas.service.DepartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<Department> getAllDepartments() {
        return departmentService.findAll();
    }

    @PostMapping
    public Department createDepartment(@RequestBody Department department) {
        return departmentService.save(department);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> updateDepartment(@PathVariable String id, @RequestBody Department departmentDetails) {
        return departmentService.findById(id)
                .map(department -> {
                    department.setName(departmentDetails.getName());
                    department.setDescription(departmentDetails.getDescription());
                    department.setManagerId(departmentDetails.getManagerId());
                    return ResponseEntity.ok(departmentService.save(department));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable String id) {
        departmentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
