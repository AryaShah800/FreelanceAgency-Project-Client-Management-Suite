# Staging deployment and recovery

The original production target was **PostgreSQL 16 on Neon**, while the current
Neon staging database reports PostgreSQL 18.6. Confirm the production major
version and align staging, production, and backup client tools before release.
The production Spring profile enables Flyway and validates Hibernate mappings.
The first startup applies the full Flyway migration chain, currently V1 and V2;
do not manually create the schema or run Hibernate `ddl-auto=update`.
CI also applies the full migration chain and validates the Hibernate mappings
against an isolated PostgreSQL 16 service. The configured Neon staging database
currently reports PostgreSQL 18.6, so keep staging and production aligned before
the final populated-data restore drill.
The app requires exactly one active profile (`dev` locally, `demo` for an isolated
public recruiter sandbox, or `prod` for real agency data).
The CI-only `migration-test` profile permits plaintext connections only to its
loopback PostgreSQL service; normal production startup still requires PostgreSQL
TLS (`sslmode=require`, `verify-ca`, or `verify-full`).
Use `/livez` for the backend liveness probe and `/readyz` for the readiness
probe. Readiness reports unavailable when the database cannot be reached;
health response details and component names are hidden.

## Staging configuration

Set these values in the backend host's secret/environment settings. Do not commit
credentials or paste them into chat.

The frontend uses same-origin `/api/v1` and `/ws` paths when its optional
`VITE_API_BASE_URL` and `VITE_WS_URL` values are blank. In that setup, configure
the frontend host or reverse proxy to route those paths to the backend. If the
API is on a separate origin, supply its HTTPS URL during the frontend build and
add the exact frontend HTTPS origin to backend CORS. Production builds reject
localhost and plaintext HTTP/WS endpoint overrides.

