package com.ovs.backend.service;

import com.ovs.backend.dto.VoteDtos;
import com.ovs.backend.exception.ApiException;
import com.ovs.backend.model.Candidate;
import com.ovs.backend.model.Election;
import com.ovs.backend.model.ElectionStatus;
import com.ovs.backend.model.User;
import com.ovs.backend.model.Vote;
import com.ovs.backend.repository.CandidateRepository;
import com.ovs.backend.repository.VoteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class VoteService {

    private final VoteRepository voteRepository;
    private final CandidateRepository candidateRepository;
    private final CurrentUserService currentUserService;
    private final ElectionService electionService;
    private final OrganizationService organizationService;
    private final AuditService auditService;

    public VoteService(VoteRepository voteRepository,
                       CandidateRepository candidateRepository,
                       CurrentUserService currentUserService,
                       ElectionService electionService,
                       OrganizationService organizationService,
                       AuditService auditService) {
        this.voteRepository = voteRepository;
        this.candidateRepository = candidateRepository;
        this.currentUserService = currentUserService;
        this.electionService = electionService;
        this.organizationService = organizationService;
        this.auditService = auditService;
    }

    public VoteDtos.VoteResponse castVote(VoteDtos.CastVoteRequest request) {
        User actor = currentUserService.getCurrentUser();
        Election election = electionService.getElection(request.electionId());
        organizationService.ensureActiveMembership(election.getOrganization().getId(), actor.getId());

        if (election.getStatus() != ElectionStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Votes can only be cast in active elections");
        }
        if (voteRepository.existsByElectionIdAndUserId(election.getId(), actor.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You have already voted in this election");
        }

        Candidate candidate = candidateRepository.findById(request.candidateId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Candidate not found"));
        if (!candidate.getElection().getId().equals(election.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Candidate does not belong to this election");
        }

        Vote vote = new Vote();
        vote.setElection(election);
        vote.setCandidate(candidate);
        vote.setUser(actor);
        voteRepository.save(vote);
        auditService.log(actor, election.getOrganization(), "CAST_VOTE", "VOTE", election.getId(), candidate.getCandidateName());
        return new VoteDtos.VoteResponse("Vote submitted successfully");
    }
}
