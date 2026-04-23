# Online Voting System

This workspace now contains a full-stack implementation based on the provided plan and the local SRS:

- `backend/`: Spring Boot REST API with JWT auth, H2 database, seeded demo data, organization scoping, elections, candidates, voting, results, and audit logs.
- `frontend/`: React + Vite client for login, registration, organization switching, admin management, voting, and result viewing.

## Demo Accounts

- `admin@ovs.local` / `Password123!`
- `orgadmin@ovs.local` / `Password123!`
- `voter@ovs.local` / `Password123!`

## Backend Features

- Self-registration and login with JWT token generation
- Role-based access for platform admin, organization admin, and voter
- Multi-organization support with membership status enforcement for voters
- Platform admins can create organizations and manage their elections without separate membership activation
- Election create, update, close, and list flows
- Election validation blocks past start dates and invalid date ranges
- Candidate management per election
- Closed elections cannot be changed or closed again
- One-vote-per-user-per-election enforcement
- Result visibility rules and audit logging
- In-memory H2 setup with seeded sample data

## Frontend Features

- Login and self-registration screens
- Dashboard with role chips and organization context switching
- Election list and candidate voting flow
- Admin forms for organizations, members, elections, and candidates
- Frontend date/time validation for election creation
- Disabled voting controls when an election is upcoming, closed, or already voted
- Close-election confirmation before finalizing results
- Winner tab for closed elections, including tie and no-vote handling
- Results and organization audit activity panels
- Helpful empty states and admin-only guidance across organization, election, candidate, membership, result, and audit views


## Run Locally

### Backend

The backend is configured as a Maven Spring Boot app:

```powershell
cd backend
mvn spring-boot:run
```

If Maven is not installed on your machine yet, install Maven first or add a Maven wrapper before running.

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

The React app expects the backend at `http://localhost:8082` by default.
If you need a different backend URL, set `VITE_API_BASE_URL` before starting Vite.

## Notes

- The implementation plan `.docx` was locked by another process during this session, so the build was aligned against the accessible requirements already present in `SRS_Online_Voting_System.md`.
- The current setup uses H2 for local development. For production or final submission, switch the datasource to MySQL or PostgreSQL and externalize the JWT secret.
