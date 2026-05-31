package com.workflow.politicas.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.workflow.politicas.security.SystemPermissions;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] WORKFLOW_ACTIVITY_PATHS = {
            "/api/workflow-activities",
            "/api/workflow-activities/**"
    };

    private static final String[] WORKFLOW_TRANSITION_PATHS = {
            "/api/workflow-transitions",
            "/api/workflow-transitions/**"
    };

    private static final String[] WORKFLOW_TRANSITION_DEDUPE_PATHS = {
            "/api/workflow-transitions/deduplicate",
            "/api/workflow-transitions/policy/*/deduplicate"
    };

    private static final String[] WORKFLOW_TRANSITION_CLEANUP_PATHS = {
            "/api/workflow-transitions/cleanup",
            "/api/workflow-transitions/policy/*/cleanup"
    };

    private static final String[] WORKFLOW_DESIGNER_PATHS = {
            "/api/workflow-designer",
            "/api/workflow-designer/**",
            "/api/workflow-designer/policy/**"
    };

    /** Permisos/roles que pueden leer actividades y conexiones workflow. */
    private static final String[] WORKFLOW_READ_AUTHORITIES = {
            "ROLE_ADMIN",
            "ROLE_PROCESS_OWNER",
            "ROLE_SUPERVISOR",
            "ROLE_CUSTOMER_SERVICE",
            SystemPermissions.WORKFLOW_VIEW,
            SystemPermissions.WORKFLOW_MANAGE,
            SystemPermissions.WORKFLOW_DESIGN,
            SystemPermissions.POLICIES_MANAGE
    };

    /** Permisos/roles que pueden crear, editar o eliminar actividades y conexiones workflow. */
    private static final String[] WORKFLOW_MUTATION_AUTHORITIES = {
            "ROLE_ADMIN",
            "ROLE_PROCESS_OWNER",
            SystemPermissions.WORKFLOW_MANAGE,
            SystemPermissions.WORKFLOW_DESIGN,
            SystemPermissions.POLICIES_MANAGE
    };

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityLoggingFilter securityLoggingFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter,
            CustomUserDetailsService userDetailsService,
            SecurityLoggingFilter securityLoggingFilter
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.securityLoggingFilter = securityLoggingFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // CORS lo maneja el CorsFilter global (CorsConfig) con HIGHEST_PRECEDENCE.
                // Evita duplicar headers Access-Control-Allow-Origin en la cadena de Security.
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/dev/migrate-phase1").permitAll()
                        .requestMatchers("/api/dashboard/**").authenticated()
                        .requestMatchers(HttpMethod.GET, WORKFLOW_ACTIVITY_PATHS)
                                .hasAnyAuthority(WORKFLOW_READ_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/workflow-activities")
                                .hasAnyAuthority(WORKFLOW_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/workflow-activities/**")
                                .hasAnyAuthority(WORKFLOW_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.PUT, WORKFLOW_ACTIVITY_PATHS)
                                .hasAnyAuthority(WORKFLOW_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.PATCH, WORKFLOW_ACTIVITY_PATHS)
                                .hasAnyAuthority(WORKFLOW_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.DELETE, WORKFLOW_ACTIVITY_PATHS)
                                .hasAnyAuthority(WORKFLOW_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.GET, WORKFLOW_TRANSITION_PATHS)
                                .hasAnyAuthority(WORKFLOW_READ_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, WORKFLOW_TRANSITION_DEDUPE_PATHS)
                                .hasAnyAuthority(WORKFLOW_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, WORKFLOW_TRANSITION_CLEANUP_PATHS)
                                .hasAnyAuthority(WORKFLOW_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/workflow-transitions")
                                .hasAnyAuthority(WORKFLOW_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/workflow-transitions/**")
                                .hasAnyAuthority(WORKFLOW_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.PUT, WORKFLOW_TRANSITION_PATHS)
                                .hasAnyAuthority(WORKFLOW_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.PATCH, WORKFLOW_TRANSITION_PATHS)
                                .hasAnyAuthority(WORKFLOW_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.DELETE, WORKFLOW_TRANSITION_PATHS)
                                .hasAnyAuthority(WORKFLOW_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.GET, "/api/workflow-designer/policy/**")
                                .hasAnyAuthority(WORKFLOW_READ_AUTHORITIES)
                        .requestMatchers(HttpMethod.GET, WORKFLOW_DESIGNER_PATHS)
                                .hasAnyAuthority(WORKFLOW_READ_AUTHORITIES)
                        .requestMatchers(HttpMethod.GET, "/api/policies/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/policies/**")
                                .hasAnyRole("ADMIN", "POLICY_DESIGNER", "DESIGNER")
                        .requestMatchers(HttpMethod.PUT, "/api/policies/**")
                                .hasAnyRole("ADMIN", "POLICY_DESIGNER", "DESIGNER")
                        .requestMatchers(HttpMethod.PATCH, "/api/policies/**")
                                .hasAnyRole("ADMIN", "POLICY_DESIGNER", "DESIGNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/policies/**").hasRole("ADMIN")
                        .requestMatchers("/api/forms/**", "/api/form-fields/**").authenticated()
                        .requestMatchers("/api/activity-diagrams/**").authenticated()
                        .requestMatchers("/api/tramites", "/api/tramites/**").authenticated()
                        .requestMatchers("/api/bitacora/**").authenticated()
                        .requestMatchers("/api/kpis/**").authenticated()
                        .requestMatchers("/api/monitoring/**").authenticated()
                        .requestMatchers("/api/my-activities/**").authenticated()
                        .requestMatchers("/api/form-submissions/**").authenticated()
                        .requestMatchers("/api/form-submissions/files/**").authenticated()
                        .requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/roles", "/api/roles/**").hasRole("ADMIN")
                        .requestMatchers("/api/departments", "/api/departments/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            new ObjectMapper().writeValue(response.getOutputStream(),
                                    Map.of("message", "No autenticado. Inicie sesión nuevamente."));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            new ObjectMapper().writeValue(response.getOutputStream(),
                                    Map.of("message", "Acceso denegado. Verifique su rol o vuelva a iniciar sesión."));
                        })
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(securityLoggingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
