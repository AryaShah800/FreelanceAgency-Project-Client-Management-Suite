# Freelance Agency Suite — Client Management & Ops Platform

A full-stack agency management suite: CRM pipeline, project/task tracking with
live comments, GST-compliant invoicing with a Razorpay checkout, AI-drafted
proposals, and a token-based public client portal for reviewing, e-signing,
and auto-converting proposals into active projects.

Built as a final-year project, deliberately on a Spring Boot backend (rather
than another Node/Express app) to demonstrate a second stack.

## Stack

**Backend** — Java 17, Spring Boot 3, Spring Security (JWT, stateless), Spring
Data JPA, PostgreSQL (H2 in-memory for local dev), STOMP over WebSocket
(SockJS) for live comments, OpenPDF for invoice/contract PDF generation.

**Frontend** — React 18 + Vite, Tailwind CSS, shadcn/ui, `@stomp/stompjs` for
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
  can review a proposal, digitally sign it (producing a SHA-256 signature
  hash and a locked, downloadable contract PDF snapshot), and one-click
  convert the signed proposal into an active project with an auto-generated
  50% deposit invoice.
- **Audit trail** — every sensitive action (proposal viewed/signed, project
  converted, etc.) is logged per-agency.

## Getting started

### Prerequisites

- Java 17+, Maven
- Node 18+
- (Optional) A Google AI Studio key, Razorpay test keys — the app runs fine
  without either; those features just report "not configured" until you add
  them.

### Backend

```bash
cd backend
cp .env.example .env
# Open .env and set JWT_SECRET at minimum, e.g.:
#   openssl rand -hex 32
mvn spring-boot:run
```

The backend runs on `http://localhost:8080` with an in-memory H2 database by
default (`SPRING_PROFILES_ACTIVE=dev`). On first run it seeds a demo agency,
users, clients, projects, and a signable demo proposal.

**Demo login:** `owner@agency.com` / `password123` (see
`DataInitializer.java` for the full seeded cast, including client-side
logins).

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

See `backend/.env.example` for the full list. Nothing is defaulted to a real
secret anymore — `JWT_SECRET` is required for the app to start at all; the
rest are optional and simply disable the corresponding feature until set.

| Variable | Required? | Purpose |
|---|---|---|
| `JWT_SECRET` | **Yes** | Signs/validates auth tokens. Generate with `openssl rand -hex 32`. |
| `GEMINI_API_KEY` | No | Enables AI proposal drafting. |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | No | Enables the Razorpay checkout flow. |
| `RAZORPAY_WEBHOOK_SECRET` | No | Enables the `payment.captured` webhook — set this once you add the webhook URL in the Razorpay Dashboard. |
| `CORS_ALLOWED_ORIGINS` | No (defaults to `http://localhost:5173`) | Comma-separated list of origins allowed to call the API and open a WebSocket. |

### Testing the Razorpay webhook locally

Add `http://localhost:8080/api/v1/public/payments/webhook` as a webhook URL
in the Razorpay Dashboard (test mode), subscribed to `payment.captured`, and
copy the signing secret it gives you into `RAZORPAY_WEBHOOK_SECRET`. For
local development without a public URL, use the Razorpay CLI or a tunnel
(e.g. `ngrok http 8080`) to forward events to your machine.

## Running tests

```bash
cd backend
mvn test
```

Covers the shared HMAC signature verification (used by both the client-side
payment confirmation and the webhook) and the proposal sign/convert flow.

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
- **E-signature**: proposals are persisted (`Proposal` entity), not held
  in-memory — signing computes a SHA-256 hash over the proposal content,
  signer name, and timestamp, and generates a locked PDF snapshot at that
  moment, so what's downloadable later can never drift from what was agreed.

## Known limitations

This is a portfolio/learning project, not a production system. Notably:
- The public portal's e-signature flow is a real, persisted, hashed
  signature — but it is **not** a legally-binding e-signature product (no
  identity verification, no tamper-evident signed PDF format like PAdES).
- Single-database, single-region; no queueing/retry layer for the Gemini or
  Razorpay API calls beyond what the HTTP client itself does.
