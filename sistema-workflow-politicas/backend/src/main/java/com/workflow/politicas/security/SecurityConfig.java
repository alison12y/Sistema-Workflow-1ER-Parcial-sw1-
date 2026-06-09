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

    private static final String[] FORM_PATHS = {
            "/api/forms",
            "/api/forms/**"
    };

    private static final String[] FORM_FIELD_PATHS = {
            "/api/form-fields",
            "/api/form-fields/**"
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

    /** Permisos/roles que pueden ver formularios dinámicos. */
    private static final String[] FORM_READ_AUTHORITIES = {
            "ROLE_ADMIN",
            "ROLE_PROCESS_OWNER",
            "ROLE_SUPERVISOR",
            "ROLE_CUSTOMER_SERVICE",
            SystemPermissions.WORKFLOW_VIEW,
            SystemPermissions.WORKFLOW_MANAGE,
            SystemPermissions.WORKFLOW_DESIGN,
            SystemPermissions.POLICIES_MANAGE,
            SystemPermissions.FORMS_MANAGE,
            SystemPermissions.TASKS_EXECUTE
    };

    /** Permisos/roles que pueden consultar repositorios y documentos (CU17). */
    private static final String[] DOCUMENT_VIEW_AUTHORITIES = {
            "ROLE_ADMIN",
            "ROLE_PROCESS_OWNER",
            "ROLE_SUPERVISOR",
            "ROLE_CUSTOMER_SERVICE",
            "ROLE_FUNCIONARIO",
            SystemPermissions.DOCUMENTS_VIEW,
            SystemPermissions.DOCUMENTS_UPLOAD,
            SystemPermissions.DOCUMENTS_DELETE,
            SystemPermissions.MONITORING_VIEW,
            SystemPermissions.WORKFLOW_MANAGE,
            SystemPermissions.POLICIES_MANAGE
    };

    /** Permisos/roles que pueden subir documentos (CU17). */
    private static final String[] DOCUMENT_UPLOAD_AUTHORITIES = {
            "ROLE_ADMIN",
            "ROLE_PROCESS_OWNER",
            "ROLE_CUSTOMER_SERVICE",
            "ROLE_FUNCIONARIO",
            SystemPermissions.DOCUMENTS_UPLOAD,
            SystemPermissions.WORKFLOW_MANAGE,
            SystemPermissions.POLICIES_MANAGE
    };

    /** Permisos/roles que pueden eliminar documentos (CU17). */
    private static final String[] DOCUMENT_DELETE_AUTHORITIES = {
            "ROLE_ADMIN",
            "ROLE_PROCESS_OWNER",
            SystemPermissions.DOCUMENTS_DELETE,
            SystemPermissions.WORKFLOW_MANAGE,
            SystemPermissions.POLICIES_MANAGE
    };

    /** Permisos/roles que pueden ejecutar tareas y usar IA de formularios. */
    private static final String[] TASK_EXECUTE_AUTHORITIES = {
            "ROLE_ADMIN",
            "ROLE_PROCESS_OWNER",
            "ROLE_SUPERVISOR",
            "ROLE_CUSTOMER_SERVICE",
            "ROLE_FUNCIONARIO",
            SystemPermissions.TASKS_EXECUTE,
            SystemPermissions.WORKFLOW_MANAGE,
            SystemPermissions.POLICIES_MANAGE
    };

    /** Permisos/roles que pueden usar analítica inteligente (CU24–CU26). */
    private static final String[] INTELLIGENT_ANALYTICS_AUTHORITIES = {
            "ROLE_ADMIN",
            "ROLE_PROCESS_OWNER",
            "ROLE_SUPERVISOR",
            SystemPermissions.INTELLIGENT_ANALYTICS_VIEW,
            SystemPermissions.KPI_VIEW,
            SystemPermissions.MONITORING_VIEW,
            SystemPermissions.REPORTS_VIEW
    };

    /** Permisos/roles que pueden usar el agente inteligente de atención (CU21–CU23). */
    private static final String[] AI_AGENT_AUTHORITIES = {
            "ROLE_ADMIN",
            "ROLE_PROCESS_OWNER",
            "ROLE_CUSTOMER_SERVICE",
            "ROLE_FUNCIONARIO",
            SystemPermissions.AI_AGENT_USE,
            SystemPermissions.TASKS_EXECUTE
    };

    /** Permisos/roles que pueden diseñar formularios dinámicos. */
    private static final String[] FORM_MUTATION_AUTHORITIES = {
            "ROLE_ADMIN",
            "ROLE_PROCESS_OWNER",
            SystemPermissions.WORKFLOW_MANAGE,
            SystemPermissions.WORKFLOW_DESIGN,
            SystemPermissions.POLICIES_MANAGE,
            SystemPermissions.FORMS_MANAGE
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
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
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
                        .requestMatchers(HttpMethod.POST, "/api/workflow-designer/policy/*/collaboration/open")
                                .hasAnyAuthority(WORKFLOW_READ_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/workflow-designer/policy/*/collaboration/heartbeat")
                                .hasAnyAuthority(WORKFLOW_READ_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/workflow-designer/policy/*/collaboration/close")
                                .hasAnyAuthority(WORKFLOW_READ_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/workflow-designer/policy/*/collaboration/conflict")
                                .hasAnyAuthority(WORKFLOW_READ_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/workflow-designer/policy/*/collaboration/editing")
                                .hasAnyAuthority(WORKFLOW_READ_AUTHORITIES)
                        .requestMatchers(HttpMethod.DELETE, "/api/workflow-designer/policy/*/collaboration/editing/*")
                                .hasAnyAuthority(WORKFLOW_READ_AUTHORITIES)
                        .requestMatchers(HttpMethod.GET, FORM_PATHS)
                                .hasAnyAuthority(FORM_READ_AUTHORITIES)
                        .requestMatchers(HttpMethod.GET, FORM_FIELD_PATHS)
                                .hasAnyAuthority(FORM_READ_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, FORM_PATHS)
                                .hasAnyAuthority(FORM_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, FORM_FIELD_PATHS)
                                .hasAnyAuthority(FORM_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.PUT, FORM_PATHS)
                                .hasAnyAuthority(FORM_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.PUT, FORM_FIELD_PATHS)
                                .hasAnyAuthority(FORM_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.PATCH, FORM_PATHS)
                                .hasAnyAuthority(FORM_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.PATCH, FORM_FIELD_PATHS)
                                .hasAnyAuthority(FORM_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.DELETE, FORM_PATHS)
                                .hasAnyAuthority(FORM_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.DELETE, FORM_FIELD_PATHS)
                                .hasAnyAuthority(FORM_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.GET, "/api/policies/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/policies/**")
                                .hasAnyRole("ADMIN", "POLICY_DESIGNER", "DESIGNER")
                        .requestMatchers(HttpMethod.PUT, "/api/policies/**")
                                .hasAnyRole("ADMIN", "POLICY_DESIGNER", "DESIGNER")
                        .requestMatchers(HttpMethod.PATCH, "/api/policies/**")
                                .hasAnyRole("ADMIN", "POLICY_DESIGNER", "DESIGNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/policies/**").hasRole("ADMIN")
                        .requestMatchers("/api/activity-diagrams/**").authenticated()
                        .requestMatchers("/api/tramites", "/api/tramites/**").authenticated()
                        .requestMatchers("/api/bitacora/**").authenticated()
                        .requestMatchers("/api/kpis/**").authenticated()
                        .requestMatchers("/api/monitoring/**").authenticated()
                        .requestMatchers("/api/my-activities/**").authenticated()
                        .requestMatchers("/api/form-submissions/**").authenticated()
                        .requestMatchers("/api/form-submissions/files/**").authenticated()
                        .requestMatchers("/api/storage/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/document-repositories/ping").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/smart-agent/ping").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/intelligent-analytics/ping").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/offline/ping").permitAll()
                        .requestMatchers("/api/offline/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/document-repositories/migrate-existing")
                                .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/document-repositories/documents/*/onlyoffice/file")
                                .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/document-repositories/documents/*/onlyoffice/callback")
                                .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/document-repositories/**")
                                .hasAnyAuthority(DOCUMENT_VIEW_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/document-repositories/documents/*/edit/*")
                                .hasAnyAuthority(DOCUMENT_VIEW_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/document-repositories/*/collaboration/**")
                                .hasAnyAuthority(DOCUMENT_VIEW_AUTHORITIES)
                        .requestMatchers(HttpMethod.DELETE, "/api/document-repositories/*/collaboration/**")
                                .hasAnyAuthority(DOCUMENT_VIEW_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/document-repositories/documents/*/permissions")
                                .hasAnyAuthority(DOCUMENT_DELETE_AUTHORITIES)
                        .requestMatchers(HttpMethod.PUT, "/api/document-repositories/documents/*/permissions")
                                .hasAnyAuthority(DOCUMENT_DELETE_AUTHORITIES)
                        .requestMatchers(HttpMethod.DELETE, "/api/document-repositories/documents/*/permissions")
                                .hasAnyAuthority(DOCUMENT_DELETE_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/document-repositories/*/upload")
                                .hasAnyAuthority(DOCUMENT_UPLOAD_AUTHORITIES)
                        .requestMatchers(HttpMethod.DELETE, "/api/document-repositories/documents/*")
                                .hasAnyAuthority(DOCUMENT_DELETE_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/ai/workflow/suggest")
                                .hasAnyAuthority(WORKFLOW_MUTATION_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/ai/assist-form")
                                .hasAnyAuthority(TASK_EXECUTE_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/my-activities/*/ai-form-assisted")
                                .hasAnyAuthority(TASK_EXECUTE_AUTHORITIES)
                        .requestMatchers("/api/smart-agent/**")
                                .hasAnyAuthority(AI_AGENT_AUTHORITIES)
                        .requestMatchers("/api/intelligent-analytics/**")
                                .hasAnyAuthority(INTELLIGENT_ANALYTICS_AUTHORITIES)
                        .requestMatchers("/api/ai/**").authenticated()
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
