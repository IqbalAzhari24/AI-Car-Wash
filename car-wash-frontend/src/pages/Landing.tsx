import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Droplets, Sparkles, Car, ShieldCheck, MapPin, Phone, Mail, Clock } from 'lucide-react';

const SERVICES = [
  {
    icon: Car,
    title: 'Standard Wash',
    benefit: 'Back on the road in 30 minutes',
    description: 'Full exterior wash and dry. Clean car, no hassle, no waiting around.',
    tag: 'Most Popular',
  },
  {
    icon: Droplets,
    title: 'Premium Wash',
    benefit: 'Clean outside and in',
    description: 'Everything in Standard plus interior vacuum and wipe-down. Step into a fresh cabin every time.',
  },
  {
    icon: Sparkles,
    title: 'Full Detailing',
    benefit: 'Showroom condition, guaranteed',
    description: 'Deep clean, machine polish, and protective wax coat. For when your car deserves the works.',
  },
  {
    icon: ShieldCheck,
    title: 'Valet Pick-Up',
    benefit: 'We come to you',
    description: 'Our driver collects your car, gets it washed, and returns it. You don\'t move a muscle.',
  },
] as const;

const TIERS = [
  {
    name: 'Basic',
    price: 'RM 25',
    duration: '~30 min',
    highlight: false,
    cta: 'Book Basic',
    features: [
      'Full exterior wash',
      'Hand dry',
      'Wheel rinse',
      'Window clean (exterior)',
    ],
  },
  {
    name: 'Standard',
    price: 'RM 45',
    duration: '~45 min',
    highlight: true,
    cta: 'Book Standard',
    features: [
      'Everything in Basic',
      'Interior vacuum',
      'Dashboard & console wipe',
      'Air freshener',
      'Tyre shine',
    ],
  },
  {
    name: 'Premium',
    price: 'RM 120',
    duration: '~90 min',
    highlight: false,
    cta: 'Book Premium',
    features: [
      'Everything in Standard',
      'Machine polish',
      'Protective wax coat',
      'Leather conditioning',
      'Engine bay rinse',
      'Priority slot',
    ],
  },
] as const;

