package com.workflow.politicas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
        final boolean isWorkflowActivityRequest = path.startsWith("/api/workflow-activities");

        if (isWorkflowActivityRequest) {
            org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class)
                    .debug("WorkflowActivity request {} {} Authorization={}",
                            request.getMethod(),
                            path,
                            authHeader != null && authHeader.startsWith("Bearer ") ? "Bearer present" : "missing");
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
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
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
                if (isWorkflowActivityRequest) {
                    org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class)
                            .info("WorkflowActivity JWT OK user={} authorities={}",
                                    username,
                                    userDetails.getAuthorities());
                }
            } else if (isWorkflowActivityRequest) {
                org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class)
                        .warn("WorkflowActivity JWT invalid for user={}", username);
            }
        } else if (isWorkflowActivityRequest) {
            org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class)
                    .warn("Invalid JWT token received for {}", request.getRequestURI());
        }
        filterChain.doFilter(request, response);
    }
}
