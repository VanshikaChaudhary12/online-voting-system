package com.ovs.backend.controller;

import com.ovs.backend.dto.VoteDtos;
import com.ovs.backend.service.VoteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/vote")
    public VoteDtos.VoteResponse castVote(@Valid @RequestBody VoteDtos.CastVoteRequest request) {
        return voteService.castVote(request);
    }
}
