package com.ovs.backend.config;

import com.ovs.backend.model.Candidate;
import com.ovs.backend.model.Election;
import com.ovs.backend.model.ElectionStatus;
import com.ovs.backend.model.MembershipStatus;
import com.ovs.backend.model.Organization;
import com.ovs.backend.model.OrganizationMembership;
import com.ovs.backend.model.ResultVisibility;
import com.ovs.backend.model.Role;
import com.ovs.backend.model.RoleName;
import com.ovs.backend.model.User;
import com.ovs.backend.repository.CandidateRepository;
import com.ovs.backend.repository.ElectionRepository;
import com.ovs.backend.repository.OrganizationMembershipRepository;
import com.ovs.backend.repository.OrganizationRepository;
import com.ovs.backend.repository.RoleRepository;
import com.ovs.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(RoleRepository roleRepository,
                               UserRepository userRepository,
                               OrganizationRepository organizationRepository,
                               OrganizationMembershipRepository membershipRepository,
                               ElectionRepository electionRepository,
                               CandidateRepository candidateRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (roleRepository.count() > 0) {
                return;
            }

            Role platformAdminRole = roleRepository.save(new Role(RoleName.PLATFORM_ADMIN));
            Role organizationAdminRole = roleRepository.save(new Role(RoleName.ORGANIZATION_ADMIN));
            Role voterRole = roleRepository.save(new Role(RoleName.VOTER));

            User platformAdmin = createUser("Platform Admin", "admin@ovs.local", passwordEncoder, List.of(platformAdminRole, organizationAdminRole, voterRole));
            User organizationAdmin = createUser("Community Admin", "orgadmin@ovs.local", passwordEncoder, List.of(organizationAdminRole, voterRole));
            User voter = createUser("Demo Voter", "voter@ovs.local", passwordEncoder, List.of(voterRole));

            userRepository.saveAll(List.of(platformAdmin, organizationAdmin, voter));

            Organization organization = new Organization();
            organization.setName("Sunrise Residency Association");
            organization.setType("Housing Society");
            organizationRepository.save(organization);

            membershipRepository.save(createMembership(organization, organizationAdmin, MembershipStatus.ACTIVE, platformAdmin));
            membershipRepository.save(createMembership(organization, voter, MembershipStatus.ACTIVE, organizationAdmin));
            membershipRepository.save(createMembership(organization, platformAdmin, MembershipStatus.ACTIVE, platformAdmin));

            Election election = new Election();
            election.setOrganization(organization);
            election.setTitle("Board Member Election 2026");
            election.setDescription("Vote for the next resident board member.");
            election.setStartTime(Instant.now().minus(1, ChronoUnit.DAYS));
            election.setEndTime(Instant.now().plus(2, ChronoUnit.DAYS));
            election.setStatus(ElectionStatus.ACTIVE);
            election.setResultVisibility(ResultVisibility.AFTER_CLOSURE);
            election.setCreatedBy(organizationAdmin);
            electionRepository.save(election);

            Candidate candidateOne = new Candidate();
            candidateOne.setElection(election);
            candidateOne.setOrganization(organization);
            candidateOne.setCandidateName("Asha Mehta");
            candidateOne.setProfileText("Focus on maintenance transparency and resident communication.");

            Candidate candidateTwo = new Candidate();
            candidateTwo.setElection(election);
            candidateTwo.setOrganization(organization);
            candidateTwo.setCandidateName("Rohan Iyer");
            candidateTwo.setProfileText("Focus on security, energy savings, and digital operations.");

            candidateRepository.saveAll(List.of(candidateOne, candidateTwo));
        };
    }

    private User createUser(String fullName, String email, PasswordEncoder passwordEncoder, List<Role> roles) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.getRoles().addAll(roles);
        return user;
    }

    private OrganizationMembership createMembership(Organization organization, User user, MembershipStatus status, User approvedBy) {
        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(organization);
        membership.setUser(user);
        membership.setStatus(status);
        membership.setApprovedBy(approvedBy);
        return membership;
    }
}
