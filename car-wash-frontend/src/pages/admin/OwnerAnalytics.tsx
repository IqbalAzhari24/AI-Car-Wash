import React, { useCallback, useEffect, useState } from 'react';
import axios from 'axios';

// ─── Types ───────────────────────────────────────────────────────────────────

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
  date: string;          // YYYY-MM-DD
  revenue: number;
  bookingCount: number;
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

const todayStr = (): string => new Date().toISOString().slice(0, 10);

const fmtMyr = (v: number): string =>
  `RM ${Number(v).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

/** "2024-06-19" → "Wed 19" */
const shortDay = (iso: string): string => {
  const d = new Date(iso + 'T00:00:00');
  return d.toLocaleDateString('en-MY', { weekday: 'short', day: 'numeric' });
};

// ─── KPI Card ─────────────────────────────────────────────────────────────────

interface KpiCardProps {
  label: string;
  value: string;
  sub?: string;
  accent?: boolean;
}

const KpiCard: React.FC<KpiCardProps> = ({ label, value, sub, accent }) => (
  <div
    className={`relative overflow-hidden rounded-2xl border p-5 ${
      accent
        ? 'border-cyan-500/40 bg-cyan-950/30'
        : 'border-white/10 bg-white/5'
    }`}
  >
    {/* subtle hex grid texture */}
    <div
      aria-hidden="true"
      className="pointer-events-none absolute inset-0 opacity-[0.04]"
      style={{
        backgroundImage:
          'url("data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' width=\'28\' height=\'49\'%3E%3Cpath d=\'M14 0l14 8v16l-14 8L0 24V8z\' fill=\'none\' stroke=\'%2300F0FF\' stroke-width=\'1\'/%3E%3C/svg%3E")',
        backgroundSize: '28px 49px',
      }}
    />
    <p className="text-xs font-medium uppercase tracking-widest text-white/40">{label}</p>
    <p
      className={`mt-1.5 font-mono text-2xl font-semibold tabular-nums tracking-tight ${
        accent ? 'text-cyan-300' : 'text-white'
      }`}
    >
      {value}
    </p>
    {sub && <p className="mt-1 text-xs text-white/40">{sub}</p>}
  </div>
);

// ─── Booking Status Bar ───────────────────────────────────────────────────────

interface StatusRow {
  label: string;
  count: number;
  color: string;
}

const StatusBreakdown: React.FC<{ rows: StatusRow[]; total: number }> = ({ rows, total }) => (
  <div className="rounded-2xl border border-white/10 bg-white/5 p-5">
    <h3 className="mb-4 text-sm font-semibold uppercase tracking-widest text-white/50">
      Booking Status
    </h3>
    <div className="space-y-3">
      {rows.map((r) => {
        const pct = total > 0 ? Math.round((r.count / total) * 100) : 0;
        return (
          <div key={r.label}>
            <div className="mb-1 flex items-center justify-between text-xs">
              <span className="text-white/70">{r.label}</span>
              <span className="font-mono text-white/50">
                {r.count} <span className="text-white/30">({pct}%)</span>
              </span>
            </div>
            <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/10">
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

// ─── SVG Bar Chart ────────────────────────────────────────────────────────────

const CHART_W = 560;
const CHART_H = 200;
const PAD_L   = 56;
const PAD_R   = 16;
const PAD_T   = 16;
const PAD_B   = 36;
const PLOT_W  = CHART_W - PAD_L - PAD_R;  // 488
const PLOT_H  = CHART_H - PAD_T - PAD_B;  // 148

const RevenueBarChart: React.FC<{ data: TrendPoint[]; days: number; onDaysChange: (d: number) => void }> = ({
  data,
  days,
  onDaysChange,
}) => {
  const [hoveredIdx, setHoveredIdx] = useState<number | null>(null);

  if (data.length === 0) {
    return (
      <div className="flex h-48 items-center justify-center text-sm text-white/30">
        No data yet
      </div>
    );
  }

  const n      = data.length;
  const maxRev = Math.max(...data.map((d) => d.revenue), 1);
  // Round max up to a neat ceiling for y-axis labels
  const magnitude = Math.pow(10, Math.floor(Math.log10(maxRev)));
  const ceiling   = Math.ceil(maxRev / magnitude) * magnitude;

  const barGap  = 6;
  const barW    = Math.max(4, PLOT_W / n - barGap);
  const slotW   = PLOT_W / n;

  // y-axis ticks (4 steps)
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
            className={`rounded-lg px-3 py-1 text-xs font-medium transition-colors ${
              days === d
                ? 'bg-cyan-500/20 text-cyan-300 ring-1 ring-cyan-500/40'
                : 'text-white/40 hover:text-white/70'
            }`}
          >
            {d}D
          </button>
        ))}
        <span className="ml-auto text-xs text-white/30">Revenue (MYR)</span>
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
          {/* Y-axis grid lines + labels */}
          {yTicks.map((tick) => {
            const y = PAD_T + PLOT_H - (tick / ceiling) * PLOT_H;
            return (
              <g key={tick}>
                <line
                  x1={PAD_L}
                  y1={y}
                  x2={CHART_W - PAD_R}
                  y2={y}
                  stroke="rgba(255,255,255,0.06)"
                  strokeWidth={1}
                />
                <text
                  x={PAD_L - 6}
                  y={y + 4}
                  textAnchor="end"
                  fontSize={9}
                  fill="rgba(255,255,255,0.30)"
                  fontFamily="JetBrains Mono, monospace"
                >
                  {tick >= 1000 ? `${(tick / 1000).toFixed(1)}k` : tick.toFixed(0)}
                </text>
              </g>
            );
          })}

          {/* Bars */}
          {data.map((point, i) => {
            const x  = barX(i);
            const h  = Math.max(barH(point.revenue), point.revenue > 0 ? 2 : 0);
            const y  = barY(point.revenue);
            const isHov = hoveredIdx === i;

            return (
              <g
                key={point.date}
                onMouseEnter={() => setHoveredIdx(i)}
                style={{ cursor: 'pointer' }}
              >
                {/* Invisible wider hit area */}
                <rect
                  x={PAD_L + i * slotW}
                  y={PAD_T}
                  width={slotW}
                  height={PLOT_H}
                  fill="transparent"
                />
                {/* Bar */}
                <rect
                  x={x}
                  y={y}
                  width={barW}
                  height={h}
                  rx={3}
                  fill={isHov ? '#00F0FF' : 'rgba(0,240,255,0.35)'}
                  style={{ transition: 'fill 0.15s' }}
                />
              </g>
            );
          })}

          {/* X-axis labels — show every Nth to avoid crowding */}
          {data.map((point, i) => {
            const step = n <= 7 ? 1 : n <= 14 ? 2 : 5;
            if (i % step !== 0 && i !== n - 1) return null;
            return (
              <text
                key={`lbl-${point.date}`}
                x={PAD_L + i * slotW + slotW / 2}
                y={CHART_H - 8}
                textAnchor="middle"
                fontSize={9}
                fill="rgba(255,255,255,0.30)"
                fontFamily="Inter, sans-serif"
              >
                {shortDay(point.date)}
              </text>
            );
          })}
        </svg>

        {/* Hover tooltip */}
        {hovered && (
          <div
            aria-live="polite"
            className="pointer-events-none absolute right-0 top-0 rounded-xl border border-cyan-500/30 bg-gray-900/90 px-3 py-2 text-xs shadow-xl backdrop-blur-sm"
          >
            <p className="font-medium text-white/80">{shortDay(hovered.date)}</p>
            <p className="mt-0.5 font-mono text-cyan-300">{fmtMyr(hovered.revenue)}</p>
            <p className="text-white/40">{hovered.bookingCount} bookings</p>
          </div>
        )}
      </div>
    </div>
  );
};

// ─── Skeleton ────────────────────────────────────────────────────────────────

const Skeleton: React.FC<{ className?: string }> = ({ className = '' }) => (
  <div className={`animate-pulse rounded-lg bg-white/10 ${className}`} />
);

// ─── Main Page ────────────────────────────────────────────────────────────────

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
    setLoadingS(true);
    setErrorS(null);
    try {
      const res = await axios.get<SalesSummary>('/api/v1/owner/analytics/summary', {
        params: { date: d },
      });
      setSummary(res.data);
    } catch {
      setErrorS('Failed to load daily summary.');
    } finally {
      setLoadingS(false);
    }
  }, []);

  const fetchTrend = useCallback(async (n: number) => {
    setLoadingT(true);
    setErrorT(null);
    try {
      const res = await axios.get<TrendPoint[]>('/api/v1/owner/analytics/trend', {
        params: { days: n },
      });
      setTrend(res.data);
    } catch {
      setErrorT('Failed to load trend data.');
    } finally {
      setLoadingT(false);
    }
  }, []);

  useEffect(() => { fetchSummary(date); }, [date, fetchSummary]);
  useEffect(() => { fetchTrend(days);   }, [days, fetchTrend]);

  const statusRows = summary
    ? [
        { label: 'Completed',  count: summary.completedBookings,  color: '#22c55e' },
        { label: 'Confirmed',  count: summary.confirmedBookings,  color: '#00F0FF' },
        { label: 'Pending',    count: summary.pendingBookings,    color: '#f59e0b' },
        { label: 'Cancelled',  count: summary.cancelledBookings,  color: '#ef4444' },
        { label: 'No-show',    count: summary.noShowBookings,     color: '#6b7280' },
      ]
    : [];

  return (
    <div className="mx-auto max-w-6xl space-y-8 px-4 py-8 sm:px-6 lg:px-8">

      {/* ── Page header ── */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-white">Analytics</h1>
          <p className="mt-1 text-sm text-white/40">Revenue and booking performance</p>
        </div>
        <div className="flex items-center gap-2">
          <label htmlFor="analytics-date" className="sr-only">Select date</label>
          <input
            id="analytics-date"
            type="date"
            value={date}
            max={todayStr()}
            onChange={(e) => setDate(e.target.value)}
            className="rounded-lg border border-white/10 bg-white/5 px-3 py-2 text-sm text-white/80 focus:outline-none focus:ring-2 focus:ring-cyan-500/50"
          />
          <button
            type="button"
            onClick={() => setDate(todayStr())}
            className="rounded-lg border border-white/10 bg-white/5 px-3 py-2 text-xs text-white/50 transition-colors hover:text-white"
          >
            Today
          </button>
        </div>
      </div>

      {/* ── Daily summary error ── */}
      {errorS && (
        <div role="alert" className="rounded-xl border border-red-500/30 bg-red-950/30 px-4 py-3 text-sm text-red-300">
          {errorS}
        </div>
      )}

      {/* ── KPI cards ── */}
      <section aria-label="Key performance indicators">
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          {loadingS ? (
            <>
              <Skeleton className="h-28" />
              <Skeleton className="h-28" />
              <Skeleton className="h-28" />
              <Skeleton className="h-28" />
            </>
          ) : summary ? (
            <>
              <KpiCard
                accent
                label="Total Revenue"
                value={fmtMyr(summary.totalRevenue)}
                sub={`${summary.completedPayments} paid transactions`}
              />
              <KpiCard
                label="Total Bookings"
                value={String(summary.totalBookings)}
                sub={`${summary.completedBookings} completed`}
              />
              <KpiCard
                label="Cash"
                value={fmtMyr(summary.cashRevenue)}
                sub="walk-in payments"
              />
              <KpiCard
                label="Online (FPX)"
                value={fmtMyr(summary.onlineRevenue)}
                sub={`${summary.pendingPayments} pending · ${summary.failedPayments} failed`}
              />
            </>
          ) : null}
        </div>
      </section>

      {/* ── Trend chart + status breakdown ── */}
      <section aria-label="Revenue trend and booking breakdown">
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">

          {/* Chart — takes 2/3 */}
          <div className="rounded-2xl border border-white/10 bg-white/5 p-5 lg:col-span-2">
            <h3 className="mb-4 text-sm font-semibold uppercase tracking-widest text-white/50">
              Revenue Trend
            </h3>
            {loadingT ? (
              <div className="space-y-2">
                <Skeleton className="h-40 w-full" />
                <Skeleton className="h-3 w-1/2" />
              </div>
            ) : errorT ? (
              <p role="alert" className="text-sm text-red-400">{errorT}</p>
            ) : (
              <RevenueBarChart
                data={trend}
                days={days}
                onDaysChange={setDays}
              />
            )}
          </div>

          {/* Status breakdown — takes 1/3 */}
          <div>
            {loadingS ? (
              <div className="rounded-2xl border border-white/10 bg-white/5 p-5 space-y-3">
                <Skeleton className="h-4 w-1/2" />
                {[1,2,3,4,5].map((k) => <Skeleton key={k} className="h-6 w-full" />)}
              </div>
            ) : summary ? (
              <StatusBreakdown
                rows={statusRows}
                total={summary.totalBookings}
              />
            ) : null}
          </div>
        </div>
      </section>

      {/* ── Payment method split ── */}
      {!loadingS && summary && (
        <section aria-label="Payment method split">
          <div className="rounded-2xl border border-white/10 bg-white/5 p-5">
            <h3 className="mb-4 text-sm font-semibold uppercase tracking-widest text-white/50">
              Payment Method Split
            </h3>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              {[
                { label: 'Cash',       value: summary.cashRevenue,   color: '#22c55e' },
                { label: 'Online FPX', value: summary.onlineRevenue, color: '#00F0FF' },
              ].map(({ label, value, color }) => {
                const total = summary.totalRevenue;
                const pct   = total > 0 ? Math.round((value / total) * 100) : 0;
                return (
                  <div key={label} className="flex items-center gap-4">
                    <div
                      className="h-10 w-10 flex-shrink-0 rounded-full"
                      style={{ backgroundColor: `${color}22`, border: `2px solid ${color}66` }}
                    />
                    <div className="flex-1">
                      <div className="flex items-baseline justify-between">
                        <span className="text-sm text-white/70">{label}</span>
                        <span className="font-mono text-sm text-white/50">{pct}%</span>
                      </div>
                      <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-white/10">
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
