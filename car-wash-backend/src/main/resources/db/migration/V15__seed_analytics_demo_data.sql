-- ==========================================
-- V15 — 3-year analytics demo data
--
-- Backfills realistic sales history so the Owner Analytics dashboard
-- (summary/trend/insight) has something meaningful to show:
--   * 40 customers with organic-looking emails (not test1@test.com)
--   * ~3 years of bookings (Fridays skipped — shop closed), weighted
--     weekday/weekend, mostly COMPLETED with a CANCELLED/NO_SHOW mix
--   * one payment per booking, dated on the booking's calendar day
--     (PaymentRepository/OwnerAnalyticsService filter revenue by
--     payment.created_at, so this must line up with slot_time's day)
--   * reviews for ~50% of COMPLETED bookings
--
-- Purely additive demo data — no schema changes. Safe to run against the
-- existing dev DB (clerk/worker ids resolve to NULL if none exist yet,
-- since bookings.clerk_id/worker_id are nullable).
--
-- Random picks are done via array indexing (array_agg + floor(random()*n)),
-- NOT "ORDER BY random() LIMIT 1" subqueries — Postgres can cache/hoist an
-- uncorrelated scalar subquery and reuse the same row for every outer row,
-- which silently collapses "random per booking" into "one value for all
-- 3800+ bookings". Array indexing is a plain expression, not a subquery, so
-- it re-evaluates per row like any other column.
--
-- ponytail: customer signup dates are randomized independently of their
-- booking dates (no signup-before-first-booking correlation enforced) —
-- fine for demo filler data; revisit only if a report ever asserts on it.
-- ==========================================

SELECT setseed(0.42);

CREATE EXTENSION IF NOT EXISTS pgcrypto;

WITH name_pool AS (
    SELECT * FROM (VALUES
        ('Ahmad','Rahman'),('Siti','Aminah'),('Muhammad','Hafiz'),('Nurul','Huda'),
        ('Amirul','Hakim'),('Farah','Diana'),('Azman','Ismail'),('Aina','Sofia'),
        ('Zulkifli','Yusof'),('Aisyah','Zulkarnain'),('Faizal','Rashid'),('Wan','Nadia'),
        ('Hafiz','Amin'),('Zainab','Idris'),('Kamal','Azhar'),('Suraya','Baharin'),
        ('Rizal','Hashim'),('Mastura','Kassim'),('Firdaus','Ramli'),('Noraini','Yaakob'),
        ('Halim','Zainuddin'),('Liyana','Roslan'),('Syafiq','Nazri'),('Rohana','Salleh'),
        ('Danial','Hakimi'),('Fatimah','Zahra'),('Iskandar','Shah'),('Azlan','Rahim'),
        ('Azrul','Nizam'),('Sharifah','Aleya'),('Hakim','Fadzil'),('Melissa','Lim'),
        ('Wei','Chong'),('Mei','Ling'),('Ravi','Kumar'),('Priya','Devi'),
        ('Suresh','Kumar'),('Kah','Wai'),('Su','Lin'),('Zhi','Hao')
    ) AS t(first_name, last_name)
),
numbered_names AS (
    SELECT row_number() OVER () AS rn, first_name, last_name FROM name_pool
),
domain_pool AS (
    SELECT * FROM (VALUES ('gmail.com'),('yahoo.com'),('hotmail.com'),('outlook.com'),('icloud.com')) AS t(domain)
),
customer_seed AS (
    SELECT
        n.rn,
        n.first_name,
        n.last_name,
        (SELECT domain FROM domain_pool ORDER BY random() LIMIT 1) AS domain,
        floor(random() * 3)::int AS email_style
    FROM numbered_names n
),
customers AS (
    INSERT INTO users (id, email, password_hash, phone_number, role, created_at)
    SELECT
        uuid_generate_v4(),
        CASE email_style
            WHEN 0 THEN lower(first_name) || '.' || lower(last_name) || '@' || domain
            WHEN 1 THEN lower(left(first_name, 1)) || lower(last_name) || rn::text || '@' || domain
            ELSE lower(first_name) || lower(left(last_name, 1)) || rn::text || '@' || domain
        END,
        crypt('CustomerPass123!', gen_salt('bf')),
        '+601' || (ARRAY['1','2','3','4','6','7','8','9'])[1 + floor(random() * 8)::int]
            || lpad(floor(random() * 10000000)::text, 7, '0'),
        'CUSTOMER',
        (CURRENT_DATE - INTERVAL '3 years' + (random() * INTERVAL '30 months'))::timestamp
    FROM customer_seed
    RETURNING id
),

-- ==========================================
-- Small lookup pools, materialized once as arrays for per-row indexing
-- ==========================================
customer_pool AS (
    SELECT array_agg(id) AS ids FROM customers
),
service_pool AS (
    SELECT array_agg(id ORDER BY id) AS ids, array_agg(price ORDER BY id) AS prices FROM services
),
clerk_pool AS (
    SELECT array_agg(id) AS ids FROM users WHERE role = 'CLERK'
),
worker_pool AS (
    SELECT array_agg(id) AS ids FROM users WHERE role = 'WORKER'
),

-- ==========================================
-- Day-by-day booking volume (3yrs back .. yesterday, Fridays closed)
-- ==========================================
days AS (
    SELECT gs::date AS d
    FROM generate_series(
        CURRENT_DATE - INTERVAL '3 years',
        CURRENT_DATE - INTERVAL '1 day',
        INTERVAL '1 day'
    ) gs
    WHERE EXTRACT(DOW FROM gs) <> 5 -- 5 = Friday
),
day_counts AS (
    SELECT d,
        CASE WHEN EXTRACT(DOW FROM d) IN (0, 6)
             THEN 4 + floor(random() * 4)::int   -- weekend: 4-7
             ELSE 2 + floor(random() * 4)::int    -- weekday: 2-5
        END AS n
    FROM days
),
booking_slots AS (
    SELECT
        d,
        random() AS r_vclass,
        random() AS r_status,
        (d + '09:00'::time + (floor(random() * 18) * INTERVAL '30 minutes'))::timestamp AS slot_time
    FROM day_counts, generate_series(1, day_counts.n)
),
booking_vclass AS (
    SELECT
        *,
        CASE
            WHEN r_vclass < 0.35 THEN 'SEDAN'
            WHEN r_vclass < 0.60 THEN 'COMPACT'
            WHEN r_vclass < 0.75 THEN 'MOTORCYCLE'
            WHEN r_vclass < 0.90 THEN 'SUV_LUXURY'
            ELSE 'MPV_LARGE'
        END AS vclass
    FROM booking_slots
),
booking_idx AS (
    SELECT
        bv.slot_time,
        bv.vclass,
        bv.r_status,
        cp.ids AS cust_ids,
        sp.ids AS svc_ids,
        sp.prices AS svc_prices,
        clkp.ids AS clerk_ids,
        wrkp.ids AS worker_ids,
        1 + floor(random() * array_length(cp.ids, 1))::int AS cust_idx,
        1 + floor(random() * array_length(sp.ids, 1))::int AS svc_idx,
        1 + floor(random() * array_length(clkp.ids, 1))::int AS clerk_idx,
        1 + floor(random() * array_length(wrkp.ids, 1))::int AS worker_idx
    FROM booking_vclass bv
    CROSS JOIN customer_pool cp
    CROSS JOIN service_pool sp
    CROSS JOIN clerk_pool clkp
    CROSS JOIN worker_pool wrkp
),
booking_full AS (
    SELECT
        slot_time,
        vclass,
        CASE vclass
            WHEN 'MOTORCYCLE' THEN (ARRAY['Yamaha Y15ZR','Honda EX5','Modenas Kriss'])[1 + floor(random() * 3)::int]
            WHEN 'COMPACT'    THEN (ARRAY['Perodua Myvi','Perodua Axia','Perodua Bezza'])[1 + floor(random() * 3)::int]
            WHEN 'SEDAN'      THEN (ARRAY['Proton Saga','Honda City','Toyota Vios'])[1 + floor(random() * 3)::int]
            WHEN 'SUV_LUXURY' THEN (ARRAY['Honda CR-V','Proton X70','Mazda CX-5'])[1 + floor(random() * 3)::int]
            ELSE                   (ARRAY['Perodua Alza','Toyota Innova','Proton Exora'])[1 + floor(random() * 3)::int]
        END AS vehicle_model,
        CASE vclass
            WHEN 'SUV_LUXURY' THEN 1.60
            WHEN 'MPV_LARGE'  THEN 2.00
            ELSE 1.00
        END AS class_multiplier,
        CASE
            WHEN r_status < 0.85 THEN 'COMPLETED'
            WHEN r_status < 0.93 THEN 'CANCELLED'
            ELSE 'NO_SHOW'
        END AS status,
        cust_ids[cust_idx] AS customer_id,
        svc_ids[svc_idx] AS service_id,
        svc_prices[svc_idx] AS service_price,
        clerk_ids[clerk_idx] AS clerk_id,
        worker_ids[worker_idx] AS worker_id,
        (SELECT id FROM locations LIMIT 1) AS location_id,
        (slot_time - (random() * INTERVAL '4 hours'))::timestamp AS created_at
    FROM booking_idx
),
new_bookings AS (
    INSERT INTO bookings (
        id, customer_id, clerk_id, worker_id, location_id, service_id,
        slot_time, vehicle_class, vehicle_model, status, is_override,
        total_price, created_at
    )
    SELECT
        uuid_generate_v4(), customer_id, clerk_id, worker_id, location_id, service_id,
        slot_time, vclass, vehicle_model, status, FALSE,
        round(service_price * class_multiplier, 2), created_at
    FROM booking_full
    RETURNING id, customer_id, status, total_price, created_at, slot_time
),

-- ==========================================
-- One payment per booking (amount/date carried straight from the booking)
-- ==========================================
booking_payment_seed AS (
    SELECT
        nb.*,
        CASE WHEN random() < 0.70 THEN 'CASH' ELSE 'TOYYIBPAY' END AS pay_method,
        random() AS pay_rand
    FROM new_bookings nb
),
new_payments AS (
    INSERT INTO payments (id, booking_id, amount, payment_status, method, transaction_id, created_at)
    SELECT
        uuid_generate_v4(),
        id,
        total_price,
        CASE
            WHEN pay_method = 'CASH' THEN 'COMPLETED'
            WHEN pay_rand < 0.90 THEN 'COMPLETED'
            WHEN pay_rand < 0.97 THEN 'PENDING'
            ELSE 'FAILED'
        END,
        pay_method,
        CASE WHEN pay_method = 'TOYYIBPAY'
             THEN 'TYB' || to_char(created_at, 'YYYYMMDD') || substr(md5(id::text), 1, 8)
             ELSE NULL
        END,
        created_at
    FROM booking_payment_seed
    RETURNING booking_id
),

-- ==========================================
-- Reviews for ~50% of COMPLETED bookings
-- ==========================================
review_seed_raw AS (
    SELECT
        nb.id AS booking_id,
        nb.customer_id,
        nb.slot_time,
        random() AS r_rating
    FROM new_bookings nb
    JOIN new_payments np ON np.booking_id = nb.id
    WHERE nb.status = 'COMPLETED' AND random() < 0.5
),
review_seed AS (
    SELECT
        booking_id,
        customer_id,
        slot_time,
        CASE
            WHEN r_rating < 0.55 THEN 5
            WHEN r_rating < 0.85 THEN 4
            WHEN r_rating < 0.95 THEN 3
            ELSE 2
        END AS rating
    FROM review_seed_raw
),
review_full AS (
    SELECT
        *,
        CASE rating
            WHEN 5 THEN (ARRAY['Excellent service, car looks brand new!','Sangat memuaskan, kereta bersih berkilat.','Fast and friendly staff, highly recommend.','Best wash in town, will come back again.'])[1 + floor(random() * 4)::int]
            WHEN 4 THEN (ARRAY['Good job, minor spots left inside.','Bagus, cepat dan kemas.','Solid wash, reasonable price.'])[1 + floor(random() * 3)::int]
            WHEN 3 THEN (ARRAY['Average wash, took longer than expected.','Okay je, boleh improve sikit.'])[1 + floor(random() * 2)::int]
            ELSE (ARRAY['Not fully satisfied, some dirt remained.','Kurang memuaskan kali ini.'])[1 + floor(random() * 2)::int]
        END AS comment
    FROM review_seed
)
INSERT INTO reviews (id, booking_id, customer_id, rating, comment, sentiment_score, created_at)
SELECT
    uuid_generate_v4(),
    booking_id,
    customer_id,
    rating,
    comment,
    round((rating - 3) / 2.0, 2),
    slot_time + INTERVAL '1 hour'
FROM review_full;
