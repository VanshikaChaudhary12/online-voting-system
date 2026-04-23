package com.ovs.backend.repository;

import com.ovs.backend.model.Election;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ElectionRepository extends JpaRepository<Election, Long> {
    List<Election> findByOrganizationIdOrderByStartTimeDesc(Long organizationId);
}
