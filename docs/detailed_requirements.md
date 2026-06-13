# Comprehensive System Requirements: Car Wash Booking Engine

## 1. Relational Database Schema & System Entities
The system relies on a PostgreSQL persistence ledger. Initialize utilizing these exact configurations:
- `users`: ID (UUID), email, password_hash, role (ENUM: customer, clerk, worker, owner).
- `slot_capacities`: location_id, slot_time (TIMESTAMP), max_limit (INT), booked_count (INT).
- `bookings`: ID, customer_id, clerk_id, worker_id, slot_time, v_class (ENUM), status (ENUM: pending, confirmed, in_progress, completed, no_show, cancelled), is_override (BOOLEAN).

## 2. Dynamic Vehicle Size Modifiers & Booking Slots
Every vehicle must be parsed by its size configuration profile, impacting operational block allocation (1 block = 30 minutes) and financial pricing matrices:
- `motorcycle`: -RM 10.00 | 0 Extra Blocks (30 min total footprint)
- `compact`: -RM 5.00 | 0 Extra Blocks (30 min total footprint)
- `sedan`: Baseline pricing | 0 Extra Blocks (30 min total footprint)
- `suv_luxury`: +RM 15.00 | +1 Extra Block (60 min total footprint)
- `mpv_large`: +RM 25.00 | +2 Extra Blocks (90 min total footprint)

*Operational Slot Constraint:* The booking engine must execute transactional safety checks. For `suv_luxury` and `mpv_large`, it must look ahead sequentially to confirm that contiguous blocks are available in the `slot_capacities` layout before confirming.

## 3. Self-Healing Inventory (No-Show Engine)
- Deploy an automated background cron scheduler running every 5 minutes (300 seconds).
- **Core Sweeper Logic:** Select all bookings with state `confirmed` where the current time is more than 15 minutes past the scheduled `slot_time`.
- **Atomic Operations:** Update the target booking status to `no_show`. Concurrently execute an atomic SQL decrement statement subtracting 1 from `booked_count` inside the corresponding `slot_capacities` rows to unlock immediate capacity for storefront walk-ins.

## 4. Comprehensive Rate-Limiting Thresholds (Redis)
All routes must intercept incoming user footprints inside an in-memory Redis token bucket middleware:
- **Chat Processing Endpoint (`/api/v1/timah/chat`):** Limit strictly to 20 messages per 5 minutes per authenticated JWT User ID.
- **Authentication Gate (`/api/v1/auth/login`):** Limit strictly to 5 submission attempts per 15 minutes mapped per inbound IP Address to block brute-forcing.

## 5. Middleware Bridges & Access Guards
- **Backend CORS Policy:** `CorsConfig.java` must explicitly allow credentials and map origins arriving from the default React-Vite local environment `http://localhost:5173`.
- **Frontend Route Protection:** A React routing module `ProtectedRoute.tsx` must parse decoded client-side JWT claims, blocking layout rendering and rerouting users who lack permission away from specific administration areas.

## 👥 Section 6: User Personas & Local Environment Seeding (Updated Staffing)

To guarantee the full-stack ecosystem can be instantly tested across realistic multi-user operations, the Spring Boot backend must implement an automated data seeding mechanism. On application startup, if the primary persistence store detects that the `users` table is entirely empty, it must initialize the runtime environment by pre-populating it with a specific roster of 2 Clerks, 5 Workers, 1 Owner, and 1 Customer.

All default passwords must be safely encrypted using the system's configured `BCryptPasswordEncoder` bean before database insertion.

### 🔐 Default Local Testing Credentials

| Role | Email Login | Password | System Assignment / Target View |
| :--- | :--- | :--- | :--- |
| **OWNER** | `theahmadiqbal24@gmail.com` | `OwnerPass123!` | Global Admin Console & Analytics Dashboard |
| **CLERK 1** | `clerk1@timahwash.com` | `ClerkPass123!` | Station 1 Operations Panel (Walk-ins & Overrides) |
| **CLERK 2** | `clerk2@timahwash.com` | `ClerkPass123!` | Station 2 Operations Panel (Valet/Driver Dispatch) |
| **WORKER 1** | `worker1@timahwash.com` | `WorkerPass123!`| Bay #1 Active Queue Tablet Checklist |
| **WORKER 2** | `worker2@timahwash.com` | `WorkerPass123!`| Bay #2 Active Queue Tablet Checklist |
| **WORKER 3** | `worker3@timahwash.com` | `WorkerPass123!`| Bay #3 Active Queue Tablet Checklist |
| **WORKER 4** | `worker4@timahwash.com` | `WorkerPass123!`| Detailing / Extra Capacity Floating Tablet |
| **WORKER 5** | `worker5@timahwash.com` | `WorkerPass123!`| Valet Pickup / Floating Tablet |
| **CUSTOMER**| `customer@gmail.com` | `CustomerPass123!` | Standard Consumer Sandbox & Timah Chat Client |

