# CLAUDE.md — AI Car Wash Project Master Guide
> **Scope:** This file is the single source of truth for all AI-assisted work on the AzureWash project.
> **Target:** Claude Code, Claude.ai Projects, Claude API
> **Version:** 1.1.0 | Author: Ahmad Iqbal bin Azhari | Updated: 2026-07-01

---

## ⚡ PRIMACY RULES — Read First, Always

These rules override everything else. NEVER violate them.

1. **Read this file completely before taking any action.** If CLAUDE.md exists in the project root, load it before writing a single line of code or prose.
2. **No invention.** NEVER fabricate statistics, API behaviour, library versions, configuration values, or architectural decisions not explicitly stated in this file or the provided source files.
3. **No scope creep.** Only make changes that were directly requested. Do NOT refactor, rename, reorganise, or "improve" code beyond the task boundary.
4. **No silent failures.** If you are uncertain about any decision, STOP and ask. Do not guess and proceed.
5. **Java-first.** All backend code is Java 21 with Spring Boot 3.4. Do not suggest Node.js, Python, or any other language for backend tasks.
6. **Verify before delivering.** For any substantive output (architecture doc, API spec, security config, report), perform a self-check: re-read the relevant section of this file and confirm every claim, config value, and architectural decision is consistent. Flag inconsistencies with `[VERIFY]` before delivering.

---

## 🏗️ Project Identity

| Field | Value |
|---|---|
| **Project Name** | AI Car Wash |
| **Type** | Full-Stack AI-Powered Car Wash Booking & Management System |
| **Purpose** | Final Year Project (FYP) — Universiti Malaysia Terengganu (UMT) |
| **Student** | Ahmad Iqball bin Azhari |
| **Company Context** | ABC Software Sdn Bhd (software architecture design involvement) |
| **Primary Language** | Bahasa Malaysia + English (bilingual codebase comments acceptable) |

---

## 🛠️ Confirmed Tech Stack

MUST use these exact versions. Do NOT upgrade, suggest alternatives, or assume newer versions unless explicitly told.

### Backend
| Component | Value |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Security | Spring Security 6 + JWT (stateless) |
| ORM | Spring Data JPA / Hibernate |
| Build | Maven |
| AI Chatbot | Spring AI + Google Gemini (chatbot named **Timah**) |
| Scheduler | Spring `@Scheduled` (no-show sweeper) |

### Frontend
| Component | Value |
|---|---|
| Framework | React 19 |
| Styling | Tailwind CSS |
| State | React Query + Zustand (confirm per module) |

### Data & Infrastructure
| Component | Value |
|---|---|
| Primary DB | PostgreSQL 15 |
| Cache / Rate Limit | Redis 7 |
| Containerisation | Docker Compose |
| Payment Gateway | ToyyibPay (FPX) — FYP phase only |

### Geolocation
- Haversine formula for valet geofencing
- No third-party mapping SDK unless explicitly requested

---

## 👥 System Roles

AI Car Wash has exactly **four roles**. Do not add, merge, or rename roles without explicit instruction.

| Role | Description |
|---|---|
| `CUSTOMER` | Books car wash, tracks status, uses valet service |
| `CLERK` | Manages bookings, handles walk-ins, confirms payments |
| `WORKER` | Executes assigned jobs, updates job status |
| `OWNER` | Full system access, analytics, staff management |

---

## 🔐 Security Architecture

### Authentication
- Stateless JWT — access token + refresh token pattern
- Spring Security 6 method-level security (`@PreAuthorize`)
- Role-based access control (RBAC) via `GrantedAuthority`
- Password encoding: `BCryptPasswordEncoder`

### Rate Limiting
- Redis-backed token bucket per user/IP
- Applied at the filter chain level, NOT at the controller level

### Endpoint Conventions
- Public endpoints: `/api/v1/auth/**`, `/api/v1/public/**`
- Protected endpoints: all others — require valid JWT
- NEVER expose internal IDs in public-facing URLs without UUID masking

---

## 📅 Slot Availability Engine

- Islamic calendar compliance is a hard requirement (Hari Raya, Friday prayer windows)
- Malaysia public holidays via `MalaysiaCalendarService`
- No-show sweeper runs on `@Scheduled` cron — marks bookings as `NO_SHOW` after grace period
- Slot state transitions follow the booking state machine (see Booking States section)

---

## 📦 Booking State Machine

Valid states and transitions ONLY. Do not add states or transitions without instruction.

```
PENDING → CONFIRMED → IN_PROGRESS → COMPLETED
PENDING → CANCELLED
CONFIRMED → CANCELLED
IN_PROGRESS → NO_SHOW  ← (triggered by scheduler only)
```

