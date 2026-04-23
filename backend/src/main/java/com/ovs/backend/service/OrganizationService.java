package com.ovs.backend.service;

import com.ovs.backend.dto.OrganizationDtos;
import com.ovs.backend.exception.ApiException;
import com.ovs.backend.model.MembershipStatus;
import com.ovs.backend.model.Organization;
import com.ovs.backend.model.OrganizationMembership;
import com.ovs.backend.model.Role;
import com.ovs.backend.model.RoleName;
import com.ovs.backend.model.User;
import com.ovs.backend.repository.OrganizationMembershipRepository;
import com.ovs.backend.repository.OrganizationRepository;
import com.ovs.backend.repository.RoleRepository;
import com.ovs.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public OrganizationService(OrganizationRepository organizationRepository,
                               OrganizationMembershipRepository membershipRepository,
                               UserRepository userRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder,
                               CurrentUserService currentUserService,
                               AuditService auditService) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    public OrganizationDtos.OrganizationResponse createOrganization(OrganizationDtos.CreateOrganizationRequest request) {
        User actor = currentUserService.getCurrentUser();
        currentUserService.requireRole(actor, RoleName.PLATFORM_ADMIN);

        Organization organization = new Organization();
        organization.setName(request.name());
        organization.setType(request.type());
        Organization saved = organizationRepository.save(organization);
        auditService.log(actor, saved, "CREATE_ORGANIZATION", "ORGANIZATION", saved.getId(), saved.getName());
        return mapOrganization(saved, null);
    }

    public List<OrganizationDtos.OrganizationResponse> getOrganizations() {
        User actor = currentUserService.getCurrentUser();
        boolean platformAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.PLATFORM_ADMIN);
        if (platformAdmin) {
            return organizationRepository.findAll().stream().map(org -> mapOrganization(org, null)).toList();
        }
        return membershipRepository.findByUserId(actor.getId()).stream()
                .map(membership -> mapOrganization(membership.getOrganization(), membership.getStatus()))
                .toList();
    }

    public List<OrganizationDtos.MembershipResponse> getMembers(Long organizationId) {
        ensureAdminForOrganization(organizationId);
        return membershipRepository.findByOrganizationId(organizationId).stream().map(this::mapMembership).toList();
    }

    public OrganizationDtos.MembershipResponse addMember(Long organizationId, OrganizationDtos.AddMemberRequest request) {
        User actor = ensureAdminForOrganization(organizationId);
        Organization organization = getOrganization(organizationId);

        User user = userRepository.findByEmail(request.email().toLowerCase()).orElseGet(() -> {
            User created = new User();
            created.setFullName(request.fullName());
            created.setEmail(request.email());
            created.setPasswordHash(passwordEncoder.encode(
                    request.password() == null || request.password().isBlank() ? "ChangeMe123!" : request.password()
            ));
            Role voterRole = roleRepository.findByName(RoleName.VOTER)
                    .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Default role missing"));
            created.getRoles().add(voterRole);
            return userRepository.save(created);
        });

        OrganizationMembership membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, user.getId())
                .orElseGet(OrganizationMembership::new);
        membership.setOrganization(organization);
        membership.setUser(user);
        membership.setStatus(request.status());
        if (request.status() == MembershipStatus.ACTIVE) {
            membership.setApprovedBy(actor);
        }
        OrganizationMembership saved = membershipRepository.save(membership);
        auditService.log(actor, organization, "ADD_MEMBER", "MEMBERSHIP", saved.getId(), user.getEmail());
        return mapMembership(saved);
    }

    public List<OrganizationDtos.MembershipResponse> bulkImport(Long organizationId, OrganizationDtos.BulkImportRequest request) {
        return request.rows().stream()
                .filter(row -> row.email() != null && !row.email().isBlank() && row.fullName() != null && !row.fullName().isBlank())
                .map(row -> addMember(organizationId, new OrganizationDtos.AddMemberRequest(row.email(), row.fullName(), "ChangeMe123!", MembershipStatus.ACTIVE)))
                .toList();
    }

    public OrganizationDtos.MembershipResponse approveMembership(Long organizationId, Long membershipId) {
        User actor = ensureAdminForOrganization(organizationId);
        OrganizationMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));
        if (!membership.getOrganization().getId().equals(organizationId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Membership does not belong to this organization");
        }
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setApprovedBy(actor);
        OrganizationMembership saved = membershipRepository.save(membership);
        auditService.log(actor, saved.getOrganization(), "APPROVE_MEMBERSHIP", "MEMBERSHIP", saved.getId(), saved.getUser().getEmail());
        return mapMembership(saved);
    }

    public User ensureAdminForOrganization(Long organizationId) {
        User actor = currentUserService.getCurrentUser();
        boolean platformAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.PLATFORM_ADMIN);
        if (platformAdmin) {
            return actor;
        }
        boolean orgAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ORGANIZATION_ADMIN);
        boolean activeMember = membershipRepository.existsByOrganizationIdAndUserIdAndStatus(organizationId, actor.getId(), MembershipStatus.ACTIVE);
        if (!orgAdmin || !activeMember) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Organization admin access required");
        }
        return actor;
    }

    public void ensureActiveMembership(Long organizationId, Long userId) {
        User actor = currentUserService.getCurrentUser();
        boolean platformAdmin = actor.getRoles().stream().anyMatch(role -> role.getName() == RoleName.PLATFORM_ADMIN);
        if (platformAdmin && actor.getId().equals(userId)) {
            return;
        }
        if (!membershipRepository.existsByOrganizationIdAndUserIdAndStatus(organizationId, userId, MembershipStatus.ACTIVE)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You need active membership in this organization before viewing elections or voting. Ask an admin to activate your membership.");
        }
    }

    public Organization getOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    private OrganizationDtos.OrganizationResponse mapOrganization(Organization organization, MembershipStatus membershipStatus) {
        return new OrganizationDtos.OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getType(),
                organization.isActive(),
                organization.getCreatedAt(),
                membershipStatus
        );
    }

    private OrganizationDtos.MembershipResponse mapMembership(OrganizationMembership membership) {
        return new OrganizationDtos.MembershipResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getFullName(),
                membership.getUser().getEmail(),
                membership.getStatus(),
                membership.getJoinedAt()
        );
    }
}
