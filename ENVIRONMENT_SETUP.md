# Environment setup: keys and where to get them

Use the safe templates below. They contain variable names and instructions, not
working credentials:

- Backend staging/prod template: `backend/.env.staging.example`
- Frontend build template: `frontend/.env.example`

For local development, copy `backend/.env.example` to `backend/.env` and set a
private `JWT_SECRET`, then start with `run_app.bat`. The launcher reads the file
and forces H2, console email, and disabled external AI/payment calls. For
staging/production, add each backend variable to the hosting provider's
secret/environment settings instead of uploading the file. Copy the frontend
template to `frontend/.env` only if you need to override the default API/
WebSocket URLs; Vite embeds `VITE_*` values into public assets.

## Required for a staging launch

| Variable | What to enter | Where it comes from |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Fixed app setting; enables PostgreSQL, Flyway, and schema validation. |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://...?...` | Neon Console → project → **Connect**. Use the PostgreSQL JDBC prefix and `sslmode=require`. |
| `SPRING_DATASOURCE_USERNAME` | Neon database role | Neon Console → **Connect** details. |
| `SPRING_DATASOURCE_PASSWORD` | Neon database password | Neon Console → **Connect** details / role password. |
| `JWT_SECRET` | Unique random secret, at least 32 characters | Generate locally with the PowerShell command in the backend template. |
| `OWNER_BOOTSTRAP_SECRET` | Different unique random secret | Generate locally; needed only for first Owner setup, then remove/rotate it. |
| `APP_FRONTEND_BASE_URL` | Full deployed frontend origin | Your frontend hosting dashboard, e.g. `https://app.yourdomain.com`. Used in email links. Production requires HTTPS and an origin only, with no app path. |
| `CORS_ALLOWED_ORIGINS` | Exact frontend origin(s), comma-separated | Your frontend hosting dashboard. Production requires HTTPS origins only; do not use `*` or include paths. |
| `RESEND_API_KEY` | Resend API key | [Resend API Keys](https://resend.com/api-keys). Create a key for this environment. |
| `RESEND_FROM` | A sender address on your verified domain | Verify a domain in [Resend Domains](https://resend.com/domains), add the DNS records Resend shows, then use a sender such as `Agency App <no-reply@yourdomain.com>`. |
| `APP_EMAIL_DELIVERY_MODE` | `resend` | Fixed app setting for staging/production. |

## Optional integrations

| Variable(s) | Needed for | Where to get them |
|---|---|---|
| `GEMINI_API_KEY` | AI proposal drafting | [Google AI Studio API keys](https://aistudio.google.com/apikey). Leave blank to disable AI drafting. |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` | Online checkout with reliable payment confirmation | Create API keys in Razorpay Dashboard → **Account & Settings** → **API Keys**. Add a webhook for `POST /api/v1/public/payments/webhook`, enable `payment.captured`, and use its separate signing secret for `RAZORPAY_WEBHOOK_SECRET`. Checkout stays disabled until all three are set. Use test keys in staging. |
| `JWT_EXPIRATION_MS` | JWT session lifetime | Choose a duration in milliseconds; default is `86400000` (24 hours). |
| `SERVER_PORT` | Backend listen port | Usually set by the hosting provider; default is `8080`. |
| `VITE_API_BASE_URL` | Frontend API location when cross-origin | Your backend URL plus `/api/v1`; leave blank when a reverse proxy serves the API at the same origin. |
| `VITE_WS_URL` | Live task/comment updates | Your backend URL plus `/ws`, using `https://` for deployed sites. |

Do not put any backend secret in `frontend/.env`; anything beginning with
`VITE_` is available to users in the browser. Keep actual `.env` files out of
Git, and use separate keys and database credentials for staging and production.
The production profile checks its required JWT, Resend, frontend URL, and CORS
settings before database migrations run. Fill in real deployed URLs before
starting staging or production.
