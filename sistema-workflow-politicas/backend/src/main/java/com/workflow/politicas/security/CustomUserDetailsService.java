package com.workflow.politicas.security;

import com.workflow.politicas.model.Role;
import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.RoleRepository;
import com.workflow.politicas.repository.UserRepository;
import com.workflow.politicas.security.RoleReferenceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private static final Pattern MONGO_OBJECT_ID = Pattern.compile("^[a-f0-9]{24}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CustomUserDetailsService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Set<String> authorities = new LinkedHashSet<>();
        if (user.getRoleIds() != null) {
            for (String roleIdOrName : user.getRoleIds()) {
                resolveRoleAuthorities(roleIdOrName).forEach(authorities::add);
            }
        }
        if (authorities.isEmpty()) {
            authorities.add("ROLE_USER");
        }

        log.info("User '{}' loaded with authorities: {}", username, authorities);

        List<SimpleGrantedAuthority> granted = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isActive(),
                true, true, true,
                granted
        );
    }

    private Set<String> resolveRoleAuthorities(String roleIdOrName) {
        Set<String> result = new LinkedHashSet<>();
        if (roleIdOrName == null || roleIdOrName.isBlank()) {
            return result;
        }

        Optional<Role> roleOpt = RoleReferenceResolver.resolve(roleRepository, roleIdOrName);
        if (roleOpt.isPresent()) {
            Role role = roleOpt.get();
            String normalized = normalizeRole(role.getName());
            result.add(normalized);
            if ("ROLE_POLICY_DESIGNER".equals(normalized)) {
                result.add("ROLE_DESIGNER");
            }
            if (role.isActive() && role.getPermissionIds() != null) {
                result.addAll(role.getPermissionIds());
            }
            addWorkflowRoleAliases(normalized, result);
            return result;
        }

        if (MONGO_OBJECT_ID.matcher(roleIdOrName).matches()) {
            log.warn("Could not resolve role reference '{}' for user authorities", roleIdOrName);
            return result;
        }

        String normalized = normalizeRole(roleIdOrName);
        result.add(normalized);
        if ("ROLE_POLICY_DESIGNER".equals(normalized)) {
            result.add("ROLE_DESIGNER");
        }
        addWorkflowRoleAliases(normalized, result);
        return result;
    }

    private void addWorkflowRoleAliases(String normalizedRole, Set<String> authorities) {
        if ("ROLE_ADMIN".equals(normalizedRole)) {
            authorities.add(SystemPermissions.WORKFLOW_MANAGE);
            authorities.add(SystemPermissions.WORKFLOW_DESIGN);
            authorities.add(SystemPermissions.WORKFLOW_VIEW);
            authorities.add(SystemPermissions.POLICIES_MANAGE);
        }
        if ("ROLE_PROCESS_OWNER".equals(normalizedRole) || "ROLE_POLICY_DESIGNER".equals(normalizedRole)) {
            authorities.add(SystemPermissions.WORKFLOW_MANAGE);
            authorities.add(SystemPermissions.WORKFLOW_DESIGN);
            authorities.add(SystemPermissions.WORKFLOW_VIEW);
            authorities.add(SystemPermissions.POLICIES_MANAGE);
        }
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_USER";
        }

        String r = role.toUpperCase()
                .replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U").replace("Ñ", "N")
                .replace(" ", "_")
                .trim();

        if (r.startsWith("ROLE_")) {
            r = r.substring(5);
        }

        if (r.equals("ADMINISTRADOR") || r.equals("ADMIN") || r.contains("ADMINISTRADOR_DEL_SISTEMA")) {
            return "ROLE_ADMIN";
        }
        if (r.contains("DISENADOR") || r.contains("DESIGNER") || r.contains("POLITIC")
                || r.equals("POLICY_DESIGNER")) {
            return "ROLE_POLICY_DESIGNER";
        }
        if (r.equals("SUPERVISOR")) {
            return "ROLE_SUPERVISOR";
        }
        if (r.equals("ANALISTA") || r.equals("ANALYST")) {
            return "ROLE_ANALYST";
        }
        if (r.contains("DUENO") && r.contains("PROCESO")) {
            return "ROLE_PROCESS_OWNER";
        }
        if (r.contains("ATENCION") && r.contains("CLIENTE")) {
            return "ROLE_CUSTOMER_SERVICE";
        }
        if (r.equals("TECNICO")) {
            return "ROLE_TECHNICIAN";
        }
        if (r.equals("LEGAL")) {
            return "ROLE_LEGAL";
        }
        if (r.contains("RESPONSABLE") && r.contains("PROCESO")) {
            return "ROLE_PROCESS_OWNER";
        }
        if (r.equals("AUDITOR")) {
            return "ROLE_AUDITOR";
        }
        if (r.equals("FUNCIONARIO") || r.equals("OFFICIAL")
                || r.equals("USUARIO_OPERATIVO") || r.equals("USER")) {
            return "ROLE_USER";
        }

        return "ROLE_" + r;
    }
}
