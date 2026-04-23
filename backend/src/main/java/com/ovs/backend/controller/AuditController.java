package com.ovs.backend.controller;

import com.ovs.backend.dto.AuditLogResponse;
import com.ovs.backend.model.RoleName;
import com.ovs.backend.model.User;
import com.ovs.backend.service.AuditService;
import com.ovs.backend.service.CurrentUserService;
import com.ovs.backend.service.OrganizationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final OrganizationService organizationService;

    public AuditController(AuditService auditService,
                           CurrentUserService currentUserService,
                           OrganizationService organizationService) {
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.organizationService = organizationService;
    }

    @GetMapping
    public List<AuditLogResponse> getLogs(@RequestParam(required = false) Long organizationId) {
        User actor = currentUserService.getCurrentUser();
        boolean platformAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.PLATFORM_ADMIN);
        if (organizationId != null) {
            organizationService.ensureAdminForOrganization(organizationId);
            return auditService.getLogsForOrganization(organizationId);
        }
        currentUserService.requireRole(actor, RoleName.PLATFORM_ADMIN);
        if (platformAdmin) {
            return auditService.getAllLogs();
        }
        return List.of();
    }
}
