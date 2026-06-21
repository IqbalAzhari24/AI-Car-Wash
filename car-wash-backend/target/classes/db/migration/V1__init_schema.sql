-- ==========================================
-- AI Car Wash (Timah Wash) — initial schema
--
-- Design notes:
--  * Enum-like columns are VARCHAR (not native PG enum types) because the JPA
--    entities map them with @Enumerated(STRING); this keeps Hibernate
--    ddl-auto=validate happy and avoids enum<->varchar cast issues on insert.
--  * All timestamps are TIMESTAMP WITHOUT TIME ZONE to match the entities'
--    java.time.LocalDateTime fields.
--  * Every child entity owns its FK column; locations is the multi-branch root.
-- ==========================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==========================================
-- LOCATIONS (multi-branch root)
-- ==========================================
CREATE TABLE locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- ==========================================
-- USERS (customers + staff)
-- ==========================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- SERVICES (wash catalogue)
-- ==========================================
CREATE TABLE services (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    duration_minutes INT NOT NULL,
    vehicle_size_multiplier DECIMAL(3, 2) DEFAULT 1.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- ==========================================
-- SLOT CAPACITIES (per-branch 30-min inventory)
-- ==========================================
CREATE TABLE slot_capacities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    location_id UUID NOT NULL,
    slot_time TIMESTAMP NOT NULL,
    max_limit INT NOT NULL DEFAULT 3,        -- max cars per 30-min window
    booked_count INT NOT NULL DEFAULT 0,
    CONSTRAINT unique_location_slot UNIQUE (location_id, slot_time),
    CONSTRAINT chk_booked_count CHECK (booked_count >= 0),
    FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE CASCADE
);

-- ==========================================
-- BOOKINGS (the hub)
-- ==========================================
CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL,
    clerk_id UUID,                            -- NULL when booked online via Timah
    worker_id UUID,                           -- assigned bay specialist
    location_id UUID NOT NULL,
    slot_time TIMESTAMP NOT NULL,
    vehicle_class VARCHAR(20) NOT NULL,
    vehicle_model VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_override BOOLEAN NOT NULL DEFAULT FALSE,
    total_price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (clerk_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (worker_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE RESTRICT
);

-- ==========================================
-- PAYMENTS (one per booking)
-- ==========================================
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id UUID UNIQUE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    method VARCHAR(30) NOT NULL,
    transaction_id VARCHAR(100) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- ==========================================
-- SHOP CLOSURES (per-branch downtime windows)
-- ==========================================
CREATE TABLE shop_closures (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    location_id UUID NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    reason VARCHAR(255),

    FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE CASCADE
);

-- ==========================================
-- REVIEWS & AI SENTIMENT (owner analytics)
-- ==========================================
CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id UUID UNIQUE NOT NULL,
    customer_id UUID NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    sentiment_score DECIMAL(3, 2),           -- AI-computed, range -1.00 .. 1.00
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ==========================================
-- SYSTEM LOGS
-- ==========================================
CREATE TABLE system_logs (
    id BIGSERIAL PRIMARY KEY,
    level VARCHAR(10) NOT NULL,
    logger VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    exception_trace TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- INDEXES
-- ==========================================
-- Sequential look-ahead in the booking engine
CREATE INDEX idx_slot_capacities_time ON slot_capacities(slot_time, location_id);
-- No-show cron sweep
CREATE INDEX idx_bookings_status_time ON bookings(status, slot_time);
-- Customer history / directory analytics
CREATE INDEX idx_bookings_customer ON bookings(customer_id);
CREATE INDEX idx_reviews_sentiment ON reviews(sentiment_score);
