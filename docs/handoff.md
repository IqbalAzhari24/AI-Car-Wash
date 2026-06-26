# Handoff — AI Car Wash Backend

> Machine-readable status handoff. Updated 2026-06-27.
> Scope: `car-wash-backend` (Spring Boot 3.4.1 / Java 21 / Maven).
> Stack locked per `CLAUDE.md` — do NOT bump versions without instruction.

---

## 1. TL;DR

| Field | Value |
|---|---|
| **CI (GitHub Actions, Linux)** | ✅ **GREEN — 56 tests, 0 failures, 0 errors.** |
| Local non-Docker tests (Windows) | 35 / 35 pass. |
| Local integration tests (Windows) | 6 ERROR — Docker Desktop gateway defect (env, not code). |
| Schema head | Flyway `V10` (`slot_capacities.booked_count`) — committed. |
| Source of truth for "all green" | CI on `ubuntu-latest`, where Testcontainers works. |

Run all tests reliably via CI (push to `main`/`develop`). Locally on this
Windows box, only the 35 non-Docker tests can pass — see §3.

---

## 2. Test Status

### CI — all green (56)
`.github/workflows/test.yml` runs `mvn test` on `ubuntu-latest`. The 6
integration tests self-provision Postgres/Redis via Testcontainers (native
Docker on the runner). Latest run: **56 run, 0 failures, 0 errors.**
(56 > local 41 because the `@Test` methods inside the 6 IT classes actually
execute on Linux; on Windows those classes error at startup and count as 6.)

### Local Windows — 35/35 (+6 env-blocked)
The 6 IT extending `src/test/java/com/carwash/backend/AbstractIntegrationTest.java`
cannot run on this machine (Docker Desktop defect, §3):
`AuthControllerTest`, `BookingControllerTest`, `BookingServiceIntegrationTest`,
`CarWashBackendApplicationTests`, `OwnerAnalyticsControllerTest`,
`SlotCapacityRepositoryTest`.

Run the 35 that pass locally:
```bash
mvn test -Dtest='!AuthControllerTest,!BookingControllerTest,!BookingServiceIntegrationTest,!CarWashBackendApplicationTests,!OwnerAnalyticsControllerTest,!SlotCapacityRepositoryTest'
```

---

## 3. Local Docker Blocker (Windows only — NOT a code issue)

**Root cause (proven):** Docker Desktop's Windows pipe gateway returns a stub
`/info` (HTTP 400, label `com.docker.desktop.address=npipe://\\.\pipe\docker_cli`)
to the **docker-java** client used by Testcontainers, while the `docker` CLI +
`curl` get HTTP 200 real data on the *same* endpoints. The gateway discriminates
by client request shape; CLI passes, docker-java gets the stub.

**Confirmed NOT the cause:** Java version, Testcontainers/docker-java version
(latest 1.21.3 also fails), pipe choice (`docker_engine` /
`dockerDesktopLinuxEngine` / tcp 2375 all route through the same gateway →
same stub), `DOCKER_HOST`, `DOCKER_API_VERSION`, factory reset.

**Resolution:** Run integration tests on Linux (CI) — done, green. Local
Windows runs the 35 non-Docker tests. Optional local-IT paths if ever needed:
WSL2 with native docker + JDK21 + Maven, or Rancher Desktop.

---

## 4. Database Schema — Migrations

Flyway only (`db/migration/`). `ddl-auto=validate`. snake_case, UUID PKs.

| Ver | File | Purpose |
|---|---|---|
| V1 | `V1__init_schema.sql` | Base schema. |
| V2 | `V2__make_phone_optional.sql` | Phone nullable. |
| V3 | `V3__add_service_to_bookings.sql` | Booking↔service link. |
| V4 | `V4__create_password_reset_tokens.sql` | Reset tokens table. |
| V5 | `V5__fix_slot_concurrency.sql` | Slot concurrency hardening. |
| V6 | `V6__add_updated_at.sql` | `updated_at` audit cols. |
| V7 | `V7__add_soft_delete.sql` | `deleted_at` soft-delete cols. |
| V8 | `V8__seed_catalog.sql` | Catalog seed data. |
| V9 | `V9__fix_token_hash_type.sql` | Token hash column type fix. |
| **V10** | `V10__add_booked_count_to_slot_capacities.sql` | `booked_count INTEGER NOT NULL DEFAULT 0`. **Committed `fbb563c`.** |

### V10 — booked_count
```sql
ALTER TABLE slot_capacities
    ADD COLUMN IF NOT EXISTS booked_count INTEGER NOT NULL DEFAULT 0;
```
- Running booked counter on `slot_capacities`. Entity field
  `SlotCapacity.bookedCount` (default 0).
- Consumers: `BookingEngineService`, `BookingService`, `SlotReplenishmentCron`,
  `SlotCapacityRepository`, `SlotAvailabilityDto`, `CatalogSeeder`.
- Availability = `max_limit - booked_count`.
- **Was the CI blocker:** entity expected the column but the migration was
  never committed → `ddl-auto=validate` failed on a clean DB. Fixed by
  committing V10.

---

## 5. Fixes Landed This Session

| Commit | Fix | Why |
|---|---|---|
| `92d38f2` | CI workflow trimmed to Testcontainers-only; `.gitignore` guards `.env.production`, `db-backups/`; this handoff. | Make CI exercise the IT on Linux. |
| `fbb563c` | Commit **V10** migration. | Schema-validation mismatch (`missing column booked_count`). |
| `545430c` | **401 entry point** in `WebSecurityConfig` (`HttpStatusEntryPoint(UNAUTHORIZED)`). | Unauthenticated returned 403; tests/contract expect 401. Authn-but-forbidden still 403. |
| `545430c` | **`@Transactional`** on `SlotCapacityRepository.{increment,decrement}BookedCount`. | `@Modifying` queries threw `TransactionRequiredException` when called directly from the repo test. |

🚩 **Security note:** unauthenticated responses changed 403 → 401. If the
frontend keyed on 403-for-not-logged-in, update it.

---

## 6. Open Items

1. **`target/` cleanup uncommitted** — ~8400 staged deletions of tracked build
   artifacts (`car-wash-backend/target/**`) sit in the index, unrelated to the
   above surgical commits. Commit separately when ready (`target/` is already
   gitignored).
2. **`.env.production`** — now gitignored. If it was committed in any past
   commit, rotate those secrets.
3. **Service-layer coverage ≥70%** (CLAUDE.md target) — verify against the
   green CI run.
4. **Node 20 deprecation warning** in CI (actions/checkout, setup-java) —
   cosmetic; bump action majors when convenient.

---

## 7. Re-run

- **CI:** push to `main`/`develop`, or `gh run watch <id> --exit-status`.
- **Local (35 only):** see §2 command.

---

## 8. Constraints (from CLAUDE.md — do not break)

- Stack locked: Java 21, Spring Boot 3.4, no dep bumps without instruction.
- Integration tests use Testcontainers — do NOT mock the DB.
- Flyway only — no `ddl-auto=create`.
- Soft delete only; constructor injection only; DTOs not entities in responses.
- No hardcoded secrets.
