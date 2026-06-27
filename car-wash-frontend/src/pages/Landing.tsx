import React from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowRight, Car, Droplets, Sparkles, Check, Bot, Activity, Wallet, RefreshCw,
  Terminal, MapPin, Building2, Clock, Zap,
} from 'lucide-react';

// Ported from the Stitch "AI Car Wash" landing design. Header + bottom nav live in
// Layout, so only the page body is here. Icons use lucide (project dep); colours and
// texture reuse the Cyber-Honeycomb tokens in index.css / ui.ts. Visitors aren't
// authenticated, so every CTA routes to /login.
//
// All figures are real and sourced from the backend:
//   prices/durations/descriptions → V8__seed_catalog.sql
//   3 cars / 30-min slot, 14-day window → CatalogSeeder
//   no-show auto-release 15 min → NoShowAutomationCron
//   location + hours → V8 seed + CatalogSeeder (Fri closed, 09:00–17:30)

const SERVICES = [
  {
    duration: '30 min',
    name: 'Standard Wash',
    icon: Car,
    accent: '#00F0FF',
    price: 'RM 25',
    blurb: 'Exterior wash and dry.',
    features: ['Exterior wash', 'Hand dry'],
    recommended: false,
  },
  {
    duration: '45 min',
    name: 'Premium Wash',
    icon: Droplets,
    accent: '#00F0FF',
    price: 'RM 45',
    blurb: 'Exterior plus interior vacuum and wipe-down.',
    features: ['Everything in Standard', 'Interior vacuum', 'Interior wipe-down'],
    recommended: true,
  },
  {
    duration: '90 min',
    name: 'Full Detailing',
    icon: Sparkles,
    accent: '#00E5A0',
    price: 'RM 120',
    blurb: 'Deep clean, polish and wax.',
    features: ['Everything in Premium', 'Deep clean', 'Polish & wax'],
    recommended: false,
  },
] as const;

const FEATURES = [
  { icon: Bot, title: 'Book with Timah', desc: 'Chat naturally with our AI assistant — it checks availability and locks your slot.' },
  { icon: Activity, title: 'Real-Time Slots', desc: 'Live availability across a 14-day window, 3 vehicles per 30-minute slot.' },
  { icon: Wallet, title: 'Cashless Payment', desc: 'Pay online via ToyyibPay FPX, confirmed by secure server callback.' },
  { icon: RefreshCw, title: 'No-Show Auto-Release', desc: 'Unclaimed slots free up automatically 15 minutes past their start time.' },
] as const;

