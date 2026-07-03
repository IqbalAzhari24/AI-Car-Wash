import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Banknote, CreditCard, CheckCircle, ExternalLink, Star } from 'lucide-react';
import api from '../../api/api';

interface BookingDetail {
  id: string;
  slotTime: string;
  vehicleClass: string | null;
  vehicleModel: string | null;
  status: string;
  totalPrice: number;
  pickupRequested: boolean;
  deliveryRequested: boolean;
  pickupAddress: string | null;
}

interface CheckoutResponse {
  bookingId: string;
  paymentId: string;
  method: string;
  paymentStatus: string;
  amount: number;
  paymentUrl: string | null;
}

interface ReviewData {
  id: string;
  rating: number;
  comment: string | null;
}

const VEHICLE_LABELS: Record<string, string> = {
  MOTORCYCLE: 'Motorcycle',
  COMPACT:    'Compact',
  SEDAN:      'Sedan',
  SUV_LUXURY: 'SUV / Luxury',
  MPV_LARGE:  'MPV / Large',
};

const formatDateTime = (iso: string): string => {
  const d = new Date(iso);
  return Number.isNaN(d.getTime())
    ? iso
    : d.toLocaleString('en-MY', {
        weekday: 'short', year: 'numeric', month: 'short',
        day: 'numeric', hour: '2-digit', minute: '2-digit',
      });
};

