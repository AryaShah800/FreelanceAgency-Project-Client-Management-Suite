# Freelance Agency Suite — Client Management & Ops Platform

A full-stack agency management suite: CRM pipeline, project/task tracking with
live comments, GST-compliant invoicing with a Razorpay checkout, AI-drafted
proposals, and a token-based public client portal for reviewing proposals,
recording acceptance, and creating projects with milestone invoices.

Designed for freelance agencies to manage client relationships, delivery,
proposals, and billing in one workspace.

## Stack

**Backend** — Java 21, Spring Boot 4, Spring Security (JWT, stateless), Spring
Data JPA, PostgreSQL (H2 in-memory for local dev), STOMP over WebSocket
(SockJS) for live comments, OpenPDF for invoice and proposal-acceptance PDFs.

**Frontend** — React 18 + Vite 7, Tailwind CSS 4, `@stomp/stompjs` for
the live comment feed, Razorpay Checkout.js.

## Features

- **CRM pipeline** — clients tracked through deal stages (Lead → Contacted →
  Proposal Sent → Won/Lost).
- **Projects & tasks** — Kanban-style task board with live, persisted comments
  broadcast over WebSocket to everyone viewing the same project.
- **GST invoicing** — CGST/SGST/IGST-aware invoices, rendered as downloadable
  PDFs.
- **Online payments** — Razorpay Checkout for client-side payment, backed by a
  **server-to-server webhook** (`payment.captured`) as the source of truth, so
  a payment is reconciled even if the browser tab closes before the client
  gets a chance to confirm it.
- **AI proposal drafting** — Gemini-drafted proposal sections from a scope,
  budget, and timeline brief (agency-side, authenticated).
- **Public client portal** — a tokenized, unauthenticated link where a client
  can review a proposal, record acceptance with their typed name and email,
  and convert it into a project with invoices matching the agreed payment
  milestones.
- **Audit trail** — every sensitive action (proposal viewed/accepted, project
  converted, etc.) is logged per-agency.

## Getting started

### Prerequisites

- Java 21+, Maven
- Node.js 20.19+ or 22.12+
- (Optional) A Google AI Studio key, Razorpay test keys — the app runs fine
  without either; those features just report "not configured" until you add
  them.

### Backend

#### Windows PowerShell

```bash
cd backend
Copy-Item .env.example .env
# Set JWT_SECRET in backend/.env to a private value of at least 32 characters.
.\run-dev.ps1
```

The Windows launcher reads `backend/.env` and always uses disposable in-memory
H2, even if the file contains staging database settings.

#### macOS or Linux

```bash
cd backend
export JWT_SECRET="$(openssl rand -hex 32)"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend runs on `http://localhost:8080` with an in-memory H2 database
when run with the `dev` profile. On first run it seeds a demo agency,
users, clients, projects, and a signable demo proposal.

**Local development login:** `owner@agency.com` / `password123`. The dev profile
seeds sample accounts and must never be used with production data.

### Public recruiter demo without provider keys

For a recruiter walkthrough, deploy a separate backend with
`SPRING_PROFILES_ACTIVE=demo` and a separate frontend build with
`VITE_DEMO_ACCESS_ENABLED=true`. The demo backend uses disposable in-memory H2,
synthetic sample records, a local proposal template instead of Gemini, and no
Resend or Razorpay credentials. The login page exposes a public demo button, so
the demo account is intentionally shared. The workspace is writable and changes
can be seen by other visitors until the demo service restarts; never enter real
client data. Email flows and real online payments are unavailable in this mode.

Use these public demo credentials only on that isolated demo backend:
`demo@agency.test` / `DemoAccess2026!`.

For a hosted demo, create a **separate Render Web Service** from the repository
with root directory `backend`, Dockerfile path `backend/Dockerfile`, and set:

- `SPRING_PROFILES_ACTIVE=demo`
- Optionally set `JWT_SECRET` to a random private value of at least 32 characters;
  otherwise the demo backend generates an ephemeral one at startup
- `CORS_ALLOWED_ORIGINS` to the exact HTTPS origin of the separate demo frontend