| Setting | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<Neon-host>/<database>?sslmode=require` (`verify-ca` and `verify-full` are also accepted) |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Neon database role credentials |
| `JWT_SECRET` | Random secret with at least 32 characters |
| `OWNER_BOOTSTRAP_SECRET` | Random one-time secret for creating the first owner |
| `APP_FRONTEND_BASE_URL` | Deployed frontend origin, without a trailing slash |
| `RESEND_API_KEY` | Resend API key with permission to send mail |
| `RESEND_FROM` | Verified sender, e.g. `Agency App <no-reply@your-domain.example>` |
| `CORS_ALLOWED_ORIGINS` | Exact deployed frontend origin(s), comma-separated |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | Leave both blank to disable payments; configure both to enable checkout |
| `RAZORPAY_WEBHOOK_SECRET` | Separate signing secret; required whenever either Razorpay key is configured |

For the frontend production build, configure these build-time values in the
frontend host (they are public metadata, never secret credentials):

| Setting | Value |
|---|---|
| `VITE_PUBLIC_SITE_URL` | Real HTTPS frontend origin; used for canonical share-card URLs |
| `VITE_LEGAL_OPERATOR` / `VITE_LEGAL_JURISDICTION` | Real legal business identity and governing region |
| `VITE_SUPPORT_EMAIL` | Monitored mailbox shown to users |
| `VITE_PLAUSIBLE_DOMAIN` / `VITE_PLAUSIBLE_SCRIPT_URL` | Optional Plausible site; use the manual-pageview script variant |
| `VITE_SENTRY_LOADER_URL` | Optional public Sentry browser loader URL (not an auth token) |

## Isolated recruiter demo (no provider API keys)

Use a separate Render service and Vercel project for the public demo. Set the
backend profile to `demo`; `JWT_SECRET` is optional in this profile, and if blank
the service generates an ephemeral signing secret on startup. Set
`CORS_ALLOWED_ORIGINS` to the demo frontend's exact HTTPS origin. Do not
set a database URL: the demo profile requires an in-memory H2 database and refuses
PostgreSQL, including Neon. It seeds synthetic records and resets them when the
service restarts. The demo login is public and shared; its workspace is writable,
so visitors must not enter real customer data.

Set `VITE_DEMO_ACCESS_ENABLED=true` only in the separate demo frontend build. The
business frontend must leave it false. The demo uses the built-in local proposal
template without Gemini, keeps Resend email delivery disabled, and keeps Razorpay
checkout disabled. No third-party provider API keys are needed by recruiters.
If you provide `JWT_SECRET`, keep it private; it is never exposed to the browser.
When auto-generated, tokens are invalidated after each service restart. See the
README for the public demo login and host setup values.

`GEMINI_API_KEY` and the Razorpay key pair are optional integrations. AI drafting
uses the built-in template when Gemini is disabled. Payment endpoints return
`503 Service Unavailable` until the Razorpay key pair and webhook signing secret
are configured. Production startup fails if only one API key is present or the
webhook secret is missing/short. The webhook secret is separate from the Razorpay
API secret.

Configure the sending domain in Resend (including DNS authentication) before
opening public signup. Anyone can create an agency account without supplying
provider API keys; the backend sends a 24-hour email verification link and does
not allow sign-in until the address is verified. Public signup is limited to 10
attempts per client address per hour on each backend instance. Owners can still
invite Admin or Staff from Team Settings. Invite links expire in 7 days and
password reset links in 1 hour. These links are single-use.

`OWNER_BOOTSTRAP_SECRET` remains only for the optional installation bootstrap
endpoint and is not needed for ordinary public signup. Keep it unset unless you
are using that manual bootstrap flow. JWT signing, database access, and email
provider credentials remain backend deployment secrets; no agency user enters
or receives them.

In-app login, AI, and PDF throttles use bounded caches per backend process.
Before public launch, configure gateway rate limits for login/bootstrap, public
token endpoints, and costly AI/PDF routes. Per-process counters do not combine
across instances; use a shared limiter or gateway enforcement when running more
than one backend instance.

For local development, use `run_app.bat`. It starts the backend with the `dev`
profile and an in-memory H2 database, isolated from the Neon credentials in
`backend/.env`. The dev profile seeds sample data and must only use disposable
local data.

## Backup and restore drill

Install PostgreSQL client tools matching the chosen production major version
(`pg_dump`, `pg_restore`, `psql`). Set the source database's `PGHOST`, `PGPORT`,
`PGUSER`, `PGPASSWORD`, and `PGSSLMODE=require` in the local process environment
or a secret manager. The password stays out of shell history and native-client
arguments. Create a custom-format backup:

```powershell
$env:PGHOST = '<Neon-host>'
$env:PGPORT = '5432'
$env:PGUSER = '<database-role>'
$env:PGPASSWORD = '<load from a local secret manager>'
$env:PGSSLMODE = 'require'
pg_dump --format=custom --no-owner --no-privileges --dbname=neondb --file=agency-staging.dump
```

Create a **separate empty Neon branch/database** for the drill and name its
database with `scratch`, `restore`, `drill`, or `test`. Supply its URI through a
secure local environment variable, then run:

```powershell
$env:RESTORE_DRILL_DATABASE_URL = 'postgresql://<Neon-host>/<restore_database>?sslmode=require'
.\backend\scripts\verify-postgres-restore.ps1 -BackupFile .\agency-staging.dump -RestoreDatabaseUrl $env:RESTORE_DRILL_DATABASE_URL
```

Set `PGUSER` and `PGPASSWORD` to the restore target's role credentials. The
script refuses a target whose database name does not look isolated or which
already contains public tables. It strips any password from the URI before
calling `psql` or `pg_restore`, restores without dropping objects, and checks
that core application tables, the V2 proposal contact column, and current Flyway
history exist. A successful
script run verifies schema restoration; for a complete operational drill, also
start a staging app pointed at the restored branch and verify login, project,
proposal, and invoice records with an owner account. Delete the scratch branch
after the drill through Neon.

### Restore drill record

- Run date: 2026-10-08 UTC.
- Source: configured Neon staging database; PostgreSQL 18.x, 24 public tables,
  approximately 9 MB. The agency, user, project, proposal, and invoice tables
  were empty, so this run verified schema recovery rather than populated-record
  recovery.
- Target: temporary `agency_restore_drill_20261008_224614` database on the
  staging branch; removed after verification.
- Result: passed. The backup restored, the required core tables were present,
  and successful Flyway history was present. Backup plus restore took about
  one minute; the temporary backup file was removed afterward.
- Limitation: no application was started against the restored database, so
  login and readback of restored business records still need an operational
  drill with representative data.

- Follow-up run date: 2026-10-09 UTC.
- Source and target: disposable scratch databases on the same Neon staging
  branch; the configured staging database was not modified. The source used the
  production Flyway profile and contained one synthetic row each for an agency,
  owner, client, project, task, proposal, invoice, and invoice line item.
- Result: passed. `pg_dump` and the restore verification script completed, and
  direct SQL confirmed all eight synthetic rows in the restore target. Both
  scratch databases were removed and their absence was verified. The packaged
  production app started against both databases and completed Hibernate schema
  validation. The source app bootstrapped and email-verified a synthetic owner
  through local console delivery. That owner logged in before and after restore;
  authenticated API reads for the client, project, and invoice and a public
  proposal read all passed on the restored target. Both scratch databases were
  removed and their absence was verified. The configured staging business
  tables remained empty.
- Restore helper security retest: the updated helper accepted a password-bearing
  URI, restored the staging schema to a disposable target, and verified 24
  public tables plus successful Flyway history. It strips passwords before
  invoking native PostgreSQL tools, requires TLS, and the scratch target was
  removed after the check.

- Post-V2 run date: 2026-10-09 UTC.
- Source: configured Neon staging database, PostgreSQL 18.6, at Flyway V2. The
  core agency, user, client, project, task, proposal, and invoice tables were
  empty, so this run verifies post-V2 schema recovery only.
- Result: passed. A fresh custom-format backup restored to a uniquely named
  empty database on the staging branch. The verifier found the required tables,
  `proposals.client_email`, and Flyway V2. The disposable database and local
  backup file were removed; the configured staging database was not modified.
- Limitation: repeat this drill with representative synthetic business records
  after the application has data, then verify restored login and record access.

## Isolated load checks

- Run date: 2026-10-09 local time.
- Target: the packaged app with the disposable H2 `dev` profile and seeded demo
  data; no Neon database or external mail/payment provider was used.
- Workload: 10 concurrent workers made 500 authenticated, read-only requests to
  the projects API. All 500 returned HTTP 200; p50 latency was 32.8 ms, p95 was
  66.3 ms, and maximum observed latency was 728.6 ms on this development host.
- Limit: this is a regression/load smoke check, not a production capacity or
  multi-tenant scale test. Repeat against production-like infrastructure before
  promising an agency-level capacity target.
- Follow-up run date: 2026-10-09 local time, after the launch-check changes.
- Target: seeded local H2 dev profile; 10 workers made 500 authenticated,
  read-only requests to `GET /api/v1/projects`. Login succeeded and all 500
  requests returned HTTP 200; p50 was 18.2 ms, p95 was 55.7 ms, and maximum
  observed latency was 342.9 ms.
- Limit: same local regression/load smoke scope; not a production capacity
  commitment.
- Follow-up run date: 2026-10-09 local time, proposal read stress pass.
- Target: local test-profile H2 through MockMvc; 12 workers made 480 authenticated
  proposal-list reads. The latest clean backend verification returned HTTP 200
  for all requests; p95 was 31 ms.
- Limit: regression smoke check, not a production capacity benchmark.

## Launch-day rollback plan

Keep the last known-good frontend and backend artifacts available in the hosting
provider, and record each release commit, migration version, and environment
configuration revision. Do not put secret values in this record.

1. Trigger rollback for failed `/readyz`, failed sign-in or core agency reads,
   cross-agency access, incorrect payment state, or sustained error/latency
   alerts. Pause new writes or enable the host's maintenance mode if data may be
   getting corrupted.
2. Revert the frontend to the last known-good static build. Revert the backend
   only when that release is compatible with the database schema currently in
   place. Flyway migrations are forward-only here; do not manually drop columns
   or tables to make an older binary start.
3. If the new database schema or data is incompatible, keep the current database
   intact. Restore the latest verified backup to a **new isolated database**,
   validate the restored records, then switch the backend connection to that
   database. Never restore over production as the first recovery step.
4. If a third-party integration is the cause, disable only that feature: remove
   both Razorpay API keys and the webhook secret together to disable checkout,
   or clear `GEMINI_API_KEY` to use local proposal templates. Keep required mail
   settings intact; use the host's routing/maintenance controls if sign-in mail
   is unavailable.
5. Confirm `/livez`, `/readyz`, owner login, project/client/invoice reads, and
   the affected user journey using a test account. Reopen traffic only after
   those checks pass. Preserve logs and the failed release for diagnosis.
6. Tell affected users what happened, whether any data was lost or delayed, and
   when service resumed. Record the incident, actions, and follow-up fixes.

Practice this procedure against staging before the first real agency data is
loaded. Confirm your host supports release rollback, maintenance routing, and
changing the database connection without deleting the previous database.

## Launch checks

- Secrets scan of the current worktree and available Git history found no
  recognizable live Google/Resend/Razorpay/OpenAI-style API tokens. It did find
  a non-placeholder JWT signing secret repeated in early committed backend
  configuration and an environment template. Treat any deployment value copied
  from those commits as exposed and rotate it; changing it signs users out. The
  current local `backend/.env` does not match it. `.env` files are ignored; keep
  current secrets in the backend host's secret store. Removing the old value from
  all Git history requires a coordinated history rewrite and has not been done.
- API controllers require authentication by default, with explicit public
  exceptions for account lifecycle, health, and token-based client portal
  routes. Agency-scoped service/repository checks and automated IDOR tests are
  present. PostgreSQL row-level security policies are **not** enabled; the app
  relies on those server-side tenant checks.
- Backend authentication, AI generation, PDF creation, and public token routes
  have in-process rate limits. AI daily/output caps are configurable. These
  in-memory counters reset on restart and do not combine across backend
  instances; configure gateway limits and the Gemini provider's own budget/quota
  before exposing the service publicly.
- Production errors return generic messages. The backend logs unexpected
  exceptions. Optional Sentry browser capture is wired with URL/token scrubbing;
  its loader is not configured, and a backend Sentry SDK/project is still needed
  for server alerts.
- The populated synthetic-record PostgreSQL restore drill passed on 2026-10-09
  and verified restored login and business-record reads. The production-like
  PostgreSQL 16 GitHub Actions migration job still needs a green run.
- Frontend 404 and render-error pages, privacy/terms drafts, contact page,
  responsive layout primitives, metadata, favicon, and a 1200x630 PNG social
  card are in the app. Before production build set `VITE_PUBLIC_SITE_URL`,
  `VITE_LEGAL_OPERATOR`, `VITE_LEGAL_JURISDICTION`, and `VITE_SUPPORT_EMAIL`.
  Legal pages remain drafts until business details, local legal review, and
  actual retention/support practices are confirmed.
- Optional Plausible funnel events are wired without sending form fields or
  identifiers. Add a Plausible site and configure its manual-pageview script URL
  and domain in the frontend build settings; verify events in its dashboard.
  The manual-pageview script is required so proposal/share tokens never appear in
  analytics URLs. Without those values, analytics is off. Set up a Sentry browser
  project and configure its loader URL; also set up a backend project/SDK to
  receive server-side alerts.
- Resend delivery has not passed: the configured API key previously returned
  HTTP 401. Replace it, verify the sender domain, then test verification, reset,
  and invitation messages and monitor bounces/spam placement.
- Before accepting real data, complete real-device Android QA, test sign-up,
  payment, and password reset end-to-end with provider test credentials, verify
  mail delivery, and confirm support messages reach a monitored inbox. Current
  local load tests do not prove production latency or capacity.

- The contract/version/signer tables are currently persistence models without a
  contract API, lifecycle service, or management UI. Their hash and lock columns
  do not enforce immutability or prove signer identity. Use the proposal
  acceptance record only until contract scope and any signing provider are
  selected and implemented.
- Neon staging has applied and validated Flyway V1 -> V2. On 2026-10-09 the
  packaged `prod` app started against staging, confirmed there were no pending
  migrations, and passed Hibernate schema validation. That smoke check used
  temporary test-only frontend-origin and webhook values. Staging is PostgreSQL
  18.6; the PostgreSQL 16 CI migration job still needs to pass before release.
- A read-only Resend API check returned HTTP 401 for the configured key. Replace
  it with a valid key, verify the sender domain, then test verification, reset,
  and invitation delivery. No email was sent during this check.
- Automated local tests cover account lifecycle and role scoping. Before real
  client data, run Owner, Admin, Staff, and Client acceptance checks on the
  deployed environment.
- The post-V2 schema restore drill passed on 2026-10-09, but the staging business
  tables were empty. Repeat with representative synthetic business records and
  verify restored login plus client/project/proposal/invoice access.
- Set the deployed HTTPS frontend URL in `APP_FRONTEND_BASE_URL` and include it
  in `CORS_ALLOWED_ORIGINS`. The current local environment has neither value.
- Add a separate Razorpay webhook signing secret (at least 32 characters) in
  the deployment secret store, or remove both Razorpay API keys to keep payment
  checkout disabled. The current local environment has no webhook secret.
- Keep production backups and restore branches isolated; never run the restore
  drill against the production database.
