package com.ovs.backend.repository;

import com.ovs.backend.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByOrganizationIdOrderByTimestampDesc(Long organizationId);
    List<AuditLog> findAllByOrderByTimestampDesc();
}
