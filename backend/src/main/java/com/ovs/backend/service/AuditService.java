package com.ovs.backend.service;

import com.ovs.backend.dto.AuditLogResponse;
import com.ovs.backend.model.AuditLog;
import com.ovs.backend.model.Organization;
import com.ovs.backend.model.User;
import com.ovs.backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(User actor, Organization organization, String action, String entityType, Long entityId, String details) {
        AuditLog log = new AuditLog();
        log.setUser(actor);
        log.setOrganization(organization);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    public List<AuditLogResponse> getLogsForOrganization(Long organizationId) {
        return auditLogRepository.findByOrganizationIdOrderByTimestampDesc(organizationId).stream().map(this::map).toList();
    }

    public List<AuditLogResponse> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc().stream().map(this::map).toList();
    }

    private AuditLogResponse map(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getTimestamp(),
                auditLog.getUser() != null ? auditLog.getUser().getEmail() : null,
                auditLog.getOrganization() != null ? auditLog.getOrganization().getName() : null,
                auditLog.getDetails()
        );
    }
}
