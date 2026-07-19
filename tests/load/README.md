# Performance / load tests

Load tests for the backend API, written for [k6](https://k6.io). They exercise the
core booking journey against a running instance of the stack:

```
GET /api/v1/public/landing
POST /api/v1/auth/register
GET /api/v1/slots
POST /api/v1/bookings
GET /api/v1/bookings/mine
```

Each virtual user (VU) registers its own account per iteration (registration isn't
rate-limited, unlike `/api/v1/auth/login` — see `RateLimitingFilter`), so the script
avoids tripping the login rate limiter while still exercising real JWT auth.

## Prerequisites

- A running backend, e.g. via `docker-compose up` at the repo root (backend on
  `http://localhost:8080`) or `./mvnw spring-boot:run` from `car-wash-backend/`.
- [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/) installed locally, or
  run it via Docker:
  ```bash
  docker run --rm -i --network host \
    -e BASE_URL -e PROFILE -e VUS -e DURATION \
    grafana/k6 run - < tests/load/k6/booking-flow.js
  ```

## Running

```bash
# Quick smoke run (2 VUs, ~35s) — good for CI / sanity checks
k6 run tests/load/k6/booking-flow.js

# Heavier load profile, configurable via env vars
BASE_URL=http://localhost:8080 PROFILE=load VUS=30 DURATION=2m \
  k6 run tests/load/k6/booking-flow.js
```

Or via npm from the repo root:

```bash
npm run test:load             # smoke profile
npm run test:load:heavy       # load profile, 30 VUs / 2m
```

### Environment variables

| Variable   | Default                 | Description                                   |
|------------|--------------------------|------------------------------------------------|
| `BASE_URL` | `http://localhost:8080` | Backend base URL                                |
| `PROFILE`  | `smoke`                 | `smoke` (light, fast) or `load` (ramped, heavier) |
| `VUS`      | `20`                    | Peak virtual users (only used by `load` profile) |
| `DURATION` | `1m`                    | Steady-state hold duration (only `load` profile) |

## Thresholds

The script fails (non-zero exit) if:
- p95 request duration exceeds the per-endpoint budgets defined in `options.thresholds`
  (see `booking-flow.js`), or
- the custom `errors` rate (failed checks — unexpected status codes, missing tokens,
  etc.) exceeds 5%.

`409`/`422` responses from booking creation are treated as expected outcomes, not
errors — under concurrent load, multiple VUs racing for the same slot capacity is a
realistic contention scenario, not a bug.

## CI

`.github/workflows/load-test.yml` runs the `smoke` profile on demand
(`workflow_dispatch`) against a docker-compose stack spun up in the runner. It is not
wired into the `push`/`pull_request` triggers so it doesn't slow down normal CI.
