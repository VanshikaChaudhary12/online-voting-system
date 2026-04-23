package com.ovs.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ovs.backend.model.Candidate;
import com.ovs.backend.model.Election;
import com.ovs.backend.model.ElectionStatus;
import com.ovs.backend.model.Organization;
import com.ovs.backend.model.ResultVisibility;
import com.ovs.backend.model.User;
import com.ovs.backend.repository.CandidateRepository;
import com.ovs.backend.repository.ElectionRepository;
import com.ovs.backend.repository.OrganizationRepository;
import com.ovs.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OnlineVotingSystemApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private UserRepository userRepository;

    private Long seededOrganizationId;
    private Long seededElectionId;
    private Long seededCandidateId;

    @BeforeEach
    void setUp() {
        Organization organization = organizationRepository.findAll().stream().findFirst().orElseThrow();
        seededOrganizationId = organization.getId();

        Election election = electionRepository.findByOrganizationIdOrderByStartTimeDesc(organization.getId()).stream()
                .findFirst()
                .orElseThrow();
        seededElectionId = election.getId();

        var candidates = candidateRepository.findByElectionIdOrderByCandidateNameAsc(election.getId());
        seededCandidateId = candidates.get(0).getId();
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Online Voting System API is running")));
    }

    @Test
    void loginReturnsJwtAndRoles() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"voter@ovs.local","password":"Password123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email", is("voter@ovs.local")))
                .andExpect(jsonPath("$.roles", hasSize(1)));
    }

    @Test
    void protectedEndpointRejectsAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void voterCanListOwnOrganizationElections() throws Exception {
        mockMvc.perform(get("/api/organizations/{id}/elections", seededOrganizationId)
                        .header("Authorization", bearer(login("voter@ovs.local", "Password123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Board Member Election 2026")));
    }

    @Test
    void outsiderCannotAccessOrganizationElections() throws Exception {
        registerUser("Outsider User", "outsider@ovs.local", "Password123!");

        mockMvc.perform(get("/api/organizations/{id}/elections", seededOrganizationId)
                        .header("Authorization", bearer(login("outsider@ovs.local", "Password123!"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You need active membership in this organization before viewing elections or voting. Ask an admin to activate your membership.")));
    }

    @Test
    void voterCannotSeeResultsBeforeElectionClosesWhenVisibilityIsAfterClosure() throws Exception {
        mockMvc.perform(get("/api/results/{electionId}", seededElectionId)
                        .header("Authorization", bearer(login("voter@ovs.local", "Password123!"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("Results are visible after closure only")));
    }

    @Test
    void orgAdminCanCreateElectionButInvalidWindowIsRejected() throws Exception {
        mockMvc.perform(post("/api/organizations/{organizationId}/elections", seededOrganizationId)
                        .header("Authorization", bearer(login("orgadmin@ovs.local", "Password123!")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Invalid Election",
                                  "description":"Bad dates",
                                  "startTime":"2026-04-01T10:00:00Z",
                                  "endTime":"2026-04-01T09:00:00Z",
                                  "resultVisibility":"AFTER_CLOSURE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("End time must be after start time")));
    }

    @Test
    void voterCanCastOneVoteOnlyOnce() throws Exception {
        String voterToken = login("voter@ovs.local", "Password123!");

        mockMvc.perform(post("/api/vote")
                        .header("Authorization", bearer(voterToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"electionId":%d,"candidateId":%d}
                                """.formatted(seededElectionId, seededCandidateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Vote submitted successfully")));

        mockMvc.perform(post("/api/vote")
                        .header("Authorization", bearer(voterToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"electionId":%d,"candidateId":%d}
                                """.formatted(seededElectionId, seededCandidateId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("You have already voted in this election")));
    }

    @Test
    void voteRequestRejectsCandidateElectionMismatch() throws Exception {
        Long otherElectionId = createElectionWithCandidate();
        Long otherCandidateId = candidateRepository.findByElectionIdOrderByCandidateNameAsc(otherElectionId).get(0).getId();

        mockMvc.perform(post("/api/vote")
                        .header("Authorization", bearer(login("voter@ovs.local", "Password123!")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"electionId":%d,"candidateId":%d}
                                """.formatted(seededElectionId, otherCandidateId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Candidate does not belong to this election")));
    }

    @Test
    void closedElectionRejectsVoteAndThenAllowsResultViewing() throws Exception {
        mockMvc.perform(put("/api/elections/{electionId}/close", seededElectionId)
                        .header("Authorization", bearer(login("orgadmin@ovs.local", "Password123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CLOSED")));

        mockMvc.perform(post("/api/vote")
                        .header("Authorization", bearer(login("voter@ovs.local", "Password123!")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"electionId":%d,"candidateId":%d}
                                """.formatted(seededElectionId, seededCandidateId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Votes can only be cast in active elections")));

        mockMvc.perform(get("/api/results/{electionId}", seededElectionId)
                        .header("Authorization", bearer(login("voter@ovs.local", "Password123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CLOSED")));
    }

    @Test
    void platformAdminCanCreateOrganization() throws Exception {
        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", bearer(login("admin@ovs.local", "Password123!")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Writers Club","type":"College Club"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Writers Club")));
    }

    @Test
    void platformAdminCanListElectionsForNewOrganizationWithoutMembership() throws Exception {
        String adminToken = login("admin@ovs.local", "Password123!");
        String response = mockMvc.perform(post("/api/organizations")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Hostel","type":"College Hostel"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long organizationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/organizations/{id}/elections", organizationId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void registerUser(String fullName, String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"%s","email":"%s","password":"%s"}
                                """.formatted(fullName, email, password)))
                .andExpect(status().isOk());
    }

    private Long createElectionWithCandidate() {
        Organization organization = organizationRepository.findById(seededOrganizationId).orElseThrow();
        User admin = userRepository.findByEmail("orgadmin@ovs.local").orElseThrow();

        Election election = new Election();
        election.setOrganization(organization);
        election.setTitle("Maintenance Committee Election");
        election.setDescription("Secondary election for mismatch testing");
        election.setStartTime(Instant.now().minus(1, ChronoUnit.HOURS));
        election.setEndTime(Instant.now().plus(1, ChronoUnit.DAYS));
        election.setStatus(ElectionStatus.ACTIVE);
        election.setResultVisibility(ResultVisibility.ALWAYS);
        election.setCreatedBy(admin);
        Election savedElection = electionRepository.save(election);

        Candidate candidate = new Candidate();
        candidate.setElection(savedElection);
        candidate.setOrganization(organization);
        candidate.setCandidateName("Mismatch Candidate");
        candidate.setProfileText("Used to verify candidate-election validation.");
        candidateRepository.save(candidate);

        return savedElection.getId();
    }
}
