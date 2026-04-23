package com.ovs.backend.security;

import com.ovs.backend.model.RoleName;
import com.ovs.backend.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Set<RoleName> roles;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.username = user.getEmail();
        this.password = user.getPasswordHash();
        this.roles = user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet());
        this.authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }

    public Long getId() {
        return id;
    }

    public Set<RoleName> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
