import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { CalendarClock, Star } from 'lucide-react';
import api from '../../api/api';
import { btnPrimary, btnSecondary, btnGhost, cardPanel, inputBase, statusBadge } from '../../components/ui';

interface Booking {
  id: string;
  serviceName: string | null;
  slotTime: string;
  vehicleClass: string | null;
  vehicleModel: string;
  status: string;
  totalPrice: number;
  createdAt: string;
}

/** "2024-01-15T09:00:00" → "Mon, 15 Jan 2024, 09:00" */
function fmtDateTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString('en-MY', {
      weekday: 'short', day: 'numeric', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  } catch {
    return iso;
  }
}

// ─── Review block (shown under COMPLETED bookings) ────────────────────────────

interface Review {
  rating: number;
  comment: string | null;
}

const Stars: React.FC<{ value: number; onPick?: (n: number) => void }> = ({ value, onPick }) => (
  <div className="flex gap-1">
    {[1, 2, 3, 4, 5].map(n => (
      <button
        key={n}
        type="button"
        disabled={!onPick}
        onClick={onPick ? () => onPick(n) : undefined}
        aria-label={`${n} star${n > 1 ? 's' : ''}`}
        className={onPick ? 'cursor-pointer' : 'cursor-default'}
      >
        <Star className={`h-5 w-5 ${n <= value ? 'fill-[#FFB800] text-[#FFB800]' : 'text-[#5A5A72]'}`} />
      </button>
    ))}
  </div>
);

const ReviewBlock: React.FC<{ bookingId: string }> = ({ bookingId }) => {
  const [existing, setExisting] = useState<Review | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<Review>(`/v1/bookings/${bookingId}/review`)
      .then(r => { if (r.status === 200 && r.data) setExisting(r.data); }) // 204 = not reviewed yet
      .catch(() => {})
      .finally(() => setLoaded(true));
  }, [bookingId]);

  async function submit() {
    if (submitting) return;
    if (rating < 1) { setErr('Pick a star rating first.'); return; }
    setSubmitting(true);
    setErr(null);
    try {
      const r = await api.post<Review>(`/v1/bookings/${bookingId}/review`, {
        rating,
        comment: comment.trim() || null,
      });
      setExisting(r.data);
    } catch (e: unknown) {
      const ax = e as { response?: { data?: { message?: string } } };
      setErr(ax.response?.data?.message ?? 'Could not submit your review.');
    } finally {
      setSubmitting(false);
    }
  }

  if (!loaded) return null;

  return (
    <div className="mt-4 border-t border-[#1E1E2D] pt-3">
      {existing ? (
        <div>
          <p className="mb-1.5 text-xs uppercase tracking-wider text-[#5A5A72]">Your review</p>
          <Stars value={existing.rating} />
          {existing.comment && <p className="mt-1.5 text-sm text-[#9090A8]">{existing.comment}</p>}
        </div>
      ) : (
        <div className="space-y-2">
          <p className="text-xs uppercase tracking-wider text-[#5A5A72]">Rate your wash</p>
          <Stars value={rating} onPick={setRating} />
          <textarea
            value={comment}
            onChange={e => setComment(e.target.value)}
            rows={2}
            placeholder="Optional comment"
            className={inputBase}
          />
          {err && <p role="alert" className="text-sm text-[#FF4466]">{err}</p>}
          <button type="button" onClick={submit} disabled={submitting} className={`${btnSecondary} w-full`}>
            {submitting ? 'Submitting…' : 'Submit review'}
          </button>
        </div>
      )}
    </div>
  );
};

export const MyBookings: React.FC = () => {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    api
      .get<Booking[]>('/v1/bookings/mine')
      .then(r => setBookings(r.data))
      .catch(() => setError('Could not load your bookings. Please try again.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  async function cancel(id: string) {
    if (busyId) return;
    setBusyId(id);
    setActionError(null);
    try {
      const r = await api.post<Booking>(`/v1/bookings/${id}/cancel`);
      setBookings(prev => prev.map(b => (b.id === id ? r.data : b)));
    } catch (e: unknown) {
      const ax = e as { response?: { data?: { message?: string } } };
      setActionError(ax.response?.data?.message ?? 'Could not cancel this booking.');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="mx-auto w-full max-w-2xl px-4 py-8">
      <header className="mb-6">
        <h1 className="font-display text-2xl font-semibold tracking-tight text-[#E8E8F0]">My Bookings</h1>
        <p className="mt-1 text-sm text-[#9090A8]">Track each wash, pay pending bookings, or cancel.</p>
      </header>

      {actionError && (
        <p role="alert" className="mb-4 rounded-lg border border-[#FF4466]/40 bg-[#FF4466]/10 px-4 py-3 text-sm text-[#FF4466]">
          {actionError}
        </p>
      )}

      {loading && (
        <div className="space-y-3">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-28 animate-pulse rounded-2xl bg-[#1A1A24]" />
          ))}
        </div>
      )}

      {!loading && error && (
        <p role="alert" className="text-sm text-[#FF4466]">{error}</p>
      )}

      {!loading && !error && bookings.length === 0 && (
        <div className={`${cardPanel} hex-corner p-8 text-center`}>
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full border border-[#00F0FF]/30 bg-[#00F0FF]/10">
            <CalendarClock className="h-7 w-7 text-[#00F0FF]" />
          </div>
          <p className="mt-4 text-sm text-[#9090A8]">You have no bookings yet.</p>
          <Link to="/book" className={`${btnPrimary} mt-5`}>Book a wash</Link>
        </div>
      )}

      {!loading && bookings.length > 0 && (
        <ul className="space-y-3">
          {bookings.map(b => {
            const canPay = b.status === 'PENDING';
            const canCancel = b.status === 'PENDING' || b.status === 'CONFIRMED';
            return (
              <li key={b.id} className={`${cardPanel} hex-corner p-4`}>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="font-semibold text-[#E8E8F0]">{b.serviceName ?? 'Wash'}</p>
                    <p className="mt-0.5 text-sm text-[#9090A8]">
                      {b.vehicleModel}
                      {b.vehicleClass && (
                        <span className="ml-1.5 text-xs text-[#5A5A72]">
                          ({b.vehicleClass.replace('_', ' ').toLowerCase()})
                        </span>
                      )}
                    </p>
                    <p className="mt-1 text-xs text-[#5A5A72]">{fmtDateTime(b.slotTime)}</p>
                  </div>
                  <div className="shrink-0 text-right">
                    <span className={statusBadge(b.status)}>{b.status.replace('_', ' ')}</span>
                    <p className="mt-1.5 font-mono text-sm font-semibold text-[#E8E8F0]">
                      RM {Number(b.totalPrice).toFixed(2)}
                    </p>
                  </div>
                </div>

                {(canPay || canCancel) && (
                  <div className="mt-4 flex gap-2 border-t border-[#1E1E2D] pt-3">
                    {canPay && (
                      <Link to={`/checkout/${b.id}`} className={`${btnPrimary} flex-1`}>
                        Pay now
                      </Link>
                    )}
                    {canCancel && (
                      <button
                        type="button"
                        disabled={busyId === b.id}
                        onClick={() => cancel(b.id)}
                        className={`${btnGhost} flex-1`}
                      >
                        {busyId === b.id ? 'Cancelling…' : 'Cancel'}
                      </button>
                    )}
                  </div>
                )}

                {b.status === 'COMPLETED' && <ReviewBlock bookingId={b.id} />}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
};
