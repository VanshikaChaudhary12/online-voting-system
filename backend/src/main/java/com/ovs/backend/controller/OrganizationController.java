package com.ovs.backend.controller;

import com.ovs.backend.dto.OrganizationDtos;
import com.ovs.backend.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    public OrganizationDtos.OrganizationResponse createOrganization(@Valid @RequestBody OrganizationDtos.CreateOrganizationRequest request) {
        return organizationService.createOrganization(request);
    }

    @GetMapping
    public List<OrganizationDtos.OrganizationResponse> getOrganizations() {
        return organizationService.getOrganizations();
    }

    @GetMapping("/{organizationId}/members")
    public List<OrganizationDtos.MembershipResponse> getMembers(@PathVariable Long organizationId) {
        return organizationService.getMembers(organizationId);
    }

    @PostMapping("/{organizationId}/members")
    public OrganizationDtos.MembershipResponse addMember(@PathVariable Long organizationId,
                                                         @Valid @RequestBody OrganizationDtos.AddMemberRequest request) {
        return organizationService.addMember(organizationId, request);
    }

    @PostMapping("/{organizationId}/members/bulk-import")
    public List<OrganizationDtos.MembershipResponse> bulkImport(@PathVariable Long organizationId,
                                                                @RequestBody OrganizationDtos.BulkImportRequest request) {
        return organizationService.bulkImport(organizationId, request);
    }

    @PutMapping("/{organizationId}/members/{membershipId}/approve")
    public OrganizationDtos.MembershipResponse approveMembership(@PathVariable Long organizationId,
                                                                @PathVariable Long membershipId) {
        return organizationService.approveMembership(organizationId, membershipId);
    }
}
