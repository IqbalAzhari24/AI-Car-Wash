import React from 'react';
import { Link } from 'react-router-dom';
import { Car, Droplet, Sparkles, MessageCircle, CalendarCheck, Wallet, ArrowRight } from 'lucide-react';

const STEPS = [
  {
    icon: MessageCircle,
    title: 'Tell Timah what you need',
    description:
      "Open the chat and say what you're bringing in and when you'd like to come. No forms, no menus.",
  },
  {
    icon: CalendarCheck,
    title: 'Get your slot confirmed',
    description: 'Timah checks availability and locks in your time on the spot — done in under a minute.',
  },
  {
    icon: Wallet,
    title: 'Pull up and pay',
    description: 'Show up at your time, get washed, and settle up in RM. That’s the whole visit.',
  },
] as const;

export const Landing: React.FC = () => {
  return (
    <div className="flex flex-1 flex-col">
      {/* Hero */}
      <section className="mx-auto w-full max-w-7xl px-4 py-12 sm:px-6 sm:py-16 lg:px-8 lg:py-24">
        <div className="grid items-center gap-12 lg:grid-cols-2 lg:gap-16">
          <div className="max-w-xl">
            <h1 className="text-3xl font-semibold tracking-tight text-ink sm:text-4xl lg:text-5xl" style={{ textWrap: 'balance' }}>
              Book your wash by chatting with Timah
            </h1>
            <p className="mt-4 text-base leading-relaxed text-muted sm:text-lg">
              Timah is your car wash's AI receptionist. Tell her what you're driving and when
              you'd like to come in, and she'll get your slot booked — right from your phone,
              no app to download.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-3">
              <Link
                to="/login"
                className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-3 text-sm font-medium text-white transition-colors duration-150 hover:bg-primary-strong focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
              >
                Sign in
                <ArrowRight className="h-4 w-4" />
              </Link>
              <p className="text-sm text-muted">
                Already a regular? Your shop sets you up with an account.
              </p>
            </div>
          </div>

          {/* Illustration */}
          <div className="flex justify-center lg:justify-end">
            <div
              className="relative flex h-64 w-64 items-center justify-center rounded-3xl bg-primary-soft sm:h-80 sm:w-80"
              aria-hidden="true"
            >
              <Car className="h-24 w-24 text-primary-soft-ink sm:h-32 sm:w-32" strokeWidth={1.5} />
              <Droplet
                className="absolute right-9 top-9 h-9 w-9 text-accent sm:right-12 sm:top-12 sm:h-11 sm:w-11"
                fill="currentColor"
                strokeWidth={1}
              />
              <Droplet
                className="absolute bottom-12 left-10 h-6 w-6 text-accent-soft-ink/70 sm:bottom-16 sm:left-14 sm:h-7 sm:w-7"
                fill="currentColor"
                strokeWidth={1}
              />
              <Sparkles className="absolute left-10 top-14 h-5 w-5 text-primary sm:left-14 sm:top-16 sm:h-6 sm:w-6" />
            </div>
          </div>
        </div>
      </section>

      {/* How it works */}
      <section className="border-t border-border bg-surface">
        <div className="mx-auto w-full max-w-7xl px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
          <h2 className="text-xl font-semibold tracking-tight text-ink sm:text-2xl">How it works</h2>
          <div className="mt-8 grid gap-10 sm:grid-cols-3 sm:gap-8">
            {STEPS.map((step, index) => {
              const Icon = step.icon;
              return (
                <div key={step.title} className="flex flex-col gap-3">
                  <div className="flex items-center gap-3">
                    <span className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-primary text-sm font-semibold text-white">
                      {index + 1}
                    </span>
                    <Icon className="h-5 w-5 text-muted" />
                  </div>
                  <h3 className="text-base font-semibold text-ink">{step.title}</h3>
                  <p className="text-sm leading-relaxed text-muted" style={{ textWrap: 'pretty' }}>
                    {step.description}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* For staff */}
      <section className="mx-auto w-full max-w-7xl px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
        <div className="grid items-start gap-8 lg:grid-cols-2 lg:gap-16">
          <div>
            <h2 className="text-xl font-semibold tracking-tight text-ink sm:text-2xl">
              Running the shop?
            </h2>
            <p className="mt-3 max-w-md text-sm leading-relaxed text-muted sm:text-base">
              Owners, clerks, and workers sign in from the same page. Owners get a directory of
              customers and staff plus each customer's transaction history; clerks and workers
              get the dashboard for the day's bookings.
            </p>
          </div>
          <div className="rounded-2xl border border-border bg-surface p-6">
            <p className="text-sm font-medium text-ink">One account, one sign-in</p>
            <p className="mt-1.5 text-sm leading-relaxed text-muted">
              Whether you're booking a wash or managing the floor, everything lives behind the
              same sign-in — your role decides what you see next.
            </p>
          </div>
        </div>
      </section>

      {/* Final CTA */}
      <section className="border-t border-border">
        <div className="mx-auto w-full max-w-7xl px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
          <div className="flex flex-col items-start justify-between gap-6 rounded-2xl bg-primary-soft px-6 py-10 sm:flex-row sm:items-center sm:px-10">
            <div>
              <h2 className="text-xl font-semibold tracking-tight text-primary-soft-ink sm:text-2xl">
                Ready when you are
              </h2>
              <p className="mt-2 max-w-md text-sm leading-relaxed text-primary-soft-ink/80">
                Sign in and Timah will take it from there.
              </p>
            </div>
            <Link
              to="/login"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-3 text-sm font-medium text-white transition-colors duration-150 hover:bg-primary-strong focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
            >
              Sign in
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
};
