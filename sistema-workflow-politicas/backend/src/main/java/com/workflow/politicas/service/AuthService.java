package com.workflow.politicas.service;

import com.workflow.politicas.dto.AuthResponse;
import com.workflow.politicas.dto.LoginRequest;
import com.workflow.politicas.dto.RegisterRequest;
import com.workflow.politicas.model.Role;
import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.RoleRepository;
import com.workflow.politicas.repository.UserRepository;
import com.workflow.politicas.security.JwtService;
import com.workflow.politicas.security.RoleReferenceResolver;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    public AuthResponse register(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setDepartmentId(request.getDepartmentId());
        user.setRoleIds(request.getRoles() != null ? request.getRoles() : new HashSet<>());
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        var jwtToken = jwtService.generateToken(userDetails);

        return buildAuthResponse(user, userDetails, jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        var jwtToken = jwtService.generateToken(userDetails);

        return buildAuthResponse(user, userDetails, jwtToken);
    }

    private AuthResponse buildAuthResponse(User user, UserDetails userDetails, String jwtToken) {
        AuthResponse response = new AuthResponse();
        response.setToken(jwtToken);
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());

        List<String> roleNames = resolveRoleNames(user);
        Set<String> permissions = resolvePermissions(user);

        response.setRoles(roleNames);
        response.setPermissions(permissions);
        response.setRoleName(roleNames.isEmpty() ? "Usuario" : roleNames.get(0));

        if (!userDetails.getAuthorities().isEmpty()) {
            response.setRole(userDetails.getAuthorities().iterator().next().getAuthority());
        } else {
            response.setRole("ROLE_USER");
        }

        return response;
    }

    private List<String> resolveRoleNames(User user) {
        List<String> names = new ArrayList<>();
        if (user.getRoleIds() == null) {
            return names;
        }
        for (String roleIdOrName : user.getRoleIds()) {
            resolveRole(roleIdOrName).ifPresent(role -> {
                if (role.getName() != null && !role.getName().isBlank()) {
                    names.add(role.getName());
                }
            });
        }
        return names;
    }

    private Set<String> resolvePermissions(User user) {
        Set<String> permissions = new LinkedHashSet<>();
        if (user.getRoleIds() == null) {
            return permissions;
        }
        for (String roleIdOrName : user.getRoleIds()) {
            resolveRole(roleIdOrName).ifPresent(role -> {
                if (role.isActive() && role.getPermissionIds() != null) {
                    permissions.addAll(role.getPermissionIds());
                }
            });
        }
        return permissions;
    }

    private Optional<Role> resolveRole(String roleIdOrName) {
        return RoleReferenceResolver.resolve(roleRepository, roleIdOrName);
    }
}