---

## 🚗 Valet Service

- Geofencing uses Haversine distance calculation
- Service radius is configurable per owner setting (stored in DB, not hardcoded)
- Valet requests are a separate entity from standard bookings — do NOT merge them

---

## 💳 Payment — ToyyibPay

- Gateway: ToyyibPay (FPX)
- Webhook/IPN callbacks are server-to-server — NEVER trust client-side payment confirmation
- Transaction status MUST be verified via server callback before updating booking state
- Payment entity is separate from Booking entity (foreign key relationship)
- Do NOT integrate Stripe, Billplz, or other gateways in the FYP codebase unless explicitly instructed

---

## 🤖 AI Agent — Timah

- Name: **Timah**
- Backend: Google Gemini 2.5 Flash via WebClient (streaming + function-calling)
- Scope: customer-facing **agentic booking assistant** — Q&A, slot lookup, service recommendations, and end-to-end booking creation
- Tool-calling enabled for the following actions:

| Tool | Access | Description |
|---|---|---|
| `get_available_slots` | Read | List open 30-min slots for a given date |
| `get_services` | Read | Return the wash service catalogue |
| `get_booking_status` | Read | Look up an existing booking by ID |
| `create_booking` | **Write** | Create a PENDING booking on behalf of the authenticated customer |

- **Agentic flow:** Timah must follow a strict Read → Confirm → Book sequence:
  1. Read available slots and services first
  2. Present options and ask for explicit customer confirmation before booking
  3. Only call `create_booking` after confirmation; redirect to `/checkout/:bookingId`
- `create_booking` is **scoped to the authenticated customer only** — Timah MUST pass the JWT subject (`userId`) as the customer ID; it CANNOT book on behalf of other users or staff
- Timah MUST NOT call any mutation other than `create_booking` (no cancellations, no status changes, no payment mutations)
- All tool-call results MUST be presented to the customer before any follow-up action

---

## 🗄️ Database Conventions

- PostgreSQL 15 — snake_case column names
- UUID primary keys for all user-facing entities
- `created_at` / `updated_at` audit columns on every table
- Soft deletes via `deleted_at` nullable column — NEVER hard delete user or booking records
- Schema migrations via Flyway — do NOT use `spring.jpa.hibernate.ddl-auto=create` in any environment

---

## 📁 Project Structure Convention

```
azurewash/
├── backend/                    # Spring Boot 3.4 application
│   ├── src/main/java/com/azurewash/
│   │   ├── config/             # Security, Redis, Gemini config
│   │   ├── controller/         # REST controllers (thin — delegate to service)
│   │   ├── service/            # Business logic
│   │   ├── repository/         # Spring Data JPA repositories
│   │   ├── entity/             # JPA entities
│   │   ├── dto/                # Request/Response DTOs (no entity leakage)
│   │   ├── security/           # JWT filter, UserDetails, RBAC
│   │   ├── scheduler/          # @Scheduled jobs
│   │   ├── chatbot/            # Timah — Spring AI integration
│   │   └── util/               # Haversine, calendar utilities
│   └── src/main/resources/
│       ├── db/migration/       # Flyway SQL migrations
│       └── application.yml     # Config (NO secrets in this file)
├── frontend/                   # React 19 application
│   └── src/
│       ├── components/
│       ├── pages/
│       ├── hooks/
│       └── api/                # Axios API layer
├── docker-compose.yml
└── CLAUDE.md                   ← YOU ARE HERE
```

---

## ✍️ Code Style Rules

### Java (Backend)
- Constructor injection ONLY — no `@Autowired` on fields
- DTOs for all API input/output — entities MUST NOT be serialised directly to JSON
- Service layer owns all business logic — controllers are thin routers
- `@Transactional` on service methods that modify state
- Checked exceptions wrapped in custom `AzureWashException` hierarchy
- No `System.out.println` — use SLF4J `log.info` / `log.error`
- All public methods in service classes MUST have Javadoc

### React (Frontend)
- Functional components only — no class components
- Custom hooks for all data-fetching logic
- No inline styles — Tailwind utility classes only
- Component files: PascalCase. Hooks: `use` prefix. Utilities: camelCase.

---

## 🧪 Testing Conventions

- Unit tests: JUnit 5 + Mockito
- Integration tests: `@SpringBootTest` with test containers (PostgreSQL + Redis)
- Test class naming: `[ClassName]Test.java`
- Minimum coverage target: 70% for service layer
- NEVER mock the database in integration tests — use Testcontainers

---

## 🚫 Hard Constraints — NEVER Do These

