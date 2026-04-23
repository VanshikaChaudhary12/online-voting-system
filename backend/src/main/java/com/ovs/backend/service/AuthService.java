package com.ovs.backend.service;

import com.ovs.backend.dto.AuthDtos;
import com.ovs.backend.exception.ApiException;
import com.ovs.backend.model.Role;
import com.ovs.backend.model.RoleName;
import com.ovs.backend.model.User;
import com.ovs.backend.repository.RoleRepository;
import com.ovs.backend.repository.UserRepository;
import com.ovs.backend.security.JwtService;
import com.ovs.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email already registered");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        Role voterRole = roleRepository.findByName(RoleName.VOTER)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Default role missing"));
        user.getRoles().add(voterRole);

        User saved = userRepository.save(user);
        auditService.log(saved, null, "REGISTER", "USER", saved.getId(), "Self registration");
        return buildAuthResponse(saved);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        log.info("Login attempt for email: {}", request.email());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
            );
            User user = userRepository.findByEmail(request.email().toLowerCase())
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
            auditService.log(user, null, "LOGIN", "USER", user.getId(), "Successful login");
            return buildAuthResponse(user);
        } catch (Exception e) {
            log.error("Login failed for email: {}, error: {}", request.email(), e.getMessage());
            throw e;
        }
    }

    private AuthDtos.AuthResponse buildAuthResponse(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);
        Set<RoleName> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return new AuthDtos.AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(), roles);
    }
}
