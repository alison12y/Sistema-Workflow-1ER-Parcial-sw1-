package com.workflow.politicas.service;

import com.workflow.politicas.dto.UserRequest;
import com.workflow.politicas.dto.UserResponse;
import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BitacoraService bitacoraService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            BitacoraService bitacoraService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bitacoraService = bitacoraService;
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public Optional<UserResponse> findById(String id) {
        return userRepository.findById(id).map(this::toResponse);
    }

    public UserResponse create(UserRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        User user = new User();
        applyRequestFields(user, request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        String actor = bitacoraService.resolveActorDisplay();
        bitacoraService.registrar(
                "Usuarios",
                "CREAR_USUARIO",
                actor + " creó el usuario " + saved.getUsername(),
                "User",
                saved.getId()
        );
        return toResponse(saved);
    }

    public Optional<UserResponse> update(String id, UserRequest request) {
        return userRepository.findById(id).map(user -> {
            applyRequestFields(user, request);
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }
            user.setUpdatedAt(LocalDateTime.now());
            User saved = userRepository.save(user);
            String actor = bitacoraService.resolveActorDisplay();
            bitacoraService.registrar(
                    "Usuarios",
                    "EDITAR_USUARIO",
                    actor + " editó el usuario " + saved.getUsername(),
                    "User",
                    saved.getId()
            );
            return toResponse(saved);
        });
    }

    public void deleteById(String id) {
        userRepository.findById(id).ifPresent(user -> {
            String actor = bitacoraService.resolveActorDisplay();
            bitacoraService.registrar(
                    "Usuarios",
                    "ELIMINAR_USUARIO",
                    actor + " eliminó el usuario " + user.getUsername(),
                    "User",
                    user.getId()
            );
            userRepository.deleteById(id);
        });
    }

    private void applyRequestFields(User user, UserRequest request) {
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setDepartmentId(request.getDepartmentId());
        user.setRoleIds(request.getRoleIds());
        user.setActive(request.isActive());
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setDepartmentId(user.getDepartmentId());
        response.setRoleIds(user.getRoleIds());
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
