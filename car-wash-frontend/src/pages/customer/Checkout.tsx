import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Banknote, CreditCard, CheckCircle, ExternalLink } from 'lucide-react';
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

  useEffect(() => {
    let active = true;
    api.get<BookingDetail>(`/v1/bookings/${bookingId}`)
      .then(res => { if (active) { setBooking(res.data); setConfirmed(res.data.status === 'CONFIRMED'); } })
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

  // --- Loading skeleton ---
  if (loading) {
    return (
      <div className="mx-auto w-full max-w-lg px-4 py-12 space-y-3">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="h-10 animate-pulse rounded-lg bg-surface-2 motion-reduce:animate-none" />
        ))}
      </div>
    );
  }

  // --- Load error ---
  if (!booking) {
    return (
      <div className="mx-auto w-full max-w-lg px-4 py-16 text-center">
        <p className="text-sm text-danger-soft-ink">{loadErr || 'Booking not found.'}</p>
        <Link to="/" className="mt-4 inline-block text-sm text-primary hover:text-primary-strong">
          Back to home
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-lg px-4 py-10">
      <Link
        to="/"
        className="mb-6 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-strong"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to home
      </Link>

      {/* Booking summary */}
      <div className="overflow-hidden rounded-xl border border-border bg-bg">
        <div className="border-b border-border bg-surface px-5 py-4">
          <h1 className="text-lg font-semibold tracking-tight text-ink">
            {confirmed ? 'Booking confirmed' : 'Complete your booking'}
          </h1>
          <p className="mt-0.5 font-mono text-xs text-muted">{bookingId}</p>
        </div>

        <dl className="divide-y divide-border">
          <div className="flex items-center justify-between px-5 py-3.5">
            <dt className="text-sm text-muted">Date &amp; time</dt>
            <dd className="text-sm font-medium text-ink">{formatDateTime(booking.slotTime)}</dd>
          </div>
          <div className="flex items-center justify-between px-5 py-3.5">
            <dt className="text-sm text-muted">Vehicle</dt>
            <dd className="text-sm font-medium text-ink">
              {VEHICLE_LABELS[booking.vehicleClass ?? ''] ?? booking.vehicleClass} — {booking.vehicleModel}
            </dd>
          </div>
          <div className="flex items-center justify-between bg-surface/50 px-5 py-4">
            <dt className="text-sm font-semibold text-ink">Total</dt>
            <dd className="text-lg font-bold tabular-nums text-ink">
              RM {Number(booking.totalPrice).toFixed(2)}
            </dd>
          </div>
        </dl>
      </div>

      {/* Payment error */}
      {payErr && (
        <div
          role="alert"
          className="mt-4 rounded-lg border border-danger/20 bg-danger-soft px-4 py-3 text-sm text-danger-soft-ink"
        >
          {payErr}
        </div>
      )}

      {/* Confirmed state */}
      {confirmed ? (
        <div className="mt-6 rounded-xl border border-success/20 bg-success-soft px-5 py-6 text-center">
          <CheckCircle className="mx-auto h-8 w-8 text-success-soft-ink" />
          <p className="mt-2 font-semibold text-success-soft-ink">Payment complete — you're all set!</p>
          <p className="mt-1 text-sm text-success-soft-ink/80">
            Your car wash is confirmed. We'll see you at the shop.
          </p>
        </div>
      ) : (
        /* Payment options */
        <div className="mt-6 space-y-3">
          <p className="text-sm font-medium text-ink">Choose how to pay</p>

          <button
            onClick={() => pay('CASH')}
            disabled={!!paying}
            className="flex w-full items-center gap-3 rounded-xl border border-border bg-bg px-5 py-4 text-left transition-colors hover:bg-surface disabled:cursor-not-allowed disabled:opacity-50"
          >
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-accent-soft">
              <Banknote className="h-5 w-5 text-accent-soft-ink" />
            </span>
            <span className="flex-1">
              <span className="block text-sm font-semibold text-ink">Pay at counter</span>
              <span className="block text-xs text-muted">Cash when you arrive</span>
            </span>
            {paying === 'CASH' && (
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-border border-t-primary" />
            )}
          </button>

          <button
            onClick={() => pay('TOYYIBPAY')}
            disabled={!!paying}
            className="flex w-full items-center gap-3 rounded-xl border border-border bg-bg px-5 py-4 text-left transition-colors hover:bg-surface disabled:cursor-not-allowed disabled:opacity-50"
          >
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-soft">
              <CreditCard className="h-5 w-5 text-primary-soft-ink" />
            </span>
            <span className="flex-1">
              <span className="block text-sm font-semibold text-ink">Pay online</span>
              <span className="block text-xs text-muted">Card or FPX via toyyibPay</span>
            </span>
            {paying === 'TOYYIBPAY' ? (
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-border border-t-primary" />
            ) : (
              <ExternalLink className="h-4 w-4 text-muted" />
            )}
          </button>
        </div>
      )}
    </div>
  );
};