Do not set `SPRING_DATASOURCE_URL` on this service; demo startup rejects anything
except H2. Do not attach the production Neon database. Then create a separate
Vercel project rooted at `frontend`, pointed at the demo backend. Set
`VITE_API_BASE_URL=https://<demo-backend-host>/api/v1`, leave `VITE_WS_URL` blank
so SockJS derives the demo backend host, and set `VITE_DEMO_ACCESS_ENABLED=true`
plus the demo site's required public metadata. Keep the existing business
frontend and backend deployment on the `prod` profile.

**H2 console:** `http://localhost:8080/h2-console` (JDBC URL
`jdbc:h2:mem:freelancedb`, user `sa`, no password).

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173`.

Or use `run_app.bat` (Windows) to launch both at once.

### Environment variables

See `backend/.env.example` for local development variables and
`DEPLOYMENT.md` for production requirements. Production requires PostgreSQL,
HTTPS frontend origins, a JWT signing secret, and a configured email sender for
public signup verification, invitations, and recovery. New agency owners can
register without a bootstrap or provider API key; provider credentials remain
private backend configuration. Payment and AI integrations are optional.

| Variable | Required? | Purpose |
|---|---|---|
| `JWT_SECRET` | **Yes** | Signs/validates auth tokens. Generate with `openssl rand -hex 32`. |
| `GEMINI_API_KEY` | No | Enables AI proposal drafting. |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | No | Enables the Razorpay checkout flow. |
| `RAZORPAY_WEBHOOK_SECRET` | Required when either Razorpay key is set | Separate webhook signing secret used to verify captured payments. |
| `CORS_ALLOWED_ORIGINS` | Yes in production | Exact frontend origins allowed to call the API and open a WebSocket. |

### Testing the Razorpay webhook locally

For staging, add your backend's public HTTPS URL ending in
`/api/v1/public/payments/webhook` in the Razorpay Dashboard (test mode), and
subscribe to `payment.captured`. Choose a strong webhook secret in Razorpay and
set the exact same value in `RAZORPAY_WEBHOOK_SECRET`; this is separate from
`RAZORPAY_KEY_SECRET`. For local development, forward port 8080 with the
Razorpay CLI or a public tunnel (e.g. `ngrok http 8080`) and use the tunnel's
HTTPS URL with the same webhook path. Razorpay cannot call `localhost` directly.

## Tests and release checks

```bash
cd backend
mvn test

cd ../frontend
npm ci
npm audit --audit-level=high
npm run build
```

The GitHub Actions workflow runs the backend test suite, checks frontend
dependencies for high severity advisories, and builds the production bundle on
pushes and pull requests to `main` or `master`.

## Architecture notes

- **Auth**: stateless JWT, validated per-request by `JwtAuthenticationFilter`
  and per-STOMP-frame by `JwtChannelInterceptor` (WebSocket subscriptions to
  a project's comment stream are authorized against the caller's agency).
- **Multi-tenancy**: enforced at the service layer — every query that returns
  agency-owned data is scoped by the authenticated user's `agencyId`.
- **Payments**: `PaymentService` verifies Razorpay's HMAC-SHA256 signature
  using a constant-time comparison (`HmacSignatureUtil`), shared between the
  client-triggered `/verify` endpoint and the `/webhook` endpoint, with
  idempotent payment recording so a retried webhook or a webhook + client
  confirmation racing each other can't double-record a payment.
- **Acceptance record**: proposals are persisted (`Proposal` entity). The
  acceptance hash includes the proposal content, signer name, contact email,
  and timestamp. A PDF record is generated at acceptance time.

## Launch scope and limitations

Review these product limitations before selling or onboarding an agency:
- The public portal records a typed name, contact email, timestamp, and a
  content-bound SHA-256 audit hash. It does not verify signer identity or
  provide a legally certified signature workflow. Do not market it as a
  legally binding e-signature service without jurisdiction-specific review.
- Single-database, single-region; no queueing/retry layer for the Gemini or
  Razorpay API calls beyond what the HTTP client itself does.
