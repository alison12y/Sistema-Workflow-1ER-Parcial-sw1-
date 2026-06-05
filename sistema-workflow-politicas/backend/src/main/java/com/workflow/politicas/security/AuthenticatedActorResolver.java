package com.workflow.politicas.security;

import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/** Resuelve el usuario autenticado real desde el SecurityContext/JWT (no confía en datos del cliente). */
@Component
public class AuthenticatedActorResolver {

    private static final Logger log = LoggerFactory.getLogger(AuthenticatedActorResolver.class);

    private final UserRepository userRepository;

    public AuthenticatedActorResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public record Actor(String userId, String username, String displayName) {}

    public Optional<String> resolveCurrentUsername() {
        return resolveCurrentActor().map(Actor::username);
    }

    public Optional<Actor> resolveCurrentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        String username = extractUsername(auth);
        if (username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username)) {
            return Optional.empty();
        }

        String normalized = username.trim();
        Optional<User> userOpt = userRepository.findByUsername(normalized);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(normalized.toLowerCase(Locale.ROOT));
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String display = user.getFullName() != null && !user.getFullName().isBlank()
                    ? user.getFullName().trim()
                    : user.getUsername();
            return Optional.of(new Actor(user.getId(), user.getUsername(), display));
        }

        log.warn("[CU16] Usuario JWT no encontrado en BD: {}", normalized);
        return Optional.of(new Actor(normalized, normalized, normalized));
    }

    public Actor requireCurrentActor() {
        return resolveCurrentActor().orElseThrow(
                () -> new IllegalStateException("No hay usuario autenticado en la sesión actual")
        );
    }

    private String extractUsername(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails details) {
            return details.getUsername();
        }
        if (principal instanceof String principalName) {
            return principalName;
        }
        return auth.getName();
    }
}
