package com.workflow.politicas.service;

import com.workflow.politicas.dto.BitacoraResponse;
import com.workflow.politicas.model.Bitacora;
import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.BitacoraRepository;
import com.workflow.politicas.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class BitacoraService {

    private final BitacoraRepository bitacoraRepository;
    private final UserRepository userRepository;

    public BitacoraService(BitacoraRepository bitacoraRepository, UserRepository userRepository) {
        this.bitacoraRepository = bitacoraRepository;
        this.userRepository = userRepository;
    }

    public void registrar(String module, String action, String description, String entityType, String entityId) {
        registrar(resolveCurrentUsername(), module, action, description, entityType, entityId);
    }

    public void registrar(
            String actorUsername,
            String module,
            String action,
            String description,
            String entityType,
            String entityId
    ) {
        String username = actorUsername != null && !actorUsername.isBlank() ? actorUsername.trim() : "system";
        String userId = username;
        String displayName = username;

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userId = user.getId();
            displayName = displayName(user);
        }

        Bitacora entry = new Bitacora();
        entry.setUserId(userId);
        entry.setUsername(displayName);
        entry.setModule(module);
        entry.setAction(action);
        entry.setDescription(description);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setCreatedAt(LocalDateTime.now());
        bitacoraRepository.save(entry);
    }

    public String resolveActorDisplay() {
        return resolveActorDisplay(resolveCurrentUsername());
    }

    public String resolveActorDisplay(String username) {
        if (username == null || username.isBlank()) {
            return "Sistema";
        }
        return userRepository.findByUsername(username.trim())
                .map(this::displayName)
                .orElse(username.trim());
    }

    public List<BitacoraResponse> findAll() {
        return bitacoraRepository.findAll().stream()
                .sorted(Comparator.comparing(Bitacora::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    public List<BitacoraResponse> findByModule(String module) {
        return bitacoraRepository.findByModule(module).stream()
                .sorted(Comparator.comparing(Bitacora::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    public List<BitacoraResponse> findByUserId(String userId) {
        return bitacoraRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Bitacora::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    private BitacoraResponse toResponse(Bitacora entry) {
        BitacoraResponse response = new BitacoraResponse();
        response.setUsername(entry.getUsername());
        response.setModule(entry.getModule());
        response.setAction(entry.getAction());
        response.setDescription(entry.getDescription());
        response.setCreatedAt(entry.getCreatedAt());
        return response;
    }

    private String resolveCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null
                && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            return auth.getName();
        }
        return "system";
    }

    private String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        return user.getUsername();
    }
}
