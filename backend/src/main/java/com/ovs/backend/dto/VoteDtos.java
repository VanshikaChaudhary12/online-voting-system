package com.ovs.backend.dto;

import jakarta.validation.constraints.NotNull;

public class VoteDtos {

    public record CastVoteRequest(
            @NotNull Long electionId,
            @NotNull Long candidateId
    ) {
    }

    public record VoteResponse(
            String message
    ) {
    }
}
