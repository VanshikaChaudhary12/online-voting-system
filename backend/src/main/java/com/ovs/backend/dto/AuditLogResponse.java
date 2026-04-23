package com.ovs.backend.dto;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String action,
        String entityType,
        Long entityId,
        Instant timestamp,
        String actorEmail,
        String organizationName,
        String details
) {
}
