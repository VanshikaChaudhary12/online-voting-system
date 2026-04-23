package com.ovs.backend.controller;

import com.ovs.backend.dto.ElectionDtos;
import com.ovs.backend.service.ElectionService;
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
@RequestMapping("/api")
public class ElectionController {

    private final ElectionService electionService;

    public ElectionController(ElectionService electionService) {
        this.electionService = electionService;
    }

    @GetMapping("/organizations/{organizationId}/elections")
    public List<ElectionDtos.ElectionResponse> getElections(@PathVariable Long organizationId) {
        return electionService.getElections(organizationId);
    }

    @PostMapping("/organizations/{organizationId}/elections")
    public ElectionDtos.ElectionResponse createElection(@PathVariable Long organizationId,
                                                        @Valid @RequestBody ElectionDtos.CreateElectionRequest request) {
        return electionService.createElection(organizationId, request);
    }

    @PutMapping("/elections/{electionId}")
    public ElectionDtos.ElectionResponse updateElection(@PathVariable Long electionId,
                                                        @Valid @RequestBody ElectionDtos.UpdateElectionRequest request) {
        return electionService.updateElection(electionId, request);
    }

    @PutMapping("/elections/{electionId}/close")
    public ElectionDtos.ElectionResponse closeElection(@PathVariable Long electionId) {
        return electionService.closeElection(electionId);
    }

    @GetMapping("/elections/{electionId}/candidates")
    public List<ElectionDtos.CandidateResponse> getCandidates(@PathVariable Long electionId) {
        return electionService.getCandidates(electionId);
    }

    @PostMapping("/elections/{electionId}/candidates")
    public ElectionDtos.CandidateResponse addCandidate(@PathVariable Long electionId,
                                                       @Valid @RequestBody ElectionDtos.CreateCandidateRequest request) {
        return electionService.addCandidate(electionId, request);
    }

    @GetMapping("/results/{electionId}")
    public ElectionDtos.ResultResponse getResults(@PathVariable Long electionId) {
        return electionService.getResults(electionId);
    }
}