export const Landing: React.FC = () => {
  return (
    <div className="flex flex-1 flex-col bg-[#0D0D11]">
      {/* Hero */}
      <section className="hex-radial-core relative px-4 py-16 sm:px-6 lg:px-8 lg:py-24">
        <div className="mx-auto grid max-w-6xl items-center gap-12 lg:grid-cols-2">
          <div className="flex flex-col gap-6">
            <span className="inline-flex w-max items-center gap-2 rounded-full border border-[#2A2A3D] bg-[#1A1A24] px-3 py-1 font-mono text-xs uppercase tracking-wider text-[#00F0FF]">
              <span className="h-2 w-2 animate-pulse rounded-full bg-[#00F0FF] motion-reduce:animate-none" />
              System Online
            </span>
            <h1 className="font-display text-3xl font-bold tracking-tight text-[#E8E8F0] sm:text-4xl lg:text-5xl">
              Precision cleaning meets <span className="text-[#00F0FF]">cybernetic efficiency</span>.
            </h1>
            <p className="max-w-lg text-base leading-relaxed text-[#9090A8] sm:text-lg">
              Book your wash by chatting with Timah, our AI assistant. Real-time slot availability,
              cashless payment, and a no-show auto-release that keeps the schedule moving.
            </p>
            <div className="mt-2 flex flex-col gap-4 sm:flex-row">
              <Link
                to="/login"
                className="inline-flex min-h-[48px] items-center justify-center gap-2 bg-[#0D0D11] px-8 font-mono text-sm font-medium uppercase tracking-wider text-[#00F0FF] transition-colors hover:bg-[#1F1F2E] hex-border-active active:scale-95"
              >
                Book Now
                <ArrowRight className="h-4 w-4" />
              </Link>
              <Link
                to="/login"
                className="inline-flex min-h-[48px] items-center justify-center bg-[#1A1A24] px-8 font-mono text-sm font-medium uppercase tracking-wider text-[#E8E8F0] transition-colors hover:bg-[#1F1F2E] border border-[#2A2A3D] active:scale-95"
              >
                Sign In
              </Link>
            </div>
          </div>

          {/* Capacity panel — texture + focal icon, with real slot facts. */}
          <div className="hex-grid hex-border hex-corner relative h-[360px] w-full overflow-hidden rounded-lg sm:h-[400px]">
            <div className="absolute inset-0 flex items-center justify-center">
              <Car className="h-28 w-28 text-[#00F0FF]/80 sm:h-36 sm:w-36" strokeWidth={1} />
            </div>
            <div className="absolute inset-x-4 bottom-4 z-20 flex items-center justify-between border border-[#2A2A3D] bg-[#0D0D11]/80 p-4 backdrop-blur-md">
              <div>
                <p className="font-mono text-xs uppercase tracking-wider text-[#9090A8]">Booking Window</p>
                <p className="text-[#00F0FF]">14 days ahead</p>
              </div>
              <div className="text-right">
                <p className="font-mono text-xs uppercase tracking-wider text-[#9090A8]">Per 30-min Slot</p>
                <p className="font-bold text-[#E8E8F0]">3 vehicles</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Wash packages */}
      <section className="border-y border-[#1E1E2D] bg-[#151d1e] px-4 py-16 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-6xl">
          <div className="mb-12 text-center md:text-left">
            <h2 className="mb-2 font-display text-2xl font-semibold tracking-tight text-[#E8E8F0] sm:text-3xl">
              Wash Packages
            </h2>
            <p className="text-[#9090A8]">Pick the level of detailing your vehicle needs.</p>
          </div>
          <div className="grid gap-6 md:grid-cols-3">
            {SERVICES.map((s) => {
              const Icon = s.icon;
              return (
                <div
                  key={s.name}
                  className={`hex-corner relative flex h-full flex-col p-6 ${
                    s.recommended
                      ? 'hex-border bg-[#1A1A24]'
                      : 'border border-[#2A2A3D] bg-[#13131A] transition-colors hover:border-[#3b494b]'
                  }`}
                >
                  {s.recommended && (
                    <div className="absolute -top-3 right-6 bg-[#0D0D11] px-3 py-1 hex-border-active">
                      <span className="font-mono text-xs uppercase tracking-wider text-[#00F0FF]">
                        Most Popular
                      </span>
                    </div>
                  )}
                  <div className={`mb-6 flex items-start justify-between ${s.recommended ? 'mt-2' : ''}`}>
                    <Icon className="h-8 w-8" style={{ color: s.accent }} strokeWidth={1.5} />
                    <span className="font-mono text-xs uppercase tracking-wider text-[#9090A8]">{s.duration}</span>
                  </div>
                  <h3 className="mb-2 font-display text-xl font-semibold text-[#E8E8F0]">{s.name}</h3>
                  <p className="mb-6 flex-grow text-sm leading-relaxed text-[#9090A8]">{s.blurb}</p>
                  <div className="mb-6 space-y-2">
                    {s.features.map((f) => (
                      <div key={f} className="flex items-center gap-2">
                        <Check className="h-4 w-4 flex-shrink-0" style={{ color: s.accent }} />
                        <span className="text-sm text-[#9090A8]">{f}</span>
                      </div>
                    ))}
                  </div>
                  <div className="mt-auto flex items-end justify-between">
                    <p className="font-display text-xl font-semibold text-[#E8E8F0]">{s.price}</p>
                    <Link
                      to="/login"
                      className="inline-flex min-h-[44px] items-center font-mono text-sm uppercase tracking-wider transition-colors hover:text-[#00F0FF]"
                      style={{ color: s.accent }}
                    >
                      Book
                    </Link>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Driven by Technology */}
      <section className="hex-grid px-4 py-20 sm:px-6 lg:px-8">
        <div className="mx-auto grid max-w-6xl items-center gap-16 lg:grid-cols-2">
          <div className="relative order-2 mx-auto aspect-square w-full max-w-md border border-[#2A2A3D] bg-[#1A1A24] p-4 lg:order-1">
            <div className="flex h-full w-full items-center justify-center border border-[#1E1E2D] bg-[#0D0D11]">
              <Bot className="h-24 w-24 text-[#00E5A0]/70" strokeWidth={1} />
            </div>
            {/* Live capacity widget */}
            <div className="absolute -right-4 top-8 w-48 bg-[#0D0D11] p-4 shadow-lg hex-border md:-right-8">
              <p className="mb-1 font-mono text-xs uppercase tracking-wider text-[#9090A8]">Live Capacity</p>
              <p className="font-display text-2xl font-semibold text-[#00E5A0]">3 / slot</p>
              <p className="mt-1 font-mono text-xs text-[#5A5A72]">every 30 min · 14-day window</p>
            </div>
          </div>
          <div className="order-1 flex flex-col gap-8 lg:order-2">
            <div>
              <h2 className="mb-4 font-display text-2xl font-semibold tracking-tight text-[#E8E8F0] sm:text-3xl">
                Driven by Technology. Powered by Timah.
              </h2>
              <p className="text-base leading-relaxed text-[#9090A8] sm:text-lg">
                Timah is your car wash's AI receptionist. Tell her what you're driving and when you'd
                like to come in, and she'll get your slot booked — no app to download.
              </p>
            </div>
            <div className="grid gap-6 sm:grid-cols-2">
              {FEATURES.map((f) => {
                const Icon = f.icon;
                return (
                  <div key={f.title} className="flex flex-col gap-2">
                    <Icon className="h-6 w-6 text-[#00F0FF]" />
                    <h4 className="font-mono text-sm font-medium uppercase tracking-wider text-[#E8E8F0]">
                      {f.title}
                    </h4>
                    <p className="text-sm leading-relaxed text-[#9090A8]">{f.desc}</p>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </section>

      {/* Location / Contact */}
      <section className="hex-grid border-t border-[#1E1E2D] bg-[#151d1e] px-4 py-20 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-6xl">
          <div className="mb-12 text-center md:text-left">
            <h2 className="mb-2 font-display text-2xl font-semibold tracking-tight text-[#E8E8F0] sm:text-3xl">
              Find Us
            </h2>
            <p className="text-[#9090A8]">Drop by our branch or book ahead from your phone.</p>
          </div>
          <div className="grid items-center gap-12 lg:grid-cols-2">
            {/* Stylized map panel with the branch marker */}
            <div className="hex-grid-dense hex-border hex-corner relative h-[360px] w-full overflow-hidden rounded-lg sm:h-[400px]">
              <span className="absolute left-1/2 top-1/2 h-4 w-4 -translate-x-1/2 -translate-y-1/2 animate-pulse rounded-full bg-[#00F0FF] shadow-[0_0_15px_#00F0FF] motion-reduce:animate-none" />
              <span className="absolute left-1/2 top-1/2 -translate-x-1/2 translate-y-6 font-mono text-xs uppercase tracking-wider text-[#9090A8]">
                Timah Wash · Main
              </span>
            </div>

            {/* Contact terminal */}
            <div className="hex-corner border border-[#2A2A3D] bg-[#1A1A24] p-8">
              <div className="mb-8 flex items-center gap-3">
                <Terminal className="h-8 w-8 text-[#00F0FF]" />
                <h3 className="font-display text-xl font-semibold text-[#E8E8F0]">Contact</h3>
              </div>
              <div className="space-y-6">
                <div className="flex items-start gap-4">
                  <Building2 className="mt-1 h-5 w-5 flex-shrink-0 text-[#00B8C4]" />
                  <div>
                    <p className="mb-1 font-mono text-xs uppercase tracking-wider text-[#9090A8]">Branch</p>
                    <p className="text-[#E8E8F0]">Timah Wash - Main</p>
                  </div>
                </div>
                <div className="flex items-start gap-4">
                  <MapPin className="mt-1 h-5 w-5 flex-shrink-0 text-[#00B8C4]" />
                  <div>
                    <p className="mb-1 font-mono text-xs uppercase tracking-wider text-[#9090A8]">Location</p>
                    <p className="text-[#E8E8F0]">Jalan Utama, Kuala Lumpur</p>
                  </div>
                </div>
                <div className="flex items-start gap-4">
                  <Clock className="mt-1 h-5 w-5 flex-shrink-0 text-[#00B8C4]" />
                  <div>
                    <p className="mb-1 font-mono text-xs uppercase tracking-wider text-[#9090A8]">Operating Hours</p>
                    <p className="text-[#E8E8F0]">Mon–Thu · 09:00–17:30 · Closed Fridays</p>
                  </div>
                </div>
              </div>
              <Link
                to="/login"
                className="mt-10 inline-flex min-h-[48px] w-full items-center justify-center gap-2 bg-[#0D0D11] font-mono text-sm font-medium uppercase tracking-wider text-[#00F0FF] transition-colors hover:bg-[#1F1F2E] hex-border-active active:scale-95"
              >
                Book a Wash
                <Zap className="h-4 w-4" />
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};
