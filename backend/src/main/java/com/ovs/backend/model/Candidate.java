package com.ovs.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "candidates", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"election_id", "candidateName"})
})
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id")
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "candidate_name", nullable = false)
    private String candidateName;

    @Column(length = 2000)
    private String profileText;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getProfileText() {
        return profileText;
    }

    public void setProfileText(String profileText) {
        this.profileText = profileText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
