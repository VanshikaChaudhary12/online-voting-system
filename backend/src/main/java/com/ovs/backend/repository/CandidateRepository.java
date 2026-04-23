package com.ovs.backend.repository;

import com.ovs.backend.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findByElectionIdOrderByCandidateNameAsc(Long electionId);

    boolean existsByElectionIdAndCandidateNameIgnoreCase(Long electionId, String candidateName);
}
