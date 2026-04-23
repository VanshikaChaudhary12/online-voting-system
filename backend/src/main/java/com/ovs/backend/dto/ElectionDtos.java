package com.ovs.backend.dto;

import com.ovs.backend.model.ElectionStatus;
import com.ovs.backend.model.ResultVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public class ElectionDtos {

    public record CreateElectionRequest(
            @NotBlank String title,
            String description,
            @NotNull Instant startTime,
            @NotNull Instant endTime,
            @NotNull ResultVisibility resultVisibility
    ) {
    }

    public record UpdateElectionRequest(
            @NotBlank String title,
            String description,
            @NotNull Instant startTime,
            @NotNull Instant endTime,
            @NotNull ResultVisibility resultVisibility
    ) {
    }

    public record ElectionResponse(
            Long id,
            Long organizationId,
            String organizationName,
            String title,
            String description,
            Instant startTime,
            Instant endTime,
            ElectionStatus status,
            ResultVisibility resultVisibility,
            boolean hasVoted
    ) {
    }

    public record CreateCandidateRequest(
            @NotBlank String candidateName,
            String profileText
    ) {
    }

    public record CandidateResponse(
            Long id,
            Long electionId,
            String candidateName,
            String profileText
    ) {
    }

    public record ResultItem(
            Long candidateId,
            String candidateName,
            long voteCount
    ) {
    }

    public record ResultResponse(
            Long electionId,
            String electionTitle,
            ElectionStatus status,
            List<ResultItem> results
    ) {
    }
}
