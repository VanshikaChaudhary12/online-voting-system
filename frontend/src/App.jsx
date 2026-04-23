import { useEffect, useState } from "react";
import { apiRequest } from "./api/client";
import { useAuth } from "./context/AuthContext";
import SectionCard from "./components/SectionCard";
import StatCard from "./components/StatCard";

const emptyLogin = { email: "", password: "" };
const emptyRegister = { fullName: "", email: "", password: "" };
const emptyOrg = { name: "", type: "" };
const emptyMember = { email: "", fullName: "", password: "", status: "ACTIVE" };
const emptyElection = {
  title: "",
  description: "",
  startTime: "",
  endTime: "",
  resultVisibility: "AFTER_CLOSURE"
};
const emptyCandidate = { candidateName: "", profileText: "" };
const emptyDashboard = {
  organizations: [],
  elections: [],
  candidates: [],
  members: [],
  results: null,
  auditLogs: []
};

function App() {
  const { auth, login, logout } = useAuth();
  const [mode, setMode] = useState("login");
  const [activeTab, setActiveTab] = useState("overview");
  const [loginForm, setLoginForm] = useState(emptyLogin);
  const [registerForm, setRegisterForm] = useState(emptyRegister);
  const [organizationForm, setOrganizationForm] = useState(emptyOrg);
  const [memberForm, setMemberForm] = useState(emptyMember);
  const [electionForm, setElectionForm] = useState(emptyElection);
  const [candidateForm, setCandidateForm] = useState(emptyCandidate);
  const [dashboard, setDashboard] = useState(emptyDashboard);
  const [selectedOrganizationId, setSelectedOrganizationId] = useState(null);
  const [selectedElectionId, setSelectedElectionId] = useState(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const roles = auth?.roles || [];
  const isPlatformAdmin = roles.includes("PLATFORM_ADMIN");
  const isOrganizationAdmin = roles.includes("ORGANIZATION_ADMIN");
  const canManage = isOrganizationAdmin || isPlatformAdmin;
  const currentOrganization = dashboard.organizations.find((item) => item.id === selectedOrganizationId);
  const selectedElection = dashboard.elections.find((item) => item.id === selectedElectionId);
  const isSelectedElectionClosed = selectedElection?.status === "CLOSED";
  const canEditSelectedElection = canManage && selectedElectionId && selectedElection?.status !== "CLOSED";
  const selectedResults = dashboard.results?.electionId === selectedElectionId ? dashboard.results : null;
  const resultItems = selectedResults?.results || [];
  const totalVotes = resultItems.reduce((sum, result) => sum + result.voteCount, 0);
  const highestVoteCount = resultItems.length ? Math.max(...resultItems.map((result) => result.voteCount)) : 0;
  const winners = highestVoteCount > 0
    ? resultItems.filter((result) => result.voteCount === highestVoteCount)
    : [];
  const minimumElectionDateTime = toDateTimeLocalValue(new Date(Date.now() + 60000));
  const minimumElectionEndTime = electionForm.startTime || minimumElectionDateTime;

  useEffect(() => {
    setError("");
    setMessage("");
    if (!auth?.token) {
      setDashboard(emptyDashboard);
      setSelectedOrganizationId(null);
      setSelectedElectionId(null);
      return;
    }
    setDashboard(emptyDashboard);
    setSelectedOrganizationId(null);
    setSelectedElectionId(null);
    loadOrganizations(true);
  }, [auth?.token]);

  useEffect(() => {
    if (selectedOrganizationId) {
      loadOrganizationDetails(selectedOrganizationId);
    }
  }, [selectedOrganizationId]);

  useEffect(() => {
    if (selectedElectionId) {
      loadCandidates(selectedElectionId);
      loadResults(selectedElectionId);
    }
  }, [selectedElectionId]);

  useEffect(() => {
    if (activeTab === "winner" && !isSelectedElectionClosed) {
      setActiveTab("results");
    }
  }, [activeTab, isSelectedElectionClosed]);

  async function handleAuthSubmit(event, kind) {
    event.preventDefault();
    setError("");
    setMessage("");
    setLoading(true);
    try {
      const payload = kind === "login" 
        ? { email: loginForm.email.trim(), password: loginForm.password.trim() }
        : { fullName: registerForm.fullName.trim(), email: registerForm.email.trim(), password: registerForm.password.trim() };
      const data = await apiRequest(`/auth/${kind}`, {
        method: "POST",
        body: JSON.stringify(payload)
      });
      login(data);
      setMode("login");
      setLoginForm(emptyLogin);
      setRegisterForm(emptyRegister);
      setMessage(`Signed in as ${data.fullName}`);
    } catch (authError) {
      setError(authError.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadOrganizations(resetSelection = false) {
    try {
      const organizations = await apiRequest("/organizations", {}, auth.token);
      setDashboard((current) => ({ ...current, organizations }));
      if (organizations.length && (resetSelection || !selectedOrganizationId)) {
        setSelectedOrganizationId(organizations[0].id);
      }
    } catch (loadError) {
      setError(loadError.message);
    }
  }

  async function loadOrganizationDetails(organizationId) {
    try {
      const elections = await apiRequest(`/organizations/${organizationId}/elections`, {}, auth.token);
      setDashboard((current) => ({ ...current, elections }));
      if (elections.length) {
        setSelectedElectionId(elections[0].id);
      } else {
        setSelectedElectionId(null);
        setDashboard((current) => ({ ...current, candidates: [], results: null }));
      }

      if (isOrganizationAdmin || isPlatformAdmin) {
        const [members, auditLogs] = await Promise.all([
          apiRequest(`/organizations/${organizationId}/members`, {}, auth.token),
          apiRequest(`/audit-logs?organizationId=${organizationId}`, {}, auth.token)
        ]);
        setDashboard((current) => ({ ...current, members, auditLogs }));
      } else {
        setDashboard((current) => ({ ...current, members: [], auditLogs: [] }));
      }
    } catch (loadError) {
      setError(loadError.message);
    }
  }

  async function loadCandidates(electionId) {
    try {
      const candidates = await apiRequest(`/elections/${electionId}/candidates`, {}, auth.token);
      setDashboard((current) => ({ ...current, candidates }));
    } catch (loadError) {
      setError(loadError.message);
    }
  }

  async function loadResults(electionId) {
    try {
      const results = await apiRequest(`/results/${electionId}`, {}, auth.token);
      setDashboard((current) => ({ ...current, results }));
    } catch (loadError) {
      setDashboard((current) => ({ ...current, results: null }));
    }
  }

  async function handleCreateOrganization(event) {
    event.preventDefault();
    await submitAction(async () => {
      await apiRequest("/organizations", {
        method: "POST",
        body: JSON.stringify(organizationForm)
      }, auth.token);
      setOrganizationForm(emptyOrg);
      await loadOrganizations();
      setMessage("Organization created");
    });
  }

  async function handleAddMember(event) {
    event.preventDefault();
    await submitAction(async () => {
      await apiRequest(`/organizations/${selectedOrganizationId}/members`, {
        method: "POST",
        body: JSON.stringify(memberForm)
      }, auth.token);
      setMemberForm(emptyMember);
      await loadOrganizationDetails(selectedOrganizationId);
      setMessage("Member saved");
    });
  }

  async function handleCreateElection(event) {
    event.preventDefault();
    const startTime = new Date(electionForm.startTime);
    const endTime = new Date(electionForm.endTime);
    if (!electionForm.startTime || startTime < new Date()) {
      setError("Start time cannot be in the past");
      return;
    }
    if (!electionForm.endTime || endTime <= startTime) {
      setError("End time must be after start time");
      return;
    }

    await submitAction(async () => {
      await apiRequest(`/organizations/${selectedOrganizationId}/elections`, {
        method: "POST",
        body: JSON.stringify({
          ...electionForm,
          startTime: new Date(electionForm.startTime).toISOString(),
          endTime: new Date(electionForm.endTime).toISOString()
        })
      }, auth.token);
      setElectionForm(emptyElection);
      await loadOrganizationDetails(selectedOrganizationId);
      setMessage("Election created");
    });
  }

  async function handleAddCandidate(event) {
    event.preventDefault();
    await submitAction(async () => {
      await apiRequest(`/elections/${selectedElectionId}/candidates`, {
        method: "POST",
        body: JSON.stringify(candidateForm)
      }, auth.token);
      setCandidateForm(emptyCandidate);
      await loadCandidates(selectedElectionId);
      await loadResults(selectedElectionId);
      setMessage("Candidate added");
    });
  }

  async function handleVote(candidateId) {
    await submitAction(async () => {
      await apiRequest("/vote", {
        method: "POST",
        body: JSON.stringify({ electionId: selectedElectionId, candidateId })
      }, auth.token);
      await loadOrganizationDetails(selectedOrganizationId);
      await loadResults(selectedElectionId);
      setMessage("Vote submitted");
    });
  }

  async function handleCloseElection(electionId) {
    const election = dashboard.elections.find((item) => item.id === electionId);
    const confirmed = window.confirm(`Close "${election?.title || "this election"}"? This will stop voting and show the final winner.`);
    if (!confirmed) {
      return;
    }

    await submitAction(async () => {
      await apiRequest(`/elections/${electionId}/close`, { method: "PUT" }, auth.token);
      await loadOrganizationDetails(selectedOrganizationId);
      await loadResults(electionId);
      setSelectedElectionId(electionId);
      setActiveTab("winner");
      setMessage("Election closed");
    });
  }

  function formatDateTime(value) {
    return value ? new Date(value).toLocaleString() : "Not set";
  }

  function toDateTimeLocalValue(date) {
    const offsetDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return offsetDate.toISOString().slice(0, 16);
  }

  function getVoteButtonLabel() {
    if (!selectedElection) {
      return "Select election";
    }
    if (selectedElection.hasVoted) {
      return "Vote submitted";
    }
    if (selectedElection.status === "UPCOMING") {
      return "Voting not open";
    }
    if (selectedElection.status === "CLOSED") {
      return "Election closed";
    }
    return "Vote";
  }

  function getElectionParticipationLabel(election) {
    if (election.hasVoted) {
      return "You have voted";
    }
    if (election.status === "ACTIVE") {
      return "Vote pending";
    }
    if (election.status === "UPCOMING") {
      return "Voting opens later";
    }
    return "Election closed";
  }

  async function submitAction(action) {
    setError("");
    setMessage("");
    setLoading(true);
    try {
      await action();
    } catch (actionError) {
      setError(actionError.message);
    } finally {
      setLoading(false);
    }
  }

  if (!auth) {
    return (
      <div className="shell auth-shell">
        <div className="auth-card">
          <div className="auth-intro">
            <p className="eyebrow">Secure digital elections for real organizations</p>
            <h1>Online Voting System</h1>
            <p className="lead">
              A multi-organization platform for secure vote casting, results, and audit visibility.
            </p>
          </div>
          <div className="auth-toggle">
            <button className={mode === "login" ? "active" : ""} onClick={() => setMode("login")}>Login</button>
            <button className={mode === "register" ? "active" : ""} onClick={() => setMode("register")}>Register</button>
          </div>

          {mode === "login" ? (
            <form className="stack" onSubmit={(event) => handleAuthSubmit(event, "login")}>
              <input
                placeholder="Email"
                type="email"
                value={loginForm.email}
                onChange={(event) => setLoginForm({ ...loginForm, email: event.target.value })}
              />
              <input
                placeholder="Password"
                type="password"
                value={loginForm.password}
                onChange={(event) => setLoginForm({ ...loginForm, password: event.target.value })}
              />
              <button disabled={loading} type="submit">Sign In</button>
            </form>
          ) : (
            <form className="stack" onSubmit={(event) => handleAuthSubmit(event, "register")}>
              <input
                placeholder="Full name"
                value={registerForm.fullName}
                onChange={(event) => setRegisterForm({ ...registerForm, fullName: event.target.value })}
              />
              <input
                placeholder="Email"
                type="email"
                value={registerForm.email}
                onChange={(event) => setRegisterForm({ ...registerForm, email: event.target.value })}
              />
              <input
                placeholder="Password"
                type="password"
                value={registerForm.password}
                onChange={(event) => setRegisterForm({ ...registerForm, password: event.target.value })}
              />
              <button disabled={loading} type="submit">Create Account</button>
            </form>
          )}

          {error ? <p className="banner error">{error}</p> : null}
          {message ? <p className="banner success">{message}</p> : null}
        </div>
      </div>
    );
  }

  return (
    <div className="shell app-shell">
      <aside className="sidebar">
        <p className="eyebrow">Signed in</p>
        <h2>{auth.fullName}</h2>
        <p>{auth.email}</p>
        <div className="chip-row">
          {roles.map((role) => (
            <span key={role} className="chip">{role.replaceAll("_", " ")}</span>
          ))}
        </div>
        <button className="ghost-button" onClick={logout}>Logout</button>

        <SectionCard title="Organizations" subtitle="Switch context">
          <div className="stack compact">
            {dashboard.organizations.length ? dashboard.organizations.map((organization) => (
              <button
                key={organization.id}
                className={selectedOrganizationId === organization.id ? "list-button active" : "list-button"}
                onClick={() => setSelectedOrganizationId(organization.id)}
              >
                <strong>{organization.name}</strong>
                <span>{organization.type || "Organization"}</span>
              </button>
            )) : (
              <p className="muted">No organizations are available for your account yet.</p>
            )}
          </div>
        </SectionCard>

        {isPlatformAdmin ? (
          <SectionCard title="Create organization" subtitle="Platform admin only">
            <form className="stack compact" onSubmit={handleCreateOrganization}>
              <input
                placeholder="Organization name"
                value={organizationForm.name}
                onChange={(event) => setOrganizationForm({ ...organizationForm, name: event.target.value })}
              />
              <input
                placeholder="Type"
                value={organizationForm.type}
                onChange={(event) => setOrganizationForm({ ...organizationForm, type: event.target.value })}
              />
              <button disabled={loading} type="submit">Create</button>
            </form>
          </SectionCard>
        ) : null}
      </aside>

      <main className="content">
        <header className="page-header">
          <div>
            <p className="eyebrow">Overview</p>
            <h1>{currentOrganization?.name || "Dashboard"}</h1>
          </div>
          <div className="stats-grid">
            <StatCard label="Organizations" value={dashboard.organizations.length} />
            <StatCard label="Elections" value={dashboard.elections.length} />
            <StatCard label="Candidates" value={dashboard.candidates.length} />
            <StatCard label="Audit entries" value={dashboard.auditLogs.length} />
          </div>
        </header>

        {error ? <p className="banner error">{error}</p> : null}
        {message ? <p className="banner success">{message}</p> : null}

        <div className="tab-strip" role="tablist" aria-label="Feature tabs">
          <button className={activeTab === "overview" ? "tab-button active" : "tab-button"} onClick={() => setActiveTab("overview")} type="button">Overview</button>
          <button className={activeTab === "organizations" ? "tab-button active" : "tab-button"} onClick={() => setActiveTab("organizations")} type="button">Organizations</button>
          <button className={activeTab === "elections" ? "tab-button active" : "tab-button"} onClick={() => setActiveTab("elections")} type="button">Elections</button>
          <button className={activeTab === "candidates" ? "tab-button active" : "tab-button"} onClick={() => setActiveTab("candidates")} type="button">Candidates</button>
          {canManage ? (
            <button className={activeTab === "memberships" ? "tab-button active" : "tab-button"} onClick={() => setActiveTab("memberships")} type="button">Memberships</button>
          ) : null}
          <button className={activeTab === "results" ? "tab-button active" : "tab-button"} onClick={() => setActiveTab("results")} type="button">Results & audit</button>
          {isSelectedElectionClosed ? (
            <button className={activeTab === "winner" ? "tab-button active" : "tab-button"} onClick={() => setActiveTab("winner")} type="button">Winner</button>
          ) : null}
        </div>

        {activeTab === "overview" ? (
          <div className="grid two-column">
            <SectionCard title="Current elections" subtitle="Quick status of election windows">
              <div className="table-list">
                {dashboard.elections.length ? dashboard.elections.map((election) => (
                  <div className="table-row" key={election.id}>
                    <div>
                      <strong>{election.title}</strong>
                      <span>{election.status}</span>
                    </div>
                    <small>{getElectionParticipationLabel(election)}</small>
                  </div>
                )) : (
                  <div className="empty-state">
                    <strong>No elections yet</strong>
                    <p>{canManage ? "Create one from the Elections tab." : "An admin has not published an election for this organization yet."}</p>
                  </div>
                )}
              </div>
            </SectionCard>

            <SectionCard title="Recent activity" subtitle="Latest visible system updates">
              <div className="table-list">
                {dashboard.auditLogs.length ? dashboard.auditLogs.slice(0, 6).map((entry) => (
                  <div className="table-row" key={entry.id}>
                    <div>
                      <strong>{entry.action}</strong>
                      <span>{entry.actorEmail || "system"}</span>
                    </div>
                    <small>{new Date(entry.timestamp).toLocaleString()}</small>
                  </div>
                )) : (
                  <div className="empty-state">
                    <strong>No recent activity</strong>
                    <p>Election and membership actions will appear here.</p>
                  </div>
                )}
              </div>
            </SectionCard>
          </div>
        ) : null}

        {activeTab === "organizations" ? (
          <div className="grid">
            <SectionCard title="Organizations" subtitle="Switch context and manage organization setup">
              <div className="table-list">
                {dashboard.organizations.length ? dashboard.organizations.map((organization) => (
                  <button
                    key={organization.id}
                    className={selectedOrganizationId === organization.id ? "list-button active" : "list-button"}
                    onClick={() => setSelectedOrganizationId(organization.id)}
                  >
                    <strong>{organization.name}</strong>
                    <span>{organization.type || "Organization"}</span>
                  </button>
                )) : (
                  <div className="empty-state">
                    <strong>No organizations available</strong>
                    <p>Ask a platform admin to create one or add your account as an active member.</p>
                  </div>
                )}
              </div>
            </SectionCard>

            {isPlatformAdmin ? (
              <SectionCard title="Create organization" subtitle="Platform admin only">
                <form className="stack compact" onSubmit={handleCreateOrganization}>
                  <input
                    placeholder="Organization name"
                    required
                    value={organizationForm.name}
                    onChange={(event) => setOrganizationForm({ ...organizationForm, name: event.target.value })}
                  />
                  <input
                    placeholder="Type"
                    value={organizationForm.type}
                    onChange={(event) => setOrganizationForm({ ...organizationForm, type: event.target.value })}
                  />
                  <button disabled={loading} type="submit">Create</button>
                </form>
              </SectionCard>
            ) : (
              <SectionCard title="Organization setup" subtitle="Platform admin only">
                <div className="empty-state">
                  <strong>Admin access required</strong>
                  <p>Only platform admins can create organizations.</p>
                </div>
              </SectionCard>
            )}
          </div>
        ) : null}

        {activeTab === "elections" ? (
          <div className="grid two-column">
            <SectionCard title="Election list" subtitle="View and manage election windows">
              <div className="stack compact">
                {dashboard.elections.length ? dashboard.elections.map((election) => (
                  <button
                    key={election.id}
                    className={selectedElectionId === election.id ? "list-button active" : "list-button"}
                    onClick={() => setSelectedElectionId(election.id)}
                  >
                    <strong>{election.title}</strong>
                    <span>{election.status}</span>
                    <small>{getElectionParticipationLabel(election)}</small>
                  </button>
                )) : (
                  <div className="empty-state">
                    <strong>No elections yet</strong>
                    <p>{canManage ? "Create an election with the form beside this list." : "An admin has not published an election for this organization yet."}</p>
                  </div>
                )}
              </div>
            </SectionCard>

            {canManage && selectedOrganizationId ? (
              <SectionCard title="Create election" subtitle="Add a new election to this organization">
                <form className="stack compact" onSubmit={handleCreateElection}>
                  <input
                    placeholder="Election title"
                    required
                    value={electionForm.title}
                    onChange={(event) => setElectionForm({ ...electionForm, title: event.target.value })}
                  />
                  <textarea
                    placeholder="Description"
                    value={electionForm.description}
                    onChange={(event) => setElectionForm({ ...electionForm, description: event.target.value })}
                  />
                  <input
                    type="datetime-local"
                    required
                    min={minimumElectionDateTime}
                    value={electionForm.startTime}
                    onChange={(event) => {
                      const startTime = event.target.value;
                      setElectionForm({
                        ...electionForm,
                        startTime,
                        endTime: electionForm.endTime && electionForm.endTime < startTime ? "" : electionForm.endTime
                      });
                    }}
                  />
                  <input
                    type="datetime-local"
                    required
                    min={minimumElectionEndTime}
                    value={electionForm.endTime}
                    onChange={(event) => setElectionForm({ ...electionForm, endTime: event.target.value })}
                  />
                  <select
                    value={electionForm.resultVisibility}
                    onChange={(event) => setElectionForm({ ...electionForm, resultVisibility: event.target.value })}
                  >
                    <option value="AFTER_CLOSURE">Visible after closure</option>
                    <option value="ALWAYS">Always visible</option>
                  </select>
                  <button disabled={loading} type="submit">Create election</button>
                </form>
              </SectionCard>
            ) : (
              <SectionCard title="Election actions" subtitle="Management tools">
                <div className="empty-state">
                  <strong>{selectedOrganizationId ? "Admin access required" : "Select an organization"}</strong>
                  <p>{selectedOrganizationId ? "Only organization admins can create elections." : "Choose an organization before creating elections."}</p>
                </div>
              </SectionCard>
            )}
          </div>
        ) : null}

        {activeTab === "candidates" ? (
          <div className="grid two-column">
            <SectionCard
              title={selectedElection ? `Candidates for ${selectedElection.title}` : "Candidates and voting"}
              subtitle={selectedElection ? `${selectedElection.status} election` : "Select an election first"}
              action={
                canEditSelectedElection ? (
                  <button className="ghost-button" disabled={loading} onClick={() => handleCloseElection(selectedElectionId)}>
                    Close election
                  </button>
                ) : null
              }
            >
              <div className="candidate-grid">
                {dashboard.candidates.length ? dashboard.candidates.map((candidate) => (
                  <article className="candidate-card" key={candidate.id}>
                    <h4>{candidate.candidateName}</h4>
                    <p>{candidate.profileText || "No profile provided."}</p>
                    <button
                      disabled={loading || !selectedElection || selectedElection.hasVoted || selectedElection.status !== "ACTIVE"}
                      onClick={() => handleVote(candidate.id)}
                    >
                      {getVoteButtonLabel()}
                    </button>
                  </article>
                )) : <p className="muted">No candidates added for this election yet.</p>}
              </div>
            </SectionCard>

            {canEditSelectedElection ? (
              <SectionCard title="Add candidate" subtitle="Attach a candidate to the selected election">
                <form className="stack compact" onSubmit={handleAddCandidate}>
                  <input
                    placeholder="Candidate name"
                    required
                    value={candidateForm.candidateName}
                    onChange={(event) => setCandidateForm({ ...candidateForm, candidateName: event.target.value })}
                  />
                  <textarea
                    placeholder="Profile text"
                    value={candidateForm.profileText}
                    onChange={(event) => setCandidateForm({ ...candidateForm, profileText: event.target.value })}
                  />
                  <button disabled={loading} type="submit">Add candidate</button>
                </form>
              </SectionCard>
            ) : (
              <SectionCard title="Candidate management" subtitle="Admin controls">
                <div className="empty-state">
                  <strong>{!selectedElection ? "Select an election" : selectedElection.status === "CLOSED" ? "Election is closed" : "Admin access required"}</strong>
                  <p>{!selectedElection ? "Choose an election before managing candidates." : selectedElection.status === "CLOSED" ? "Closed elections cannot be changed." : "Only organization admins can add candidates."}</p>
                </div>
              </SectionCard>
            )}
          </div>
        ) : null}

        {activeTab === "memberships" && canManage ? (
          <div className="grid two-column">
            <SectionCard title="Memberships" subtitle="Manual onboarding and approvals">
              <div className="table-list">
                {dashboard.members.length ? dashboard.members.map((member) => (
                  <div className="table-row" key={member.membershipId}>
                    <div>
                      <strong>{member.fullName}</strong>
                      <span>{member.email}</span>
                    </div>
                    <span className="chip">{member.status}</span>
                  </div>
                )) : (
                  <div className="empty-state">
                    <strong>No members yet</strong>
                    <p>Add a member to let them access this organization and vote.</p>
                  </div>
                )}
              </div>
            </SectionCard>

            <SectionCard title="Add member" subtitle="Create and activate a new member">
              <form className="stack compact" onSubmit={handleAddMember}>
                <input
                  placeholder="Full name"
                  required
                  value={memberForm.fullName}
                  onChange={(event) => setMemberForm({ ...memberForm, fullName: event.target.value })}
                />
                <input
                  placeholder="Email"
                  type="email"
                  required
                  value={memberForm.email}
                  onChange={(event) => setMemberForm({ ...memberForm, email: event.target.value })}
                />
                <input
                  placeholder="Temporary password"
                  type="password"
                  value={memberForm.password}
                  onChange={(event) => setMemberForm({ ...memberForm, password: event.target.value })}
                />
                <select
                  value={memberForm.status}
                  onChange={(event) => setMemberForm({ ...memberForm, status: event.target.value })}
                >
                  <option value="ACTIVE">Active</option>
                  <option value="PENDING">Pending</option>
                  <option value="INACTIVE">Inactive</option>
                </select>
                <button disabled={loading} type="submit">Save member</button>
              </form>
            </SectionCard>
          </div>
        ) : null}

        {activeTab === "results" ? (
          <div className="grid two-column">
            <SectionCard
              title={selectedElection ? `Results for ${selectedElection.title}` : "Election results"}
              subtitle={selectedElection ? `${selectedElection.status} election` : "Select an election first"}
            >
              {selectedResults ? (
                <div className="table-list">
                  {selectedResults.results.length ? selectedResults.results.map((result) => (
                    <div className="table-row" key={result.candidateId}>
                      <div>
                        <strong>{result.candidateName}</strong>
                        <span>Candidate</span>
                      </div>
                      <strong>{result.voteCount} votes</strong>
                    </div>
                  )) : <p className="muted">No candidates are available for this election.</p>}
                </div>
              ) : (
                <p className="muted">Results are not visible yet for this election.</p>
              )}
            </SectionCard>

            <SectionCard title="Audit trail" subtitle="Transparency after each action">
              {canManage && dashboard.auditLogs.length ? (
                <div className="table-list">
                  {dashboard.auditLogs.slice(0, 8).map((entry) => (
                    <div className="table-row" key={entry.id}>
                      <div>
                        <strong>{entry.action}</strong>
                        <span>{entry.actorEmail || "system"}</span>
                      </div>
                      <small>{new Date(entry.timestamp).toLocaleString()}</small>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="empty-state">
                  <strong>{canManage ? "No audit entries yet" : "Admin access required"}</strong>
                  <p>{canManage ? "Election and membership actions will appear here." : "Only admins can view audit entries."}</p>
                </div>
              )}
            </SectionCard>
          </div>
        ) : null}

        {activeTab === "winner" && isSelectedElectionClosed ? (
          <div className="grid two-column">
            <SectionCard title="Winner" subtitle="Final result for the closed election">
              {selectedResults ? (
                <div className="winner-summary">
                  <p className="eyebrow">{winners.length > 1 ? "Tie result" : "Winner"}</p>
                  {winners.length ? (
                    <>
                      <h2>{winners.length > 1 ? `Tie between ${winners.map((winner) => winner.candidateName).join(", ")}` : winners[0].candidateName}</h2>
                      <p>{winners.length > 1 ? `Each candidate has ${highestVoteCount} votes` : `${highestVoteCount} votes`} out of {totalVotes} total votes.</p>
                    </>
                  ) : (
                    <>
                      <h2>No winner</h2>
                      <p>No votes were cast in this election.</p>
                    </>
                  )}
                </div>
              ) : (
                <p className="muted">Winner details are not available yet for this election.</p>
              )}
            </SectionCard>

            <SectionCard title="Election details" subtitle="Closed election summary">
              {selectedElection ? (
                <div className="table-list">
                  <div className="table-row">
                    <div>
                      <strong>{selectedElection.title}</strong>
                      <span>{selectedElection.organizationName}</span>
                    </div>
                    <span className="chip">{selectedElection.status}</span>
                  </div>
                  <div className="table-row">
                    <div>
                      <strong>Started</strong>
                      <span>{formatDateTime(selectedElection.startTime)}</span>
                    </div>
                  </div>
                  <div className="table-row">
                    <div>
                      <strong>Ended</strong>
                      <span>{formatDateTime(selectedElection.endTime)}</span>
                    </div>
                  </div>
                  <div className="table-row">
                    <div>
                      <strong>Total votes</strong>
                      <span>{totalVotes}</span>
                    </div>
                    <span>{resultItems.length} candidates</span>
                  </div>
                  {selectedElection.description ? <p className="muted">{selectedElection.description}</p> : null}
                </div>
              ) : (
                <p className="muted">Select a closed election to view winner details.</p>
              )}
            </SectionCard>
          </div>
        ) : null}
      </main>
    </div>
  );
}

export default App;