---

### 🚀 Backend Execution Requirements (`UserSeeder.java`)

1. **Startup Hook:** Establish a `UserSeeder` component implementing Spring Boot's `CommandLineRunner` interface so execution triggers immediately after database migrations complete.
2. **Deterministic Loop:** Use structured loops to programmatically generate `clerk{i}@timahwash.com` (1 to 2) and `worker{i}@timahwash.com` (1 to 5) to keep the initialization code clean and scalable.
3. **Idempotency Check:** Verify that `UserRepository.count() == 0` before executing insertions to protect against duplicate records on container hot-reloads.

---

### 💻 Frontend Dev-Mode Requirements (`Login.tsx`)

1. **Quick-Login Dock:** When the frontend application runs in development mode, render an organized "Staff Roster Quick Login" panel directly on the login screen.
2. **Role Selection Grid:** The panel must display grouped buttons for Clerks (1-2) and Workers (1-5). Clicking any specific worker or clerk button must instantly fill their exact seeded credentials and execute the login pipeline, allowing the developer to swap perspectives in the Antigravity integrated browser in under a second.

## 🤖 Section 7: Timah AI Conversational Core & WebSocket Pipeline

Timah is an intelligent, context-aware routing assistant. She does not just chat; she orchestrates booking states by dynamically interacting with backend services.

### 1. The Core Prompt Context Injection (System Persona)
Every chat session initialized must append a hidden System Prompt before processing user messages. The prompt must strictly enforce:
- **Identity:** "You are Timah, the elite AI receptionist for Timah Automated Car Wash."
- **Rules Boundaries:** Operational hours are 09:00 AM to 05:00 PM. The facility closes at 06:00 PM. No bookings can cross past 06:00 PM. Fridays are strictly closed.
- **Dynamic Context Hydration:** Before sending a request to the LLM, the backend must inject a JSON string of *live* data including: Today's date, current weather forecasting data, and a list of open slots from `slot_capacities`.

### 2. The Conversational Booking Loop & Function Calling
Timah must utilize tool-calling/function-calling protocols supported by the LLM:
- If the customer specifies a vehicle type, Timah must invoke `checkVehicleClass(model)` to determine if it is a compact, sedan, SUV, or MPV.
- If the customer requests a time slot, Timah must execute `verifyConsecutiveSlots(dateTime, blocks)` against the backend engine.
- Once details are verified, Timah provides a structured JSON response payload to the frontend containing a deep link to checkout: `{"action": "REDIRECT_CHECKOUT", "bookingId": "UUID"}`.

### 3. The WebSocket Streaming Middleware
- **Protocol:** Real-time bi-directional streaming via Spring WebSockets (`/ws/timah`).
- **Performance Requirement:** Chat responses must stream token-by-token using reactive emitters so the customer watches the response print in real-time, matching modern chat experiences.

## 🔐 Section 8: Owner Staff Provisioning & Access Control

To maintain strict security boundaries, the public registration endpoint is restricted to creating `CUSTOMER` accounts. The creation of `CLERK` and `WORKER` tiers must be executed exclusively by an authenticated `OWNER`.

### 1. The Staff Creation Pipeline
- **Endpoint:** `POST /api/v1/owner/staff`
- **Security Guard:** Must be protected by Spring Security, requiring a valid JWT containing the `ROLE_OWNER` authority. Any other role attempting to hit this endpoint must receive a `403 Forbidden` response.
- **Payload Requirements:** Accepts a JSON object containing `email`, `phoneNumber`, `role` (ENUM: CLERK, WORKER), and a temporary `password`.
- **Validation Engine:** The backend must cross-reference the incoming email and phone number against the database. If a conflict occurs, it must return a structured `409 Conflict` error to prevent duplicates.

### 2. Frontend Management Interface (`StaffManagement.tsx`)
- **The Admin View:** A dedicated sub-layout inside the Owner Dashboard displaying a master datatable of the active workforce (the 2 Clerks and 5 Workers).
- **The Action Drawer:** A slide-out form allowing the owner to input a new employee's details. 
- **Temporary Password Protocol:** Upon successful creation, the UI displays a secure modal showing a temporary password card with a "Copy to Clipboard" utility, allowing the owner to hand off the initial login credentials to the new employee.