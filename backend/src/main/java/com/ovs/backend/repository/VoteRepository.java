package com.ovs.backend.repository;

import com.ovs.backend.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByElectionIdAndUserId(Long electionId, Long userId);

    @Query("""
            select v.candidate.id, count(v)
            from Vote v
            where v.election.id = :electionId
            group by v.candidate.id
            """)
    List<Object[]> countVotesByElection(@Param("electionId") Long electionId);
}
