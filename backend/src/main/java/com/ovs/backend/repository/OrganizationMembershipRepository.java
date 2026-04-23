package com.ovs.backend.repository;

import com.ovs.backend.model.MembershipStatus;
import com.ovs.backend.model.OrganizationMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {
    List<OrganizationMembership> findByUserId(Long userId);
    List<OrganizationMembership> findByOrganizationId(Long organizationId);
    Optional<OrganizationMembership> findByOrganizationIdAndUserId(Long organizationId, Long userId);
    boolean existsByOrganizationIdAndUserIdAndStatus(Long organizationId, Long userId, MembershipStatus status);
}
