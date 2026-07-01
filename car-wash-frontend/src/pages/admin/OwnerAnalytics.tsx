import React, { useCallback, useEffect, useState } from 'react';
import axios from 'axios';

interface SalesSummary {
  date: string;
  totalRevenue: number;
  cashRevenue: number;
  onlineRevenue: number;
  completedPayments: number;
  pendingPayments: number;
  failedPayments: number;
  totalBookings: number;
  completedBookings: number;
  confirmedBookings: number;
  pendingBookings: number;
  cancelledBookings: number;
  noShowBookings: number;
}

interface TrendPoint {
  date: string;
  revenue: number;
  bookingCount: number;
}

const todayStr = (): string => new Date().toISOString().slice(0, 10);

const fmtMyr = (v: number): string =>
  `RM ${Number(v).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const shortDay = (iso: string): string => {
  const d = new Date(iso + 'T00:00:00');
  return d.toLocaleDateString('en-MY', { weekday: 'short', day: 'numeric' });
};

// ─── KPI Card ─────────────────────────────────────────────────

interface KpiCardProps {
  label: string;
  value: string;
  sub?: string;
  accent?: boolean;
}

const KpiCard: React.FC<KpiCardProps> = ({ label, value, sub, accent }) => (
  <div className={`relative hex-corner overflow-hidden rounded-2xl p-5 ${
    accent ? 'hex-radial-core hex-border-active' : 'hex-grid hex-border'
  }`}>
    <p className="text-xs font-medium uppercase tracking-widest text-muted">{label}</p>
    <p className={`mt-1.5 font-display text-2xl font-bold tabular-nums tracking-tight ${
      accent ? 'text-cyan' : 'text-primary'
    }`}>
      {value}
    </p>
    {sub && <p className="mt-1 text-xs text-secondary">{sub}</p>}
  </div>
);

// ─── Booking Status Breakdown ─────────────────────────────────

interface StatusRow { label: string; count: number; color: string; }

const StatusBreakdown: React.FC<{ rows: StatusRow[]; total: number }> = ({ rows, total }) => (
  <div className="hex-grid hex-border hex-corner rounded-2xl p-5 overflow-hidden">
    <h3 className="mb-4 text-xs font-medium uppercase tracking-widest text-muted">
      Booking Status
    </h3>
    <div className="space-y-3">
      {rows.map((r) => {
        const pct = total > 0 ? Math.round((r.count / total) * 100) : 0;
        return (
          <div key={r.label}>
            <div className="mb-1 flex items-center justify-between text-xs">
              <span className="text-secondary">{r.label}</span>
              <span className="font-mono text-muted">
                {r.count} <span className="opacity-50">({pct}%)</span>
              </span>
            </div>
            <div className="h-1.5 w-full overflow-hidden rounded-full bg-surface-raised">
              <div
                className="h-full rounded-full transition-all duration-500"
                style={{ width: `${pct}%`, backgroundColor: r.color }}
              />
            </div>
          </div>
        );
      })}
    </div>
  </div>
);

// ─── SVG Bar Chart ────────────────────────────────────────────

const CHART_W = 560;
const CHART_H = 200;
const PAD_L   = 56;
const PAD_R   = 16;
const PAD_T   = 16;
const PAD_B   = 36;
const PLOT_W  = CHART_W - PAD_L - PAD_R;
const PLOT_H  = CHART_H - PAD_T - PAD_B;

const RevenueBarChart: React.FC<{
  data: TrendPoint[];
  days: number;
  onDaysChange: (d: number) => void;
}> = ({ data, days, onDaysChange }) => {
  const [hoveredIdx, setHoveredIdx] = useState<number | null>(null);

  if (data.length === 0) {
    return (
      <div className="flex h-48 items-center justify-center text-sm text-muted">
        No data yet
      </div>
    );
  }

  const n         = data.length;
  const maxRev    = Math.max(...data.map((d) => d.revenue), 1);
  const magnitude = Math.pow(10, Math.floor(Math.log10(maxRev)));
  const ceiling   = Math.ceil(maxRev / magnitude) * magnitude;

  const barGap = 6;
  const barW   = Math.max(4, PLOT_W / n - barGap);
  const slotW  = PLOT_W / n;

  const yTicks = [0, 0.25, 0.5, 0.75, 1].map((f) => ceiling * f);

  const barX = (i: number) => PAD_L + i * slotW + slotW / 2 - barW / 2;
  const barY = (rev: number) => PAD_T + PLOT_H - (rev / ceiling) * PLOT_H;
  const barH = (rev: number) => (rev / ceiling) * PLOT_H;

  const hovered = hoveredIdx !== null ? data[hoveredIdx] : null;

  return (
    <div className="space-y-3">
      {/* Range selector */}
      <div className="flex items-center gap-2">
        {[7, 14, 30].map((d) => (
          <button
            key={d}
            type="button"
            onClick={() => onDaysChange(d)}
            className={`min-h-[44px] rounded-lg px-3 py-1 text-xs font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan ${
              days === d
                ? 'bg-cyan/20 text-cyan border border-cyan/40'
                : 'text-secondary hover:text-primary border border-transparent'
            }`}
          >
            {d}D
          </button>
        ))}
        <span className="ml-auto text-xs text-muted">Revenue (MYR)</span>
      </div>

      {/* SVG chart */}
      <div className="relative w-full overflow-x-auto">
        <svg
          viewBox={`0 0 ${CHART_W} ${CHART_H}`}
          role="img"
          aria-label="Revenue bar chart"
          className="w-full"
          style={{ minWidth: Math.max(320, n * 20) }}
          onMouseLeave={() => setHoveredIdx(null)}
        >
          {yTicks.map((tick) => {
            const y = PAD_T + PLOT_H - (tick / ceiling) * PLOT_H;
            return (
              <g key={tick}>
                <line
                  x1={PAD_L} y1={y} x2={CHART_W - PAD_R} y2={y}
                  stroke="rgba(42,42,61,0.8)" strokeWidth={1}
                />
                <text
                  x={PAD_L - 6} y={y + 4}
                  textAnchor="end" fontSize={9}
                  fill="#5A5A72"
                  fontFamily="JetBrains Mono, monospace"
                >
                  {tick >= 1000 ? `${(tick / 1000).toFixed(1)}k` : tick.toFixed(0)}
                </text>
              </g>
            );
          })}

          {data.map((point, i) => {
            const x     = barX(i);
            const h     = Math.max(barH(point.revenue), point.revenue > 0 ? 2 : 0);
            const y     = barY(point.revenue);
            const isHov = hoveredIdx === i;

            return (
              <g key={point.date} onMouseEnter={() => setHoveredIdx(i)} style={{ cursor: 'pointer' }}>
                <rect
                  x={PAD_L + i * slotW} y={PAD_T}
                  width={slotW} height={PLOT_H}
                  fill="transparent"
                />
                <rect
                  x={x} y={y} width={barW} height={h} rx={3}
                  fill={isHov ? '#00F0FF' : 'rgba(0,240,255,0.35)'}
                  style={{ transition: 'fill 0.15s' }}
                />
              </g>
            );
          })}

          {data.map((point, i) => {
            const step = n <= 7 ? 1 : n <= 14 ? 2 : 5;
            if (i % step !== 0 && i !== n - 1) return null;
            return (
              <text
                key={`lbl-${point.date}`}
                x={PAD_L + i * slotW + slotW / 2}
                y={CHART_H - 8}
                textAnchor="middle" fontSize={9}
                fill="#5A5A72"
                fontFamily="Inter, sans-serif"
              >
                {shortDay(point.date)}
              </text>
            );
          })}
        </svg>

        {hovered && (
          <div
            aria-live="polite"
            className="pointer-events-none absolute right-0 top-0 hex-grid-subtle hex-border rounded-xl px-3 py-2 text-xs"
          >
            <p className="font-medium text-primary">{shortDay(hovered.date)}</p>
            <p className="mt-0.5 font-mono text-cyan">{fmtMyr(hovered.revenue)}</p>
            <p className="text-secondary">{hovered.bookingCount} bookings</p>
          </div>
        )}
      </div>
    </div>
  );
};

// ─── Skeleton ─────────────────────────────────────────────────

const Skeleton: React.FC<{ className?: string }> = ({ className = '' }) => (
  <div className={`animate-pulse rounded-lg bg-surface-raised ${className}`} />
);

// ─── Main Page ────────────────────────────────────────────────

export const OwnerAnalytics: React.FC = () => {
  const [date, setDate]         = useState<string>(todayStr());
  const [days, setDays]         = useState<number>(7);
  const [summary, setSummary]   = useState<SalesSummary | null>(null);
  const [trend, setTrend]       = useState<TrendPoint[]>([]);
  const [loadingS, setLoadingS] = useState(true);
  const [loadingT, setLoadingT] = useState(true);
  const [errorS, setErrorS]     = useState<string | null>(null);
  const [errorT, setErrorT]     = useState<string | null>(null);

  const fetchSummary = useCallback(async (d: string) => {
    setLoadingS(true); setErrorS(null);
    try {
      const res = await axios.get<SalesSummary>('/api/v1/owner/analytics/summary', { params: { date: d } });
      setSummary(res.data);
    } catch { setErrorS('Failed to load daily summary.'); }
    finally { setLoadingS(false); }
  }, []);

  const fetchTrend = useCallback(async (n: number) => {
    setLoadingT(true); setErrorT(null);
    try {
      const res = await axios.get<TrendPoint[]>('/api/v1/owner/analytics/trend', { params: { days: n } });
      setTrend(res.data);
    } catch { setErrorT('Failed to load trend data.'); }
    finally { setLoadingT(false); }
  }, []);

  useEffect(() => { fetchSummary(date); }, [date, fetchSummary]);
  useEffect(() => { fetchTrend(days);   }, [days, fetchTrend]);

  const statusRows = summary ? [
    { label: 'Completed',  count: summary.completedBookings,  color: '#00E5A0' },
    { label: 'Confirmed',  count: summary.confirmedBookings,  color: '#00F0FF' },
    { label: 'Pending',    count: summary.pendingBookings,    color: '#FFB800' },
    { label: 'Cancelled',  count: summary.cancelledBookings,  color: '#FF4466' },
    { label: 'No-show',    count: summary.noShowBookings,     color: '#5A5A72' },
  ] : [];

  return (
    <div className="mx-auto max-w-6xl space-y-8 px-4 py-8 sm:px-6 lg:px-8">

      {/* Page header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="font-display text-2xl font-bold text-primary">Analytics</h1>
          <p className="mt-1 text-sm text-secondary">Revenue and booking performance</p>
        </div>
        <div className="flex items-center gap-2">
          <label htmlFor="analytics-date" className="sr-only">Select date</label>
          <input
            id="analytics-date"
            type="date"
            value={date}
            max={todayStr()}
            onChange={(e) => setDate(e.target.value)}
            className="min-h-[44px] rounded-lg border border-border bg-surface px-3 py-2 text-sm text-primary transition-all duration-150 focus:outline-none focus-visible:border-cyan focus-visible:shadow-cyan-glow"
            style={{ colorScheme: 'dark' }}
          />
          <button
            type="button"
            onClick={() => setDate(todayStr())}
            className="min-h-[44px] rounded-lg border border-border bg-surface px-3 py-2 text-xs text-secondary transition-colors hover:border-slate hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate"
          >
            Today
          </button>
        </div>
      </div>

      {errorS && (
        <div role="alert" className="hex-border-danger rounded-xl px-4 py-3 text-sm text-danger" style={{ backgroundColor: 'rgba(255,68,102,0.08)' }}>
          {errorS}
        </div>
      )}

      {/* KPI cards */}
      <section aria-label="Key performance indicators">
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          {loadingS ? (
            Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-28" />)
          ) : summary ? (
            <>
              <KpiCard accent label="Total Revenue"  value={fmtMyr(summary.totalRevenue)}   sub={`${summary.completedPayments} paid`} />
              <KpiCard        label="Total Bookings" value={String(summary.totalBookings)}   sub={`${summary.completedBookings} completed`} />
              <KpiCard        label="Cash"           value={fmtMyr(summary.cashRevenue)}     sub="walk-in payments" />
              <KpiCard        label="Online (FPX)"   value={fmtMyr(summary.onlineRevenue)}   sub={`${summary.pendingPayments} pending · ${summary.failedPayments} failed`} />
            </>
          ) : null}
        </div>
      </section>

      {/* Trend chart + status breakdown */}
      <section aria-label="Revenue trend and booking breakdown">
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">

          {/* Chart */}
          <div className="hex-grid hex-border hex-corner rounded-2xl p-5 overflow-hidden lg:col-span-2">
            <h3 className="mb-4 text-xs font-medium uppercase tracking-widest text-muted">
              Revenue Trend
            </h3>
            {loadingT ? (
              <div className="space-y-2 animate-pulse" aria-busy="true" aria-label="Loading trend">
                <Skeleton className="h-40 w-full" />
                <Skeleton className="h-3 w-1/2" />
              </div>
            ) : errorT ? (
              <p role="alert" className="text-sm text-danger">{errorT}</p>
            ) : (
              <RevenueBarChart data={trend} days={days} onDaysChange={setDays} />
            )}
          </div>

          {/* Status breakdown */}
          <div>
            {loadingS ? (
              <div className="hex-grid hex-border rounded-2xl p-5 space-y-3 animate-pulse" aria-busy="true">
                <Skeleton className="h-4 w-1/2" />
                {[1,2,3,4,5].map((k) => <Skeleton key={k} className="h-6 w-full" />)}
              </div>
            ) : summary ? (
              <StatusBreakdown rows={statusRows} total={summary.totalBookings} />
            ) : null}
          </div>
        </div>
      </section>

      {/* Payment method split */}
      {!loadingS && summary && (
        <section aria-label="Payment method split">
          <div className="hex-grid hex-border hex-corner rounded-2xl p-5 overflow-hidden">
            <h3 className="mb-4 text-xs font-medium uppercase tracking-widest text-muted">
              Payment Method Split
            </h3>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              {[
                { label: 'Cash',       value: summary.cashRevenue,   color: '#00E5A0' },
                { label: 'Online FPX', value: summary.onlineRevenue, color: '#00F0FF' },
              ].map(({ label, value, color }) => {
                const pct = summary.totalRevenue > 0
                  ? Math.round((value / summary.totalRevenue) * 100)
                  : 0;
                return (
                  <div key={label} className="flex items-center gap-4">
                    <div
                      className="h-10 w-10 flex-shrink-0 rounded-full"
                      style={{ backgroundColor: `${color}22`, border: `2px solid ${color}66` }}
                      aria-hidden="true"
                    />
                    <div className="flex-1">
                      <div className="flex items-baseline justify-between">
                        <span className="text-sm text-secondary">{label}</span>
                        <span className="font-mono text-sm text-muted">{pct}%</span>
                      </div>
                      <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-surface-raised">
                        <div
                          className="h-full rounded-full transition-all duration-700"
                          style={{ width: `${pct}%`, backgroundColor: color }}
                        />
                      </div>
                      <p className="mt-0.5 font-mono text-xs" style={{ color }}>
                        {fmtMyr(value)}
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </section>
      )}
    </div>
  );
};
