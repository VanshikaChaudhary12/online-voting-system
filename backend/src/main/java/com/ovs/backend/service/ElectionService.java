package com.ovs.backend.service;

import com.ovs.backend.dto.ElectionDtos;
import com.ovs.backend.exception.ApiException;
import com.ovs.backend.model.Candidate;
import com.ovs.backend.model.Election;
import com.ovs.backend.model.ElectionStatus;
import com.ovs.backend.model.Organization;
import com.ovs.backend.model.ResultVisibility;
import com.ovs.backend.model.RoleName;
import com.ovs.backend.model.User;
import com.ovs.backend.repository.CandidateRepository;
import com.ovs.backend.repository.ElectionRepository;
import com.ovs.backend.repository.VoteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElectionService {

    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;
    private final CurrentUserService currentUserService;
    private final OrganizationService organizationService;
    private final AuditService auditService;

    public ElectionService(ElectionRepository electionRepository,
                           CandidateRepository candidateRepository,
                           VoteRepository voteRepository,
                           CurrentUserService currentUserService,
                           OrganizationService organizationService,
                           AuditService auditService) {
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
        this.voteRepository = voteRepository;
        this.currentUserService = currentUserService;
        this.organizationService = organizationService;
        this.auditService = auditService;
    }

    public ElectionDtos.ElectionResponse createElection(Long organizationId, ElectionDtos.CreateElectionRequest request) {
        User actor = organizationService.ensureAdminForOrganization(organizationId);
        validateElectionWindow(request.startTime(), request.endTime());
        if (request.startTime().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Start time cannot be in the past");
        }

        Organization organization = organizationService.getOrganization(organizationId);
        Election election = new Election();
        election.setOrganization(organization);
        election.setTitle(request.title());
        election.setDescription(request.description());
        election.setStartTime(request.startTime());
        election.setEndTime(request.endTime());
        election.setResultVisibility(request.resultVisibility());
        election.setStatus(resolveStatus(request.startTime(), request.endTime(), null));
        election.setCreatedBy(actor);

        Election saved = electionRepository.save(election);
        auditService.log(actor, organization, "CREATE_ELECTION", "ELECTION", saved.getId(), saved.getTitle());
        return mapElection(saved, actor.getId());
    }

    public List<ElectionDtos.ElectionResponse> getElections(Long organizationId) {
        User actor = currentUserService.getCurrentUser();
        organizationService.ensureActiveMembership(organizationId, actor.getId());
        return electionRepository.findByOrganizationIdOrderByStartTimeDesc(organizationId).stream()
                .peek(this::refreshStatus)
                .map(election -> mapElection(election, actor.getId()))
                .toList();
    }

    public ElectionDtos.ElectionResponse updateElection(Long electionId, ElectionDtos.UpdateElectionRequest request) {
        Election election = getElection(electionId);
        User actor = organizationService.ensureAdminForOrganization(election.getOrganization().getId());
        if (election.getStatus() == ElectionStatus.CLOSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Closed elections cannot be updated");
        }
        validateElectionWindow(request.startTime(), request.endTime());
        election.setTitle(request.title());
        election.setDescription(request.description());
        election.setStartTime(request.startTime());
        election.setEndTime(request.endTime());
        election.setResultVisibility(request.resultVisibility());
        refreshStatus(election);
        Election saved = electionRepository.save(election);
        auditService.log(actor, election.getOrganization(), "UPDATE_ELECTION", "ELECTION", saved.getId(), saved.getTitle());
        return mapElection(saved, actor.getId());
    }

    public ElectionDtos.ElectionResponse closeElection(Long electionId) {
        Election election = getElection(electionId);
        User actor = organizationService.ensureAdminForOrganization(election.getOrganization().getId());
        if (election.getStatus() == ElectionStatus.CLOSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Election is already closed");
        }
        election.setStatus(ElectionStatus.CLOSED);
        Election saved = electionRepository.save(election);
        auditService.log(actor, election.getOrganization(), "CLOSE_ELECTION", "ELECTION", saved.getId(), saved.getTitle());
        return mapElection(saved, actor.getId());
    }

    public ElectionDtos.CandidateResponse addCandidate(Long electionId, ElectionDtos.CreateCandidateRequest request) {
        Election election = getElection(electionId);
        User actor = organizationService.ensureAdminForOrganization(election.getOrganization().getId());
        if (election.getStatus() == ElectionStatus.CLOSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Closed elections cannot be changed");
        }
        if (candidateRepository.existsByElectionIdAndCandidateNameIgnoreCase(electionId, request.candidateName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Candidate already exists in this election");
        }
        Candidate candidate = new Candidate();
        candidate.setElection(election);
        candidate.setOrganization(election.getOrganization());
        candidate.setCandidateName(request.candidateName());
        candidate.setProfileText(request.profileText());
        Candidate saved = candidateRepository.save(candidate);
        auditService.log(actor, election.getOrganization(), "ADD_CANDIDATE", "CANDIDATE", saved.getId(), saved.getCandidateName());
        return mapCandidate(saved);
    }

    public List<ElectionDtos.CandidateResponse> getCandidates(Long electionId) {
        Election election = getElection(electionId);
        User actor = currentUserService.getCurrentUser();
        organizationService.ensureActiveMembership(election.getOrganization().getId(), actor.getId());
        return candidateRepository.findByElectionIdOrderByCandidateNameAsc(electionId).stream().map(this::mapCandidate).toList();
    }

    public ElectionDtos.ResultResponse getResults(Long electionId) {
        Election election = getElection(electionId);
        User actor = currentUserService.getCurrentUser();
        boolean isAdmin = actor.getRoles().stream().anyMatch(role ->
                role.getName() == RoleName.PLATFORM_ADMIN || role.getName() == RoleName.ORGANIZATION_ADMIN);
        if (isAdmin) {
            organizationService.ensureAdminForOrganization(election.getOrganization().getId());
        } else {
            organizationService.ensureActiveMembership(election.getOrganization().getId(), actor.getId());
            refreshStatus(election);
            if (election.getResultVisibility() == ResultVisibility.AFTER_CLOSURE && election.getStatus() != ElectionStatus.CLOSED) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Results are visible after closure only");
            }
        }

        Map<Long, Long> counts = new HashMap<>();
        voteRepository.countVotesByElection(electionId).forEach(row -> counts.put((Long) row[0], (Long) row[1]));
        List<ElectionDtos.ResultItem> items = candidateRepository.findByElectionIdOrderByCandidateNameAsc(electionId).stream()
                .map(candidate -> new ElectionDtos.ResultItem(
                        candidate.getId(),
                        candidate.getCandidateName(),
                        counts.getOrDefault(candidate.getId(), 0L)
                )).toList();
        return new ElectionDtos.ResultResponse(election.getId(), election.getTitle(), election.getStatus(), items);
    }

    public Election getElection(Long electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Election not found"));
        refreshStatus(election);
        return election;
    }

    private void validateElectionWindow(Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
    }

    private ElectionDtos.ElectionResponse mapElection(Election election, Long userId) {
        return new ElectionDtos.ElectionResponse(
                election.getId(),
                election.getOrganization().getId(),
                election.getOrganization().getName(),
                election.getTitle(),
                election.getDescription(),
                election.getStartTime(),
                election.getEndTime(),
                election.getStatus(),
                election.getResultVisibility(),
                voteRepository.existsByElectionIdAndUserId(election.getId(), userId)
        );
    }

    private ElectionDtos.CandidateResponse mapCandidate(Candidate candidate) {
        return new ElectionDtos.CandidateResponse(
                candidate.getId(),
                candidate.getElection().getId(),
                candidate.getCandidateName(),
                candidate.getProfileText()
        );
    }

    private void refreshStatus(Election election) {
        ElectionStatus resolved = resolveStatus(election.getStartTime(), election.getEndTime(), election.getStatus());
        if (resolved != election.getStatus()) {
            election.setStatus(resolved);
            electionRepository.save(election);
        }
    }

    private ElectionStatus resolveStatus(Instant startTime, Instant endTime, ElectionStatus currentStatus) {
        if (currentStatus == ElectionStatus.CLOSED) {
            return ElectionStatus.CLOSED;
        }
        Instant now = Instant.now();
        if (now.isBefore(startTime)) {
            return ElectionStatus.UPCOMING;
        }
        if (now.isAfter(endTime)) {
            return ElectionStatus.CLOSED;
        }
        return ElectionStatus.ACTIVE;
    }
}
