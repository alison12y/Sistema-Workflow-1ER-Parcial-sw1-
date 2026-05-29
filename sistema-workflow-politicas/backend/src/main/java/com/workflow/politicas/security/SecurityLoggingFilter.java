package com.workflow.politicas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
public class SecurityLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        String path = request.getRequestURI();
        if (!path.startsWith("/api/forms")
                && !path.startsWith("/api/form-fields")
                && !path.startsWith("/api/tramites")
                && !path.startsWith("/api/workflow-activities")
                && !path.startsWith("/api/workflow-transitions")
                && !path.startsWith("/api/workflow-designer")) {
            return;
        }

        String authHeader = request.getHeader("Authorization");
        boolean hasBearer = authHeader != null && authHeader.startsWith("Bearer ");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "anonymous";
        String authorities = auth != null
                ? auth.getAuthorities().stream().map(Object::toString).collect(Collectors.joining(", "))
                : "none";

        log.info(
                "API [{} {}] authHeader={} user={} authorities=[{}] status={}",
                request.getMethod(),
                path,
                hasBearer ? "Bearer present" : "missing",
                username,
                authorities,
                response.getStatus()
        );
    }
}