export const Landing: React.FC = () => {
  return (
    <div className="flex flex-1 flex-col bg-base">

      {/* ── 1. HERO ─────────────────────────────────────────────── */}
      <section className="hex-grid-dense">
        <div className="mx-auto w-full max-w-7xl px-4 py-16 sm:px-6 sm:py-28 lg:px-8">
          <div className="grid items-center gap-12 lg:grid-cols-2 lg:gap-16">
            <div className="max-w-xl">
              <p className="mb-3 text-xs font-medium uppercase tracking-widest text-cyan">
                AI-Powered Car Wash · Kuala Terengganu
              </p>
              <h1 className="font-display text-3xl font-bold tracking-tight text-primary sm:text-4xl lg:text-5xl leading-tight">
                Your car, cleaner.{' '}
                <span className="text-cyan">Booked in seconds.</span>
              </h1>
              <p className="mt-5 text-base leading-relaxed text-secondary sm:text-lg">
                AICarWash uses Timah — our AI receptionist — to book your slot, track your wash,
                and handle everything in between. No calls, no queues, no hassle.
              </p>
              <div className="mt-8 flex flex-wrap items-center gap-4">
                <Link
                  to="/login"
                  className="inline-flex min-h-[44px] items-center gap-2 rounded-lg bg-cyan px-6 py-2.5 text-sm font-semibold text-base shadow-cyan-glow transition-all duration-150 hover:brightness-110 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan focus-visible:ring-offset-2 focus-visible:ring-offset-base"
                >
                  Book a wash
                  <ArrowRight className="h-4 w-4" aria-hidden="true" />
                </Link>
                <a
                  href="#pricing"
                  className="inline-flex min-h-[44px] items-center gap-2 rounded-lg border border-border px-6 py-2.5 text-sm font-medium text-secondary transition-all duration-150 hover:border-cyan hover:text-cyan active:scale-[0.98]"
                >
                  See pricing
                </a>
              </div>
            </div>

            {/* Illustration */}
            <div className="flex justify-center lg:justify-end" aria-hidden="true">
              <div className="hex-grid hex-border hex-corner relative flex h-64 w-64 items-center justify-center rounded-3xl sm:h-80 sm:w-80">
                <svg viewBox="0 0 120 80" className="w-40 sm:w-52 text-cyan/20 fill-current" aria-hidden="true">
                  <ellipse cx="60" cy="55" rx="55" ry="16" />
                  <rect x="10" y="30" width="100" height="30" rx="12" fill="none" stroke="#00F0FF" strokeOpacity="0.25" strokeWidth="1.5" />
                  <rect x="20" y="18" width="80" height="22" rx="8" fill="none" stroke="#00F0FF" strokeOpacity="0.15" strokeWidth="1" />
                  <circle cx="26" cy="60" r="8" fill="none" stroke="#00F0FF" strokeOpacity="0.35" strokeWidth="1.5" />
                  <circle cx="94" cy="60" r="8" fill="none" stroke="#00F0FF" strokeOpacity="0.35" strokeWidth="1.5" />
                </svg>
                <div className="absolute right-8 top-8 h-3 w-3 rounded-full bg-cyan shadow-cyan-glow animate-pulse" />
                <div className="absolute bottom-10 left-8 h-2 w-2 rounded-full bg-cyan/60" />
                <div className="absolute top-16 left-12 h-2 w-2 rounded-full bg-cyan/40" />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── 2. SERVICES ─────────────────────────────────────────── */}
      <section id="services" className="border-t border-border-subtle">
        <div className="mx-auto w-full max-w-7xl px-4 py-14 sm:px-6 sm:py-20 lg:px-8">
          <div className="max-w-xl">
            <p className="mb-2 text-xs font-medium uppercase tracking-widest text-cyan">What we offer</p>
            <h2 className="font-display text-2xl font-bold text-primary sm:text-3xl">
              Services built around your day
            </h2>
            <p className="mt-3 text-sm leading-relaxed text-secondary sm:text-base">
              Whether you need a quick rinse or a full detail, we have a package that fits your schedule and budget.
            </p>
          </div>

          <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {SERVICES.map((svc) => {
              const Icon = svc.icon;
              return (
                <div
                  key={svc.title}
                  className="relative hex-grid hex-border hex-corner rounded-xl p-5 overflow-hidden flex flex-col gap-3"
                >
                  {'tag' in svc && (
                    <span className="absolute right-4 top-4 rounded-full bg-cyan/10 border border-cyan/30 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-cyan">
                      {svc.tag}
                    </span>
                  )}
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-cyan/10 border border-cyan/30">
                    <Icon className="h-5 w-5 text-cyan" aria-hidden="true" />
                  </div>
                  <div>
                    <h3 className="text-base font-semibold text-primary">{svc.title}</h3>
                    <p className="mt-0.5 text-xs font-medium text-cyan">{svc.benefit}</p>
                  </div>
                  <p className="text-sm leading-relaxed text-secondary">{svc.description}</p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* ── 3. PRICING ──────────────────────────────────────────── */}
      <section id="pricing" className="border-t border-border-subtle">
        <div className="mx-auto w-full max-w-7xl px-4 py-14 sm:px-6 sm:py-20 lg:px-8">
          <div className="max-w-xl">
            <p className="mb-2 text-xs font-medium uppercase tracking-widest text-cyan">Transparent pricing</p>
            <h2 className="font-display text-2xl font-bold text-primary sm:text-3xl">
              No hidden charges. Ever.
            </h2>
            <p className="mt-3 text-sm leading-relaxed text-secondary sm:text-base">
              Pick the tier that suits your car and your budget. Pay after the wash via FPX — no upfront deposit.
            </p>
          </div>

          <div className="mt-10 grid gap-5 sm:grid-cols-3">
            {TIERS.map((tier) => (
              <div
                key={tier.name}
                className={`relative flex flex-col rounded-xl p-6 overflow-hidden ${
                  tier.highlight
                    ? 'border border-cyan shadow-cyan-glow bg-cyan/5'
                    : 'hex-grid hex-border hex-corner'
                }`}
              >
                {tier.highlight && (
                  <span className="absolute right-4 top-4 rounded-full bg-cyan px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide text-base">
                    Best Value
                  </span>
                )}
                <p className="text-xs font-semibold uppercase tracking-widest text-secondary">{tier.name}</p>
                <div className="mt-2 flex items-end gap-1">
                  <span className="font-display text-3xl font-bold text-primary">{tier.price}</span>
                  <span className="mb-1 text-xs text-muted">/ wash</span>
                </div>
                <p className="mt-1 text-xs text-muted">{tier.duration}</p>

                <ul className="mt-5 flex flex-col gap-2 flex-1">
                  {tier.features.map((f) => (
                    <li key={f} className="flex items-start gap-2 text-sm text-secondary">
                      <span className="mt-0.5 h-4 w-4 flex-shrink-0 text-cyan">✓</span>
                      {f}
                    </li>
                  ))}
                </ul>

                <Link
                  to="/login"
                  className={`mt-6 inline-flex min-h-[40px] items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-all duration-150 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan focus-visible:ring-offset-2 focus-visible:ring-offset-base ${
                    tier.highlight
                      ? 'bg-cyan text-base hover:brightness-110'
                      : 'border border-border text-secondary hover:border-cyan hover:text-cyan'
                  }`}
                >
                  {tier.cta}
                  <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
                </Link>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── 4. LOCATION & CONTACT ───────────────────────────────── */}
      <section id="contact" className="border-t border-border-subtle">
        <div className="mx-auto w-full max-w-7xl px-4 py-14 sm:px-6 sm:py-20 lg:px-8">
          <div className="max-w-xl">
            <p className="mb-2 text-xs font-medium uppercase tracking-widest text-cyan">Find us</p>
            <h2 className="font-display text-2xl font-bold text-primary sm:text-3xl">
              We're easy to find — even easier to book.
            </h2>
            <p className="mt-3 text-sm leading-relaxed text-secondary sm:text-base">
              Walk in or book ahead with Timah. Either way, we're ready when you pull up.
            </p>
          </div>

          <div className="mt-10 grid gap-6 lg:grid-cols-2">
            {/* Map placeholder */}
            <div className="hex-grid hex-border hex-corner flex h-64 items-center justify-center rounded-xl overflow-hidden lg:h-auto">
              <div className="flex flex-col items-center gap-3 text-center px-6">
               <iframe src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d993.0059926782855!2d103.06959629095037!3d5.413319652869992!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x31b7bb000993c973%3A0xffa0d942be402c3e!2sVanduta%20Carwash!5e0!3m2!1sen!2smy!4v1782930229894!5m2!1sen!2smy"
                 width="100%"
                 height="100%"
                 style={{ border: 0 }}
                 allowFullScreen
                 loading="lazy"
                 referrerPolicy="strict-origin-when-cross-origin"
                 title="AICarWash location map"
                 ></iframe>
              </div>
            </div>

            {/* Contact details */}
            <div className="flex flex-col gap-5">
              <div className="hex-grid hex-border hex-corner rounded-xl p-5">
                <p className="mb-3 text-xs font-semibold uppercase tracking-widest text-cyan">Contact</p>
                <ul className="flex flex-col gap-3">
                  <li className="flex items-center gap-3 text-sm text-secondary">
                    <MapPin className="h-4 w-4 flex-shrink-0 text-cyan/60" aria-hidden="true" />
                    PT30125 A, Kampung Gong Pa' Jin, Kampung Wakaf Tengah, 21030 Kuala Terengganu, Terengganu
                  </li>
                  <li className="flex items-center gap-3 text-sm text-secondary">
                    <Phone className="h-4 w-4 flex-shrink-0 text-cyan/60" aria-hidden="true" />
                    +6016 922 0499
                  </li>
                  <li className="flex items-center gap-3 text-sm text-secondary">
                    <Mail className="h-4 w-4 flex-shrink-0 text-cyan/60" aria-hidden="true" />
                    hello@aicarwash.my
                  </li>
                </ul>
              </div>

              <div className="hex-grid hex-border hex-corner rounded-xl p-5">
                <p className="mb-3 text-xs font-semibold uppercase tracking-widest text-cyan">Operating Hours</p>
                <ul className="flex flex-col gap-2 text-sm">
                  <li className="flex justify-between text-secondary">
                    <span className="flex items-center gap-2">
                      <Clock className="h-3.5 w-3.5 text-cyan/60" aria-hidden="true" />
                      Monday – Thursday
                    </span>
                    <span className="font-medium text-primary">8:00 AM – 6:00 PM</span>
                  </li>
                  <li className="flex justify-between text-secondary">
                    <span className="flex items-center gap-2">
                      <Clock className="h-3.5 w-3.5 text-cyan/60" aria-hidden="true" />
                      Friday
                    </span>
                    <span className="font-medium text-primary">8:00 AM – 12:00 PM, 2:30 PM – 6:00 PM</span>
                  </li>
                  <li className="flex justify-between text-secondary">
                    <span className="flex items-center gap-2">
                      <Clock className="h-3.5 w-3.5 text-cyan/60" aria-hidden="true" />
                      Saturday
                    </span>
                    <span className="font-medium text-primary">8:00 AM – 4:00 PM</span>
                  </li>
                  <li className="flex justify-between text-muted">
                    <span className="flex items-center gap-2">
                      <Clock className="h-3.5 w-3.5 text-muted/40" aria-hidden="true" />
                      Sunday & Public Holidays
                    </span>
                    <span className="font-medium">Closed</span>
                  </li>
                </ul>
              </div>

              <Link
                to="/login"
                className="inline-flex min-h-[44px] items-center justify-center gap-2 rounded-lg bg-cyan px-6 py-2.5 text-sm font-semibold text-base shadow-cyan-glow transition-all duration-150 hover:brightness-110 active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan focus-visible:ring-offset-2 focus-visible:ring-offset-base"
              >
                Book your slot now
                <ArrowRight className="h-4 w-4" aria-hidden="true" />
              </Link>
            </div>
          </div>
        </div>
      </section>

    </div>
  );
};
