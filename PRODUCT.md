# Product

## Register

product

## Users

- **Customers** of a Malaysian neighborhood car wash. They book a wash by chatting with "Timah", the AI receptionist — almost always on a phone, often outdoors in bright daylight, mid-errand. Their job: get a slot booked in under a minute without learning an app.
- **Staff** — Owner, Clerks, Workers. The Owner manages the business from a laptop at the counter or at home: browsing the user directory, checking a customer's transaction history. Clerks and Workers sign in to their operational dashboard.

## Product Purpose

An AI-receptionist booking system for a car wash. Timah (a chat agent) handles slot booking conversationally over WebSocket; the admin side gives the Owner oversight of accounts and payments (amounts in RM). Success looks like: a customer books without friction, and the Owner trusts the numbers at a glance.

## Brand Personality

Fresh, friendly, dependable. A well-run local business that happens to have a clever AI receptionist — not a tech startup. Timah has warmth and a touch of charm in the chat; everywhere else the interface stays quiet and gets out of the way.

## Anti-references

- Stock Tailwind-UI blue-on-gray SaaS — the generic "AI built this" look the app currently has.
- Dark "AI terminal" chat aesthetics (gray-950 walls, neon accents). Timah is a receptionist, not a hacker console.
- Cream/beige "warm AI default" surfaces. Freshness lives in the brand color, not a tinted background.
- Corporate fintech severity. RM amounts must be trustworthy, but the brand is a neighborhood service, not a bank.

## Design Principles

1. **Booking is the product** — every customer surface should shorten the path to a confirmed slot; the chat is the hero surface and the only place that earns extra personality.
2. **Daylight-first** — light theme, strong contrast, generous touch targets; designed for a phone held in the sun.
3. **One vocabulary** — the same buttons, inputs, badges, and tables on every screen; admin and customer surfaces are the same product.
4. **States are designed, not defaulted** — loading skeletons, taught empty states, readable errors; "Loading…" text is a bug.

## Accessibility & Inclusion

WCAG AA: body text ≥4.5:1, large text ≥3:1, visible focus rings on all interactive elements, real labels on form fields, `prefers-reduced-motion` alternatives for every animation, semantic HTML (buttons are buttons, tables announce sort state).
