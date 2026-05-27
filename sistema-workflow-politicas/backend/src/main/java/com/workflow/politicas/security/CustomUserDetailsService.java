package com.workflow.politicas.security;

import com.workflow.politicas.model.Role;
import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.RoleRepository;
import com.workflow.politicas.repository.UserRepository;
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

        Optional<String> roleName = resolveRoleName(roleIdOrName);
        if (roleName.isEmpty()) {
            log.warn("Could not resolve role reference '{}' for user authorities", roleIdOrName);
            return result;
        }

        String normalized = normalizeRole(roleName.get());
        result.add(normalized);

        if ("ROLE_POLICY_DESIGNER".equals(normalized)) {
            result.add("ROLE_DESIGNER");
        }

        return result;
    }

    private Optional<String> resolveRoleName(String roleIdOrName) {
        Optional<Role> byId = roleRepository.findById(roleIdOrName);
        if (byId.isPresent()) {
            return Optional.ofNullable(byId.get().getName());
        }

        Optional<Role> byName = roleRepository.findByNameIgnoreCase(roleIdOrName);
        if (byName.isPresent()) {
            return Optional.ofNullable(byName.get().getName());
        }

        if (MONGO_OBJECT_ID.matcher(roleIdOrName).matches()) {
            return Optional.empty();
        }

        return Optional.of(roleIdOrName);
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

        if (r.equals("ADMINISTRADOR") || r.equals("ADMIN")) {
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
