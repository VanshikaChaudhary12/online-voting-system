package com.ovs.backend.repository;

import com.ovs.backend.model.Role;
import com.ovs.backend.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
