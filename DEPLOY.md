# Deployment Guide — AI Car Wash

**Stack:** Spring Boot 3.4 backend on Render · React 19 frontend on Cloudflare Pages

---

## ⚠️ Before You Start — Rotate Secrets

The `.env` file previously contained real API keys. **Rotate them now:**

| Secret | Where to regenerate |
|---|---|
| `JWT_SECRET` | Run: `openssl rand -base64 48` |
| `GEMINI_API_KEY` | [Google AI Studio](https://aistudio.google.com/app/apikey) → delete old key, create new |
| `TOYYIBPAY_SECRET_KEY` | [dev.toyyibpay.com](https://dev.toyyibpay.com) → Account Settings → API Keys |

---

## Step 1 — Deploy Backend to Render

### Option A: Render Blueprint (Recommended — one click)

1. Push this repo to GitHub.
2. Go to [render.com](https://render.com) → **New** → **Blueprint**.
3. Connect your GitHub repo. Render reads `render.yaml` and creates:
   - Spring Boot web service (`car-wash-backend`)
   - PostgreSQL 15 database (`car-wash-db`)
   - Redis 7 instance (`car-wash-redis`)
4. During setup, Render will prompt you for the `sync: false` secrets. Enter:
   - `JWT_SECRET` — your new generated value
   - `GEMINI_API_KEY` — your new Gemini key
   - `TOYYIBPAY_SECRET_KEY` — your sandbox key
   - `TOYYIBPAY_CATEGORY_CODE` — your sandbox category code
   - Leave `TOYYIBPAY_RETURN_URL`, `TOYYIBPAY_CALLBACK_URL`, `CORS_ALLOWED_ORIGINS` blank for now (fill after Step 2).

5. Click **Apply**. Wait for deploy to go green.
6. Note your backend URL: `https://car-wash-backend-xxxx.onrender.com`

### Option B: Manual Render setup

1. New → **Web Service** → Docker → point to `./car-wash-backend`
2. New → **PostgreSQL** → choose Postgres 15
3. New → **Redis**
4. Add all env vars from `.env` to the web service's **Environment** tab.

---

## Step 2 — Deploy Frontend to Cloudflare Pages

1. Go to [Cloudflare Pages](https://pages.cloudflare.com) → **Create a project** → **Connect to Git**.
2. Select your repo. Set:
   - **Framework preset:** Vite
   - **Build command:** `npm run build`
   - **Build output directory:** `dist`
   - **Root directory:** `car-wash-frontend`
3. Under **Environment Variables (Production)**, add:
   ```
   VITE_API_BASE_URL = https://car-wash-backend-xxxx.onrender.com/api
   ```
   (Use your actual Render backend URL from Step 1.)
4. Click **Save and Deploy**. Note your Pages URL: `https://car-wash-frontend-xxxx.pages.dev`

---

## Step 3 — Wire Up the URLs

Now that you have both URLs, go back to **Render Dashboard** → `car-wash-backend` → **Environment** and fill in:

| Variable | Value |
|---|---|
| `TOYYIBPAY_RETURN_URL` | `https://car-wash-frontend-xxxx.pages.dev/checkout/return` |
| `TOYYIBPAY_CALLBACK_URL` | `https://car-wash-backend-xxxx.onrender.com/api/v1/payments/toyyibpay/callback` |
| `CORS_ALLOWED_ORIGINS` | `https://car-wash-frontend-xxxx.pages.dev` |

Click **Save Changes** — Render will redeploy automatically.

---

## Step 4 — Verify

- [ ] Open `https://car-wash-frontend-xxxx.pages.dev` — app loads
- [ ] Register a new account — no CORS errors in browser console
- [ ] Login — JWT is returned and stored
- [ ] Make a test booking — booking state transitions correctly
- [ ] Test payment flow — ToyyibPay sandbox checkout opens
- [ ] Check callback — booking status updates after sandbox payment
- [ ] Open Render logs → **Logs** tab — no stack traces, no SQL output

---

## Render Free Tier Notes

- The free web service **spins down after 15 minutes of inactivity**. First request after sleep takes ~30s. Acceptable for FYP demo; upgrade to Starter ($7/mo) to keep it awake.
- Free PostgreSQL is limited to 1 GB. Sufficient for FYP.
- Free Redis is available via Render's add-on.

---

## Local Docker Compose (still works)

For local testing with Docker, `.env` still drives everything:

```bash
cp .env .env          # fill in your local values
docker compose up --build
```

Frontend now serves on **http://localhost:80** (not 5173).

---

*Last updated: 2026-06-22*
