# Antigravity Workspace System Rules

## Tech Stack Constraints
- Backend Framework: Java 21, Spring Boot 3.x, Spring Data JPA, Spring Security
- Frontend Framework: React 19, TypeScript, Vite, TailwindCSS
- Databases: PostgreSQL (Primary Ledger), Redis (Rate Limiter Caching Layer)

## Architectural Enforcement
- All incoming requests must route through `JwtAuthenticationFilter` and `RateLimitingFilter` (via Redis token buckets).
- Multi-slot time blocks must follow sequential logic: verify consecutive available intervals in `slot_capacities` table before approving a booking.
- Business parameters (Friday closures, size price modifiers) must be fully dynamic database reads, never hardcoded values.

## Agent Behavior Conventions
- **Plan First**: Always output a structured Implementation Plan artifact for human verification before running file system updates.
- **Terminal Autonomy**: You are permitted to execute Maven builds, npm installs, and docker-compose tasks autonomously.
- **Browser Verification**: Once the frontend is running, utilize your integrated browser agent (`/browser`) to verify that the Auth Guard routes block unauthorized users.