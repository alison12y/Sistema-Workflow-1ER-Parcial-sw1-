package com.workflow.politicas.service;

import com.workflow.politicas.dto.AuthResponse;
import com.workflow.politicas.dto.LoginRequest;
import com.workflow.politicas.dto.RegisterRequest;
import com.workflow.politicas.model.User;
import com.workflow.politicas.repository.UserRepository;
import com.workflow.politicas.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager, UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
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
        
        AuthResponse response = new AuthResponse();
        response.setToken(jwtToken);
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        if (!userDetails.getAuthorities().isEmpty()) {
            response.setRole(userDetails.getAuthorities().iterator().next().getAuthority());
        }
        return response;
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
        
        AuthResponse response = new AuthResponse();
        response.setToken(jwtToken);
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        if (!userDetails.getAuthorities().isEmpty()) {
            response.setRole(userDetails.getAuthorities().iterator().next().getAuthority());
        }
        return response;
    }
}
