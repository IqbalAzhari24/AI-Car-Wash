// Performance/load test for the core booking journey:
//   GET /api/v1/public/landing -> POST /api/v1/auth/register -> GET /api/v1/slots
//   -> POST /api/v1/bookings -> GET /api/v1/bookings/mine
//
// Run with k6 (https://k6.io). Examples:
//   k6 run tests/load/k6/booking-flow.js
//   BASE_URL=http://localhost:8080 PROFILE=load VUS=20 DURATION=1m k6 run tests/load/k6/booking-flow.js
//
// See tests/load/README.md for prerequisites and profile details.

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PROFILE = __ENV.PROFILE || 'smoke';
const VUS = Number(__ENV.VUS || 20);
const DURATION = __ENV.DURATION || '1m';

const errorRate = new Rate('errors');

const STAGES = {
  smoke: [
    { duration: '10s', target: 2 },
    { duration: '20s', target: 2 },
    { duration: '5s', target: 0 },
  ],
  load: [
    { duration: '30s', target: VUS },
    { duration: DURATION, target: VUS },
    { duration: '15s', target: 0 },
  ],
};

export const options = {
  stages: STAGES[PROFILE] || STAGES.smoke,
  thresholds: {
    'http_req_duration': ['p(95)<1000'],
    'http_req_duration{name:landing}': ['p(95)<300'],
    'http_req_duration{name:register}': ['p(95)<600'],
    'http_req_duration{name:slots}': ['p(95)<400'],
    'http_req_duration{name:create_booking}': ['p(95)<800'],
    'http_req_duration{name:my_bookings}': ['p(95)<400'],
    'errors': ['rate<0.05'],
  },
};

const VEHICLE_CLASSES = ['MOTORCYCLE', 'COMPACT', 'SEDAN', 'SUV_LUXURY', 'MPV_LARGE'];

function isoDate(date) {
  return date.toISOString().slice(0, 10);
}

/** Finds the nearest upcoming date (starting tomorrow) that has at least one open slot. */
function findAvailableSlot(authHeaders) {
  const base = new Date();
  for (let offset = 1; offset <= 6; offset++) {
    const date = new Date(base.getTime() + offset * 86400000);
    const dateStr = isoDate(date);

    const res = http.get(`${BASE_URL}/api/v1/slots?date=${dateStr}`, {
      headers: authHeaders,
      tags: { name: 'slots' },
    });

    const ok = check(res, { 'slots: status 200': (r) => r.status === 200 });
    errorRate.add(!ok);
    if (!ok) continue;

    const slots = res.json();
    const available = slots.filter((s) => s.available);
    if (available.length > 0) {
      const pick = available[(__VU + __ITER) % available.length];
      return pick.slotTime;
    }
  }
  return null;
}

export default function () {
  let services = [];
  let locationId = null;

  group('public landing', () => {
    const res = http.get(`${BASE_URL}/api/v1/public/landing`, {
      tags: { name: 'landing' },
    });
    const ok = check(res, {
      'landing: status 200': (r) => r.status === 200,
      'landing: has services': (r) => r.json('data.services').length > 0,
    });
    errorRate.add(!ok);
    if (ok) {
      const body = res.json();
      services = body.data.services;
      locationId = body.data.locations.length > 0 ? body.data.locations[0].id : null;
    }
  });

  if (services.length === 0) {
    sleep(1);
    return;
  }

  let token = null;

  group('register', () => {
    const email = `loadtest.vu${__VU}.iter${__ITER}.${Date.now()}@example.com`;
    const payload = JSON.stringify({
      email,
      password: 'LoadTest123!',
      phoneNumber: '0100000000',
    });
    const res = http.post(`${BASE_URL}/api/v1/auth/register`, payload, {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'register' },
    });
    const ok = check(res, {
      'register: status 201': (r) => r.status === 201,
      'register: has token': (r) => !!r.json('token'),
    });
    errorRate.add(!ok);
    if (ok) {
      token = res.json('token');
    }
  });

  if (!token) {
    sleep(1);
    return;
  }

  const authHeaders = {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  };

  const slotTime = findAvailableSlot(authHeaders);

  if (slotTime && locationId) {
    group('create booking', () => {
      const service = services[(__VU + __ITER) % services.length];
      const vehicleClass = VEHICLE_CLASSES[(__VU + __ITER) % VEHICLE_CLASSES.length];
      const payload = JSON.stringify({
        locationId,
        serviceId: service.id,
        slotTime,
        vehicleClass,
        vehicleModel: 'Load Test Car',
      });
      const res = http.post(`${BASE_URL}/api/v1/bookings`, payload, {
        headers: authHeaders,
        tags: { name: 'create_booking' },
      });
      // 409/422 are expected under concurrency (slot capacity races) and are not
      // counted as errors — only unexpected status codes are.
      const ok = check(res, {
        'booking: expected status': (r) => [201, 409, 422].includes(r.status),
      });
      errorRate.add(!ok);
    });
  }

  group('list my bookings', () => {
    const res = http.get(`${BASE_URL}/api/v1/bookings/mine`, {
      headers: authHeaders,
      tags: { name: 'my_bookings' },
    });
    const ok = check(res, { 'my bookings: status 200': (r) => r.status === 200 });
    errorRate.add(!ok);
  });

  sleep(1);
}