| Constraint | Reason |
|---|---|
| Do NOT use `spring.jpa.hibernate.ddl-auto=create` | Schema managed by Flyway only |
| Do NOT hardcode secrets, API keys, or DB passwords | Use environment variables / `.env` |
| Do NOT expose JPA entities directly in REST responses | Always use DTOs |
| Do NOT add CoT scaffolding to reasoning-native models | Degrades output |
| Do NOT upgrade dependency versions without instruction | FYP stack is locked |
| Do NOT merge Valet and Booking entities | Separate domain objects |
| Do NOT give Timah mutations beyond `create_booking` | Only the agentic booking tool is permitted |
| Do NOT let Timah book on behalf of other users | Always use JWT subject as customer ID |
| Do NOT hard delete user or booking records | Soft delete only |
| Do NOT use `@Autowired` on fields | Constructor injection only |

---

## 📐 API Design Conventions

- Base path: `/api/v1/`
- RESTful resource naming: plural nouns (`/bookings`, `/users`, `/slots`)
- HTTP methods: `GET` read, `POST` create, `PUT` full replace, `PATCH` partial update, `DELETE` soft-delete
- Response envelope:
```json
{
  "status": "success | error",
  "data": {},
  "message": "human-readable string",
  "timestamp": "ISO-8601"
}
```
- Pagination: `?page=0&size=20&sort=createdAt,desc`
- Error codes follow `AW-[DOMAIN]-[CODE]` convention (e.g. `AW-BOOKING-404`)

---

## 📋 Task Execution Protocol

When given any task, Claude MUST follow this sequence:

```
1. LOAD     → Read CLAUDE.md fully before any action
2. SCOPE    → State exactly what will be changed and what will NOT be changed
3. PLAN     → List the files/classes that will be created or modified
4. EXECUTE  → Implement only what was scoped
5. VERIFY   → Check output against this file's constraints
6. DELIVER  → Present output with a short summary of what changed
```

For any task touching security, payment, or database schema — add:
```
7. FLAG     → Explicitly state any security implications or migration required
```

---

## 🔍 Self-Verification Checklist

Before delivering any code or document, confirm:

- [ ] Stack versions match Section "Confirmed Tech Stack"
- [ ] No entities exposed directly in API responses (DTOs used)
- [ ] No hardcoded secrets or credentials
- [ ] Role boundaries respected (Customer / Clerk / Worker / Owner)
- [ ] Booking state transitions valid per state machine
- [ ] Payment callbacks are server-side only
- [ ] Timah only calls `create_booking` (no other mutations) and scopes it to the JWT subject
- [ ] Soft deletes used (not hard deletes) for user/booking records
- [ ] Flyway migration file created for any schema change
- [ ] Constructor injection used (not field `@Autowired`)

---

## 🗺️ Source Map Protocol

For any document, report, or specification generated from source materials:

1. Read ALL source files before writing
2. Every factual claim traces to a specific source (file + section)
3. Unverifiable claims tagged `[NEEDS SOURCE]`
4. After draft: run verification pass — re-read sources, check every claim
5. Remove unverified claims from the clean draft; list them separately
6. NEVER invent statistics, percentages, or quotes — not even "directionally correct" ones

---

## 📝 Academic Deliverables Context

When assisting with academic documents (FYP report, coursework):

- Follow UMT academic writing conventions
- Citations: IEEE format unless otherwise specified
- Do NOT invent references — only cite sources explicitly provided
- Flag placeholder references as `[CITE: describe what needs to be sourced]`
- Reports in English unless module requires Bahasa Malaysia

---

## 🔄 Session Memory Block

Carry this context forward in every session:

```
## AI Car Wash — Persistent Decisions
- Payment gateway: ToyyibPay (FPX) — locked for FYP phase
- AI agent: Timah, powered by Google Gemini 2.5 Flash (WebClient streaming + function-calling); agentic booking allowed with Read→Confirm→Book guardrails
- Geofencing: Haversine formula (no third-party SDK)
- Calendar compliance: Islamic calendar + Malaysia public holidays
- Schema management: Flyway only (ddl-auto=validate in prod)
- Authentication: stateless JWT, Spring Security 6
- Cache: Redis 7 for rate limiting and session hints
- Containerisation: Docker Compose (not Kubernetes for FYP phase)
- Frontend: React 19, Tailwind CSS
- Database: PostgreSQL 15, UUID PKs, soft deletes, audit columns
```

---

*End of CLAUDE.md — If any section contradicts a direct user instruction given in the active session, the direct instruction takes precedence. Update this file to reflect confirmed changes.*
