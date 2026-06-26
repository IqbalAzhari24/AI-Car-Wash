# Handoff — AI Car Wash Backend

> Machine-readable status handoff. Generated 2026-06-27.
> Scope: `car-wash-backend` (Spring Boot 3.4.1 / Java 21 / Maven).
> Stack locked per `CLAUDE.md` — do NOT bump versions without instruction.

---

## 1. TL;DR

| Field | Value |
|---|---|
| Build | Compiles. `mvn -DskipTests package` OK. |
| Unit/non-Docker tests | **35 / 35 pass** (0 failures). |
| Integration tests | **6 ERROR** — blocked by Docker env, not code. |
| Total suite | 41 tests, 0 failures, 6 errors. |
| Blocker | Testcontainers cannot reach this machine's Docker Desktop. |
| Schema head | Flyway `V10` (adds `slot_capacities.booked_count`). |
| pom.xml | Clean/original. No uncommitted dep changes. |

---

## 2. Test Status

### Passing (35)
All service-layer + non-`@SpringBootTest` unit tests (JUnit 5 + Mockito).
Run them isolated (skips the 6 Docker tests):

```bash
mvn test -Dtest='!AuthControllerTest,!BookingControllerTest,!BookingServiceIntegrationTest,!CarWashBackendApplicationTests,!OwnerAnalyticsControllerTest,!SlotCapacityRepositoryTest'
```

### Erroring (6) — Docker-dependent
All extend `src/test/java/com/carwash/backend/AbstractIntegrationTest.java`,
which spins `postgres:15-alpine` + `redis:7-alpine` via Testcontainers:

1. `AuthControllerTest`
2. `BookingControllerTest`
3. `BookingServiceIntegrationTest`
4. `CarWashBackendApplicationTests`
5. `OwnerAnalyticsControllerTest`
6. `SlotCapacityRepositoryTest`

Error (all 6, identical):
```
Could not find a valid Docker environment.
```

---

## 3. Docker Blocker — Root Cause (diagnosed)

**NOT** Java version. **NOT** Testcontainers/docker-java version.
It is **this Docker Desktop install** mis-routing the docker-java client.

### Evidence
- `docker` CLI + `curl` → HTTP **200** with real `/info` on every endpoint.
- docker-java (used by Testcontainers) → HTTP **400** stub `/info` (empty `Info`,
  label `com.docker.desktop.address=npipe://\\.\pipe\docker_cli`) on EVERY transport:
  npipe `docker_engine`, npipe `dockerDesktopLinuxEngine`, tcp 2375, tcp 2376 relay.
- CLI/curl escape via docker context `desktop-linux`; docker-java hits the
  internal `docker_cli` admin proxy → 400.
- Engine 29.5.3, API 1.54.

### Tried — ALL FAILED
`~/.testcontainers.properties` `docker.host` (npipe linux engine / tcp localhost / tcp 127.0.0.1);
`DOCKER_HOST` env; `DOCKER_API_VERSION=1.40`; surefire `argLine` system prop;
WSL python tcp→unixsocket relay; containerd image-store toggle (driver flipped
overlayfs→overlay2, tests still failed); Docker Desktop update (engine stayed 29.5.3);
Testcontainers bump to 1.21.3 / latest docker-java (still 400, **reverted** per stack lock).

### Cleanup done
- pom.xml reverted to clean state.
- `~/.testcontainers.properties` removed.
- WSL relay killed + deleted.

### Next candidate fix (UNTRIED)
Docker Desktop **factory reset** — see §6. Likely fixes the proxy mis-route.
After reset, re-run full suite (§7).

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
| **V10** | **`V10__add_booked_count_to_slot_capacities.sql`** | **NEW — staged. See below.** |

### V10 (current head, git-staged `A`)
```sql
ALTER TABLE slot_capacities
    ADD COLUMN IF NOT EXISTS booked_count INTEGER NOT NULL DEFAULT 0;
```
- Adds running booked counter to `slot_capacities` (was max_limit only).
- Entity: `entity/SlotCapacity.java` → field `bookedCount` (default 0).
- Consumers: `BookingEngineService`, `BookingService`, `SlotReplenishmentCron`,
  `SlotCapacityRepository`, `SlotAvailabilityDto`, `CatalogSeeder`.
- Enables availability = `max_limit - booked_count` instead of row-counting bookings.

---

## 5. Immediate Next Tasks

1. **Unblock Docker** (priority). Factory reset Docker Desktop (§6), then
   re-run full suite (§7). Target: 41/41 green.
2. **Verify V10 path under real Postgres.** The 6 IT classes (esp.
   `SlotCapacityRepositoryTest`, `BookingServiceIntegrationTest`) exercise
   `booked_count` concurrency — only meaningful once Docker tests run.
3. **Commit V10 + related code** once IT green. Currently V10 staged, code may
   be unstaged — check `git status` before commit.
4. **Confirm coverage ≥70% service layer** (CLAUDE.md target) after IT runs.

---

## 6. Docker Factory Reset — Impact (pre-reset checklist)

Reset wipes ALL containers/volumes/images. Inventory at last check:

### Safe to lose (Docker Desktop internals, auto re-pulled)
- Container `xenodochial_einstein` (docker/lsp).
- Images: kindest/node, desktop-cloud-provider-kind, envoy, registry-mirror,
  mcp/docker, docker/lsp(+treesitter).
- Volumes: `maven-cache`, `docker-lsp`, anonymous hash volumes, `aicarwash_redis_data` (cache only).

### ⚠️ BACK UP FIRST (may hold dev data)
- `aicarwash_postgres_data` — dev Postgres DB.
- `theah_database` — unknown DB, verify before wiping.

Backup volume → tarball:
```bash
docker run --rm -v aicarwash_postgres_data:/data -v "/a/AI Car Wash":/backup alpine \
  tar czf /backup/aicarwash_postgres_data.tgz -C /data .
```
> Tests do NOT need this data — they use throwaway containers. Back up only if dev DB matters.

---

## 7. Re-run Full Suite (after Docker fixed)

```bash
cd "A:/AI Car Wash/car-wash-backend"
mvn clean test
```
Expect: `Tests run: 41, Failures: 0, Errors: 0`.

---

## 8. Constraints (from CLAUDE.md — do not break)

- Stack locked: Java 21, Spring Boot 3.4, no dep bumps without instruction.
- Integration tests use Testcontainers — do NOT mock the DB.
- Flyway only — no `ddl-auto=create`.
- Soft delete only; constructor injection only; DTOs not entities in responses.
- No hardcoded secrets.