export const CheckoutPage: React.FC = () => {
  const { bookingId } = useParams<{ bookingId: string }>();

  const [booking, setBooking]     = useState<BookingDetail | null>(null);
  const [loadErr, setLoadErr]     = useState('');
  const [loading, setLoading]     = useState(true);
  const [paying, setPaying]       = useState<'CASH' | 'TOYYIBPAY' | null>(null);
  const [payErr, setPayErr]       = useState('');
  const [confirmed, setConfirmed] = useState(false);

  const [review, setReview]       = useState<ReviewData | null | 'none'>('none');
  const [hoverStar, setHoverStar] = useState(0);
  const [rating, setRating]       = useState(0);
  const [comment, setComment]     = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [reviewErr, setReviewErr] = useState('');

  useEffect(() => {
    let active = true;
    api.get<BookingDetail>(`/v1/bookings/${bookingId}`)
      .then((res) => {
        if (!active) return;
        setBooking(res.data);
        setConfirmed(res.data.status === 'CONFIRMED');
        if (res.data.status === 'COMPLETED') {
          api.get<ReviewData>(`/v1/bookings/${bookingId}/review`)
            .then((r) => { if (active) setReview(r.data); })
            .catch(() => { if (active) setReview(null); });
        }
      })
      .catch((err) => {
        if (active) setLoadErr(err?.response?.status === 404 ? 'Booking not found.' : 'Failed to load booking.');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [bookingId]);

  const pay = async (method: 'CASH' | 'TOYYIBPAY') => {
    if (!booking) return;
    setPaying(method);
    setPayErr('');
    try {
      const res = await api.post<CheckoutResponse>(`/v1/bookings/${bookingId}/checkout`, { method });
      if (method === 'TOYYIBPAY' && res.data.paymentUrl) {
        if (!res.data.paymentUrl.startsWith('https://')) {
          setPayErr('Payment URL is invalid. Please contact support.');
          return;
        }
        window.location.href = res.data.paymentUrl;
        return;
      }
      setConfirmed(true);
      setBooking((b) => (b ? { ...b, status: 'CONFIRMED' } : b));
    } catch (err: any) {
      const s = err?.response?.status;
      if (s === 503) setPayErr('Online payment is not available. Please pay at the counter.');
      else if (s === 409) setPayErr('This booking has already been paid.');
      else setPayErr('Payment failed. Please try again or pay at the counter.');
    } finally {
      setPaying(null);
    }
  };

  const submitReview = async () => {
    if (rating === 0) { setReviewErr('Please select a star rating.'); return; }
    setSubmitting(true);
    setReviewErr('');
    try {
      const res = await api.post<ReviewData>(`/v1/bookings/${bookingId}/review`, {
        rating,
        comment: comment.trim() || null,
      });
      setReview(res.data);
    } catch (err: any) {
      if (err?.response?.status === 409) {
        setReview({ id: '', rating, comment: comment.trim() || null });
      } else {
        setReviewErr('Could not submit review. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="mx-auto w-full max-w-lg px-4 py-12 space-y-3" aria-busy="true" aria-label="Loading booking">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="h-10 animate-pulse rounded-lg bg-surface-raised motion-reduce:animate-none" />
        ))}
      </div>
    );
  }

  if (!booking) {
    return (
      <div className="mx-auto w-full max-w-lg px-4 py-16 text-center">
        <p className="text-sm text-danger">{loadErr || 'Booking not found.'}</p>
        <Link
          to="/"
          className="mt-4 inline-flex min-h-[44px] items-center rounded-lg text-sm font-medium text-cyan transition-colors hover:text-cyan-dim"
        >
          Back to home
        </Link>
      </div>
    );
  }

  const isCompleted    = booking.status === 'COMPLETED';
  const reviewSubmitted = review !== 'none' && review !== null;

  return (
    <div className="mx-auto w-full max-w-lg px-4 py-10">
      <Link
        to="/"
        className="mb-6 inline-flex min-h-[44px] items-center gap-1.5 text-sm font-medium text-secondary transition-colors hover:text-cyan focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Back to home
      </Link>

      {/* Booking summary */}
      <div className="hex-grid hex-border hex-corner overflow-hidden rounded-xl">
        <div className="border-b border-border-subtle px-5 py-4">
          <h1 className="font-display text-lg font-semibold text-primary">
            {isCompleted ? 'Wash completed' : confirmed ? 'Booking confirmed' : 'Complete your booking'}
          </h1>
          <p className="mt-0.5 font-mono text-xs text-muted">{bookingId}</p>
        </div>

        <dl className="divide-y divide-border-subtle">
          <div className="flex items-center justify-between px-5 py-3.5">
            <dt className="text-sm text-secondary">Date &amp; time</dt>
            <dd className="text-sm font-medium text-primary">{formatDateTime(booking.slotTime)}</dd>
          </div>
          <div className="flex items-center justify-between px-5 py-3.5">
            <dt className="text-sm text-secondary">Vehicle</dt>
            <dd className="text-sm font-medium text-primary">
              {VEHICLE_LABELS[booking.vehicleClass ?? ''] ?? booking.vehicleClass} — {booking.vehicleModel}
            </dd>
          </div>
          {(booking.pickupRequested || booking.deliveryRequested) && (
            <div className="flex items-center justify-between px-5 py-3.5">
              <dt className="text-sm text-secondary">Add-ons</dt>
              <dd className="text-sm font-medium text-primary">
                {[booking.pickupRequested && 'Valet pick-up', booking.deliveryRequested && 'Return delivery']
                  .filter(Boolean)
                  .join(' + ')}
              </dd>
            </div>
          )}
          <div className="flex items-center justify-between bg-cyan/5 px-5 py-4">
            <dt className="text-sm font-semibold text-primary">Total</dt>
            <dd className="font-display text-lg font-bold tabular-nums text-cyan">
              RM {Number(booking.totalPrice).toFixed(2)}
            </dd>
          </div>
        </dl>
      </div>

      {/* Payment error */}
      {payErr && (
        <div
          role="alert"
          className="mt-4 hex-border-danger rounded-xl px-4 py-3 text-sm text-danger"
          style={{ backgroundColor: 'rgba(255,68,102,0.08)' }}
        >
          {payErr}
        </div>
      )}

      {/* COMPLETED — review section */}
      {isCompleted && (
        <div className="mt-6 hex-grid hex-border hex-corner overflow-hidden rounded-xl">
          <div className="border-b border-border-subtle px-5 py-3">
            <h2 className="text-sm font-semibold text-primary">How was your wash?</h2>
          </div>
          <div className="px-5 py-5">
            {reviewSubmitted ? (
              <div className="text-center">
                <div className="flex justify-center gap-1 mb-2">
                  {[1, 2, 3, 4, 5].map((s) => (
                    <Star
                      key={s}
                      className={`h-6 w-6 ${
                        s <= (review as ReviewData).rating
                          ? 'fill-warning text-warning'
                          : 'text-border'
                      }`}
                      aria-hidden="true"
                    />
                  ))}
                </div>
                <p className="text-sm font-medium text-primary">Thanks for your feedback!</p>
                {(review as ReviewData).comment && (
                  <p className="mt-1 text-xs text-secondary italic">"{(review as ReviewData).comment}"</p>
                )}
              </div>
            ) : (
              <>
                <div className="flex justify-center gap-2 mb-4" role="group" aria-label="Star rating">
                  {[1, 2, 3, 4, 5].map((s) => (
                    <button
                      key={s}
                      type="button"
                      onClick={() => setRating(s)}
                      onMouseEnter={() => setHoverStar(s)}
                      onMouseLeave={() => setHoverStar(0)}
                      aria-label={`${s} star${s > 1 ? 's' : ''}`}
                      className="min-h-[44px] min-w-[44px] flex items-center justify-center p-1 transition-transform hover:scale-110 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-warning"
                    >
                      <Star
                        className={`h-8 w-8 transition-colors ${
                          s <= (hoverStar || rating)
                            ? 'fill-warning text-warning'
                            : 'text-border'
                        }`}
                        aria-hidden="true"
                      />
                    </button>
                  ))}
                </div>

                <textarea
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="Tell us more (optional)"
                  rows={3}
                  aria-label="Review comment"
                  className="w-full rounded-lg border border-border bg-surface-raised px-3 py-2 text-sm text-primary placeholder:text-muted resize-none transition-all duration-150 focus:outline-none focus-visible:border-cyan focus-visible:shadow-cyan-glow"
                />

                {reviewErr && (
                  <p role="alert" className="mt-2 text-xs text-danger">{reviewErr}</p>
                )}

                <button
                  onClick={submitReview}
                  disabled={submitting || rating === 0}
                  className="mt-3 w-full min-h-[44px] inline-flex items-center justify-center rounded-lg border border-cyan px-4 py-2.5 text-sm font-medium text-cyan shadow-cyan-glow transition-all duration-150 hover:bg-cyan hover:text-base active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-40 disabled:pointer-events-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan focus-visible:ring-offset-2 focus-visible:ring-offset-base"
                >
                  {submitting ? 'Submitting…' : 'Submit review'}
                </button>
              </>
            )}
          </div>
        </div>
      )}

      {/* CONFIRMED — success banner */}
      {!isCompleted && confirmed && (
        <div className="mt-6 hex-radial-success hex-border-success rounded-xl px-5 py-6 text-center">
          <CheckCircle className="mx-auto h-8 w-8 text-success" aria-hidden="true" />
          <p className="mt-2 font-semibold text-success">Payment complete — you're all set!</p>
          <p className="mt-1 text-sm text-secondary">
            Your car wash is confirmed. We'll see you at the shop.
          </p>
        </div>
      )}

      {/* PENDING — payment options */}
      {!isCompleted && !confirmed && (
        <div className="mt-6 space-y-3">
          <p className="text-sm font-medium text-primary">Choose how to pay</p>

          <button
            onClick={() => pay('CASH')}
            disabled={!!paying}
            className="flex w-full min-h-[60px] items-center gap-3 hex-grid hex-border rounded-xl px-5 py-4 text-left transition-all duration-150 hover:hex-border-active active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan"
          >
            <span className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-success/10 border border-success/30">
              <Banknote className="h-5 w-5 text-success" aria-hidden="true" />
            </span>
            <span className="flex-1">
              <span className="block text-sm font-semibold text-primary">Pay at counter</span>
              <span className="block text-xs text-secondary">Cash when you arrive</span>
            </span>
            {paying === 'CASH' && (
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-border border-t-cyan" aria-hidden="true" />
            )}
          </button>

          <button
            onClick={() => pay('TOYYIBPAY')}
            disabled={!!paying}
            className="flex w-full min-h-[60px] items-center gap-3 hex-grid hex-border rounded-xl px-5 py-4 text-left transition-all duration-150 hover:hex-border-active active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan"
          >
            <span className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-cyan/10 border border-cyan/30">
              <CreditCard className="h-5 w-5 text-cyan" aria-hidden="true" />
            </span>
            <span className="flex-1">
              <span className="block text-sm font-semibold text-primary">Pay online</span>
              <span className="block text-xs text-secondary">Card or FPX via toyyibPay</span>
            </span>
            {paying === 'TOYYIBPAY' ? (
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-border border-t-cyan" aria-hidden="true" />
            ) : (
              <ExternalLink className="h-4 w-4 text-muted" aria-hidden="true" />
            )}
          </button>
        </div>
      )}
    </div>
  );
};
