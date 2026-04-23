package com.ovs.backend.dto;

import com.ovs.backend.model.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            @Size(min = 6) String password
    ) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record AuthResponse(
            String token,
            Long userId,
            String fullName,
            String email,
            Set<RoleName> roles
    ) {
    }
}
