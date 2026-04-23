package com.ovs.backend.service;

import com.ovs.backend.exception.ApiException;
import com.ovs.backend.model.RoleName;
import com.ovs.backend.model.User;
import com.ovs.backend.repository.UserRepository;
import com.ovs.backend.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public void requireRole(User user, RoleName roleName) {
        boolean hasRole = user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
        if (!hasRole) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
    }
}
