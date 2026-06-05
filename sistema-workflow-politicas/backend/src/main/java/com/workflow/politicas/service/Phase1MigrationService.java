package com.workflow.politicas.service;

import com.workflow.politicas.config.Phase1BootstrapData;
import com.workflow.politicas.config.Phase1BootstrapData.DepartmentSeed;
import com.workflow.politicas.config.Phase1BootstrapData.RoleSeed;
import com.workflow.politicas.config.Phase1BootstrapData.UserSeed;
import com.workflow.politicas.dto.MigrationReport;
import com.workflow.politicas.model.Department;
import com.workflow.politicas.model.Role;
import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.DepartmentRepository;
import com.workflow.politicas.repository.RoleRepository;
import com.workflow.politicas.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Migra o crea datos  sin borrar colecciones completas.
 * Los registros existentes con otros nombres (p. ej. ADMINISTRADOR, Alison) se conservan.
 */
@Service
public class Phase1MigrationService {

    private static final Logger log = LoggerFactory.getLogger(Phase1MigrationService.class);

    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Phase1MigrationService(
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public MigrationReport migratePhase1() {
        MigrationReport report = new MigrationReport();
        migrateRoles(report);
        migrateDepartments(report);
        migrateUsers(report);
        log.info(
                "Phase1 migration finished: roles +{} ~{}, departments +{} ~{}, users +{} ~{}",
                report.getRolesCreated().size(),
                report.getRolesUpdated().size(),
                report.getDepartmentsCreated().size(),
                report.getDepartmentsUpdated().size(),
                report.getUsersCreated().size(),
                report.getUsersUpdated().size()
        );
        return report;
    }

    public void seedIfEmpty() {
        if (roleRepository.count() == 0) {
            migrateRoles(new MigrationReport());
        }
        if (departmentRepository.count() == 0) {
            migrateDepartments(new MigrationReport());
        }
        if (userRepository.count() == 0) {
            migrateUsers(new MigrationReport());
        }
    }

    private void migrateRoles(MigrationReport report) {
        for (RoleSeed seed : Phase1BootstrapData.ROLES) {
            Optional<Role> existing = roleRepository.findByNameIgnoreCase(seed.name());
            if (existing.isPresent()) {
                Role role = existing.get();
                role.setDescription(seed.description());
                role.setPermissionIds(new HashSet<>(seed.permissions()));
                role.setActive(true);
                roleRepository.save(role);
                report.getRolesUpdated().add(seed.name());
            } else {
                Role role = new Role();
                role.setName(seed.name());
                role.setDescription(seed.description());
                role.setPermissionIds(new HashSet<>(seed.permissions()));
                role.setActive(true);
                roleRepository.save(role);
                report.getRolesCreated().add(seed.name());
            }
        }
    }

    private void migrateDepartments(MigrationReport report) {
        for (DepartmentSeed seed : Phase1BootstrapData.DEPARTMENTS) {
            Optional<Department> existing = findDepartmentByName(seed.name());
            if (existing.isPresent()) {
                Department department = existing.get();
                department.setDescription(seed.description());
                departmentRepository.save(department);
                report.getDepartmentsUpdated().add(seed.name());
            } else {
                Department department = new Department();
                department.setName(seed.name());
                department.setDescription(seed.description());
                departmentRepository.save(department);
                report.getDepartmentsCreated().add(seed.name());
            }
        }
    }

    private void migrateUsers(MigrationReport report) {
        for (UserSeed seed : Phase1BootstrapData.USERS) {
            Role role = roleRepository.findByNameIgnoreCase(seed.roleName())
                    .orElse(null);
            Department department = findDepartmentByName(seed.departmentName()).orElse(null);

            if (role == null) {
                report.addWarning("Rol no encontrado para usuario " + seed.username() + ": " + seed.roleName());
                continue;
            }
            if (department == null) {
                report.addWarning("Departamento no encontrado para usuario " + seed.username() + ": " + seed.departmentName());
            }

            Optional<User> existing = userRepository.findByUsername(seed.username());
            if (existing.isPresent()) {
                User user = existing.get();
                user.setPassword(passwordEncoder.encode(seed.rawPassword()));
                user.setFullName(seed.fullName());
                user.setEmail(seed.email());
                user.setRoleIds(Set.of(role.getId()));
                user.setActive(true);
                if (department != null) {
                    user.setDepartmentId(department.getId());
                }
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
                report.getUsersUpdated().add(seed.username());
            } else {
                User user = new User();
                user.setUsername(seed.username());
                user.setPassword(passwordEncoder.encode(seed.rawPassword()));
                user.setFullName(seed.fullName());
                user.setEmail(seed.email());
                user.setRoleIds(Set.of(role.getId()));
                user.setActive(true);
                if (department != null) {
                    user.setDepartmentId(department.getId());
                }
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
                report.getUsersCreated().add(seed.username());
            }
        }
    }

    private Optional<Department> findDepartmentByName(String name) {
        return departmentRepository.findAll().stream()
                .filter(department -> name.equalsIgnoreCase(department.getName()))
                .findFirst();
    }
}
