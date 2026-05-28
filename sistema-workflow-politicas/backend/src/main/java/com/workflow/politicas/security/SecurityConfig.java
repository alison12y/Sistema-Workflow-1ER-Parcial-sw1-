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
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityLoggingFilter securityLoggingFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter,
            CustomUserDetailsService userDetailsService,
            SecurityLoggingFilter securityLoggingFilter,
            CorsConfigurationSource corsConfigurationSource
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.securityLoggingFilter = securityLoggingFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
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
