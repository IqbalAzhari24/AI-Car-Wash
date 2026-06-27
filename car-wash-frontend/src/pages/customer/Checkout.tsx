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
  COMPACT: 'Compact',
  SEDAN: 'Sedan',
  SUV_LUXURY: 'SUV / Luxury',
  MPV_LARGE: 'MPV / Large',
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

  const [booking, setBooking] = useState<BookingDetail | null>(null);
  const [loadErr, setLoadErr] = useState('');
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState<'CASH' | 'TOYYIBPAY' | null>(null);
  const [payErr, setPayErr] = useState('');
  const [confirmed, setConfirmed] = useState(false);

  // Review state — only shown when booking.status === 'COMPLETED'
  const [review, setReview] = useState<ReviewData | null | 'none'>('none');
  const [hoverStar, setHoverStar] = useState(0);
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [reviewErr, setReviewErr] = useState('');

  useEffect(() => {
    let active = true;
    api.get<BookingDetail>(`/v1/bookings/${bookingId}`)
      .then(res => {
        if (!active) return;
        setBooking(res.data);
        setConfirmed(res.data.status === 'CONFIRMED');
        // Fetch existing review only when wash is done
        if (res.data.status === 'COMPLETED') {
          api.get<ReviewData>(`/v1/bookings/${bookingId}/review`)
            .then(r => { if (active) setReview(r.data); })
            .catch(err => { if (active) setReview(err?.response?.status === 204 ? null : null); });
        }
      })
      .catch(err => { if (active) setLoadErr(err?.response?.status === 404 ? 'Booking not found.' : 'Failed to load booking.'); })
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
      setBooking(b => b ? { ...b, status: 'CONFIRMED' } : b);
    } catch (err: any) {
      const status = err?.response?.status;
      if (status === 503) setPayErr('Online payment is not available. Please pay at the counter.');
      else if (status === 409) setPayErr('This booking has already been paid.');
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
      const status = err?.response?.status;
      if (status === 409) setReview({ id: '', rating, comment: comment.trim() || null }); // already reviewed
      else setReviewErr('Could not submit review. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  // --- Loading skeleton ---
  if (loading) {
    return (
      <div className="mx-auto w-full max-w-lg px-4 py-12 space-y-3">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="h-10 animate-pulse rounded-lg bg-[#1A1A24] motion-reduce:animate-none" />
        ))}
      </div>
    );
  }

  // --- Load error ---
  if (!booking) {
    return (
      <div className="mx-auto w-full max-w-lg px-4 py-16 text-center">
        <p className="text-sm text-[#FF4466]">{loadErr || 'Booking not found.'}</p>
        <Link to="/" className="mt-4 inline-block text-sm font-medium text-[#00F0FF] hover:text-[#00B8C4]">
          Back to home
        </Link>
      </div>
    );
  }

  const isCompleted = booking.status === 'COMPLETED';
  const reviewSubmitted = review !== 'none' && review !== null;

  return (
    <div className="mx-auto w-full max-w-lg px-4 py-10">
      <Link
        to="/"
        className="mb-6 inline-flex items-center gap-1 text-sm font-medium text-[#00F0FF] hover:text-[#00B8C4]"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to home
      </Link>

      {/* Booking summary */}
      <div className="hex-border hex-corner overflow-hidden rounded-xl bg-[#13131A]">
        <div className="border-b border-[#1E1E2D] bg-[#1A1A24] px-5 py-4">
          <h1 className="font-display text-lg font-semibold tracking-tight text-[#E8E8F0]">
            {isCompleted ? 'Wash completed' : confirmed ? 'Booking confirmed' : 'Complete your booking'}
          </h1>
          <p className="mt-0.5 font-mono text-xs text-[#5A5A72]">{bookingId}</p>
        </div>

        <dl className="divide-y divide-[#1E1E2D]">
          <div className="flex items-center justify-between px-5 py-3.5">
            <dt className="text-sm text-[#9090A8]">Date &amp; time</dt>
            <dd className="text-sm font-medium text-[#E8E8F0]">{formatDateTime(booking.slotTime)}</dd>
          </div>
          <div className="flex items-center justify-between px-5 py-3.5">
            <dt className="text-sm text-[#9090A8]">Vehicle</dt>
            <dd className="text-sm font-medium text-[#E8E8F0]">
              {VEHICLE_LABELS[booking.vehicleClass ?? ''] ?? booking.vehicleClass} — {booking.vehicleModel}
            </dd>
          </div>
          <div className="flex items-center justify-between bg-[#1A1A24]/50 px-5 py-4">
            <dt className="text-sm font-semibold text-[#E8E8F0]">Total</dt>
            <dd className="font-mono text-lg font-bold tabular-nums text-[#00F0FF]">
              RM {Number(booking.totalPrice).toFixed(2)}
            </dd>
          </div>
        </dl>
      </div>

      {/* Payment error */}
      {payErr && (
        <div
          role="alert"
          className="hex-border-danger mt-4 rounded-lg px-4 py-3 text-sm text-[#FF4466]"
        >
          {payErr}
        </div>
      )}

      {/* COMPLETED — review section */}
      {isCompleted && (
        <div className="hex-border mt-6 overflow-hidden rounded-xl bg-[#13131A]">
          <div className="border-b border-[#1E1E2D] bg-[#1A1A24] px-5 py-3">
            <h2 className="font-display text-sm font-semibold text-[#E8E8F0]">How was your wash?</h2>
          </div>
          <div className="px-5 py-5">
            {reviewSubmitted ? (
              <div className="text-center">
                <div className="flex justify-center gap-1 mb-2">
                  {[1, 2, 3, 4, 5].map(s => (
                    <Star
                      key={s}
                      className={`h-6 w-6 ${s <= (review as ReviewData).rating ? 'fill-[#FFB800] text-[#FFB800]' : 'text-[#2A2A3D]'}`}
                    />
                  ))}
                </div>
                <p className="text-sm font-medium text-[#E8E8F0]">Thanks for your feedback!</p>
                {(review as ReviewData).comment && (
                  <p className="mt-1 text-xs italic text-[#9090A8]">"{(review as ReviewData).comment}"</p>
                )}
              </div>
            ) : (
              <>
                {/* Star picker */}
                <div className="flex justify-center gap-2 mb-4">
                  {[1, 2, 3, 4, 5].map(s => (
                    <button
                      key={s}
                      type="button"
                      onClick={() => setRating(s)}
                      onMouseEnter={() => setHoverStar(s)}
                      onMouseLeave={() => setHoverStar(0)}
                      className="p-1 transition-transform hover:scale-110"
                      aria-label={`${s} star${s > 1 ? 's' : ''}`}
                    >
                      <Star
                        className={`h-8 w-8 transition-colors ${
                          s <= (hoverStar || rating)
                            ? 'fill-[#FFB800] text-[#FFB800]'
                            : 'text-[#2A2A3D]'
                        }`}
                      />
                    </button>
                  ))}
                </div>

                {/* Comment */}
                <textarea
                  value={comment}
                  onChange={e => setComment(e.target.value)}
                  placeholder="Tell us more (optional)"
                  rows={3}
                  className="w-full resize-none rounded-lg border border-[#2A2A3D] bg-[#13131A] px-3 py-2 text-sm text-[#E8E8F0] placeholder:text-[#5A5A72] focus:border-[#00F0FF] focus:outline-none focus:ring-1 focus:ring-[#00F0FF]"
                />

                {reviewErr && (
                  <p role="alert" className="mt-2 text-xs text-[#FF4466]">{reviewErr}</p>
                )}

                <button
                  onClick={submitReview}
                  disabled={submitting || rating === 0}
                  className="mt-3 w-full rounded-lg bg-[#00F0FF] px-4 py-2.5 text-sm font-semibold text-[#0D0D11] shadow-cyan-glow transition-colors hover:bg-[#00B8C4] disabled:cursor-not-allowed disabled:opacity-50"
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
        <div className="hex-border-success mt-6 rounded-xl bg-[#13131A] px-5 py-6 text-center">
          <CheckCircle className="mx-auto h-8 w-8 text-[#00E5A0]" />
          <p className="mt-2 font-semibold text-[#00E5A0]">Payment complete — you're all set!</p>
          <p className="mt-1 text-sm text-[#9090A8]">
            Your car wash is confirmed. We'll see you at the shop.
          </p>
        </div>
      )}

      {/* PENDING — payment options */}
      {!isCompleted && !confirmed && (
        <div className="mt-6 space-y-3">
          <p className="text-sm font-medium text-[#E8E8F0]">Choose how to pay</p>

          <button
            onClick={() => pay('CASH')}
            disabled={!!paying}
            className="hex-border flex w-full items-center gap-3 rounded-xl bg-[#13131A] px-5 py-4 text-left transition-colors hover:bg-[#1F1F2E] disabled:cursor-not-allowed disabled:opacity-50"
          >
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-[#00E5A0]/30 bg-[#00E5A0]/10">
              <Banknote className="h-5 w-5 text-[#00E5A0]" />
            </span>
            <span className="flex-1">
              <span className="block text-sm font-semibold text-[#E8E8F0]">Pay at counter</span>
              <span className="block text-xs text-[#9090A8]">Cash when you arrive</span>
            </span>
            {paying === 'CASH' && (
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-[#2A2A3D] border-t-[#00F0FF]" />
            )}
          </button>

          <button
            onClick={() => pay('TOYYIBPAY')}
            disabled={!!paying}
            className="hex-border flex w-full items-center gap-3 rounded-xl bg-[#13131A] px-5 py-4 text-left transition-colors hover:bg-[#1F1F2E] disabled:cursor-not-allowed disabled:opacity-50"
          >
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-[#00F0FF]/30 bg-[#00F0FF]/10">
              <CreditCard className="h-5 w-5 text-[#00F0FF]" />
            </span>
            <span className="flex-1">
              <span className="block text-sm font-semibold text-[#E8E8F0]">Pay online</span>
              <span className="block text-xs text-[#9090A8]">Card or FPX via toyyibPay</span>
            </span>
            {paying === 'TOYYIBPAY' ? (
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-[#2A2A3D] border-t-[#00F0FF]" />
            ) : (
              <ExternalLink className="h-4 w-4 text-[#9090A8]" />
            )}
          </button>
        </div>
      )}
    </div>
  );
};
