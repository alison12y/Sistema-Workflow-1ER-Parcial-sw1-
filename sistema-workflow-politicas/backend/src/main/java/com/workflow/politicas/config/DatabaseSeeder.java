package com.workflow.politicas.config;

import com.workflow.politicas.model.BusinessPolicy;
import com.workflow.politicas.model.Department;
import com.workflow.politicas.model.Role;
import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.BusinessPolicyRepository;
import com.workflow.politicas.repository.DepartmentRepository;
import com.workflow.politicas.repository.RoleRepository;
import com.workflow.politicas.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final BusinessPolicyRepository businessPolicyRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            BusinessPolicyRepository businessPolicyRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.businessPolicyRepository = businessPolicyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedRoles();
        seedDepartments();
        seedUsers();
        seedPolicies();
    }

    private void seedRoles() {
        if (roleRepository.count() > 0) {
            log.info("DatabaseSeeder: roles already exist, skipping role seed");
            return;
        }

        List<String> roleNames = List.of(
                "Administrador",
                "Diseñador de Políticas",
                "Supervisor",
                "Usuario Operativo",
                "Recursos Humanos",
                "Funcionario"
        );

        for (String name : roleNames) {
            Role role = new Role();
            role.setName(name);
            role.setDescription("Rol inicial del sistema: " + name);
            role.setPermissionIds(new HashSet<>());
            roleRepository.save(role);
        }

        log.info("DatabaseSeeder: created {} initial roles", roleNames.size());
    }

    private void seedDepartments() {
        if (departmentRepository.count() > 0) {
            log.info("DatabaseSeeder: departments already exist, skipping department seed");
            return;
        }

        List<DepartmentSeed> departments = List.of(
                new DepartmentSeed("Sistemas", "Departamento de tecnología y soporte"),
                new DepartmentSeed("Recursos Humanos", "Gestión de personal y permisos"),
                new DepartmentSeed("Administración", "Procesos administrativos y compras"),
                new DepartmentSeed("Dirección", "Dirección general y aprobaciones")
        );

        for (DepartmentSeed seed : departments) {
            Department department = new Department();
            department.setName(seed.name());
            department.setDescription(seed.description());
            departmentRepository.save(department);
        }

        log.info("DatabaseSeeder: created {} initial departments", departments.size());
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("DatabaseSeeder: users already exist, skipping user seed");
            return;
        }

        Role adminRole = roleRepository.findByNameIgnoreCase("Administrador")
                .orElseThrow(() -> new IllegalStateException("Administrador role not found for user seed"));
        Department sistemas = findDepartmentByName("Sistemas")
                .orElseThrow(() -> new IllegalStateException("Sistemas department not found for user seed"));

        createUser("admin", "admin123", "Admin", "admin@local.test", adminRole.getId(), sistemas.getId());
        createUser("alison", "alison123", "Alison Yes", "alison@local.test", adminRole.getId(), sistemas.getId());

        log.info("DatabaseSeeder: created initial users admin and alison");
    }

    private void seedPolicies() {
        if (businessPolicyRepository.count() > 0) {
            log.info("DatabaseSeeder: policies already exist, skipping policy seed");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        BusinessPolicy policy = new BusinessPolicy();
        policy.setName("Política de solicitud de vacaciones");
        policy.setDescription(
                "Define el proceso para registrar, revisar y aprobar solicitudes de vacaciones del personal."
        );
        policy.setType("Permiso de Ausencia");
        policy.setStatus("ACTIVE");
        policy.setVersion("1.0");
        policy.setResponsible("Recursos Humanos");
        policy.setCreatedBy("admin");
        policy.setCreatedAt(now);
        policy.setUpdatedAt(now);
        businessPolicyRepository.save(policy);

        log.info("DatabaseSeeder: created initial business policy '{}'", policy.getName());
    }

    private void createUser(
            String username,
            String rawPassword,
            String fullName,
            String email,
            String roleId,
            String departmentId
    ) {
        if (userRepository.findByUsername(username).isPresent()) {
            log.info("DatabaseSeeder: user '{}' already exists, skipping", username);
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setDepartmentId(departmentId);
        user.setRoleIds(Set.of(roleId));
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private java.util.Optional<Department> findDepartmentByName(String name) {
        return departmentRepository.findAll().stream()
                .filter(department -> name.equalsIgnoreCase(department.getName()))
                .findFirst();
    }

    private record DepartmentSeed(String name, String description) {}
}
