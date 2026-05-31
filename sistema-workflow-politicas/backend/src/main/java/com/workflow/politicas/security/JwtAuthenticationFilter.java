package com.workflow.politicas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String path = request.getRequestURI();
        final String authHeader = request.getHeader("Authorization");
        final boolean isWorkflowApiRequest = path.startsWith("/api/workflow-activities")
                || path.startsWith("/api/workflow-transitions")
                || path.startsWith("/api/workflow-designer");
        final boolean isTransitionDedupeRequest = path.startsWith("/api/workflow-transitions")
                && path.contains("/deduplicate");
        final boolean isTransitionCleanupRequest = path.startsWith("/api/workflow-transitions")
                && path.contains("/cleanup");

        if (isWorkflowApiRequest) {
            log.debug("Workflow API request {} {} Authorization={}",
                    request.getMethod(),
                    path,
                    authHeader != null && authHeader.startsWith("Bearer ") ? "Bearer present" : "missing");
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                final String jwt = authHeader.substring(7);
                final String username = jwtService.extractUsername(jwt);
                if (username != null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        if (isTransitionDedupeRequest) {
                            log.info("WorkflowTransition deduplicate JWT OK user={} authorities={}",
                                    username, userDetails.getAuthorities());
                        } else if (isTransitionCleanupRequest) {
                            log.info("WorkflowTransition cleanup JWT OK user={} authorities={}",
                                    username, userDetails.getAuthorities());
                        } else if (isWorkflowApiRequest) {
                            String logLabel = "WorkflowActivity";
                            if (path.startsWith("/api/workflow-transitions")) {
                                logLabel = "WorkflowTransition";
                            } else if (path.startsWith("/api/workflow-designer")) {
                                logLabel = "WorkflowDesigner";
                            }
                            log.info("{} JWT OK user={} authorities={}",
                                    logLabel, username, userDetails.getAuthorities());
                        }
                    } else if (isWorkflowApiRequest) {
                        log.warn("Workflow API JWT invalid for user={} path={}", username, path);
                    }
                } else if (isWorkflowApiRequest) {
                    log.warn("Invalid JWT token received for {}", path);
                }
            } catch (Exception ex) {
                log.warn("JWT processing failed for {}: {}", path, ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
