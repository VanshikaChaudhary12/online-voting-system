package com.ovs.backend.dto;

import com.ovs.backend.model.MembershipStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public class OrganizationDtos {

    public record CreateOrganizationRequest(
            @NotBlank String name,
            String type
    ) {
    }

    public record OrganizationResponse(
            Long id,
            String name,
            String type,
            boolean active,
            Instant createdAt,
            MembershipStatus membershipStatus
    ) {
    }

    public record AddMemberRequest(
            @Email @NotBlank String email,
            @NotBlank String fullName,
            String password,
            @NotNull MembershipStatus status
    ) {
    }

    public record BulkImportRow(
            String email,
            String fullName
    ) {
    }

    public record BulkImportRequest(
            List<BulkImportRow> rows
    ) {
    }

    public record MembershipResponse(
            Long membershipId,
            Long userId,
            String fullName,
            String email,
            MembershipStatus status,
            Instant joinedAt
    ) {
    }
}
