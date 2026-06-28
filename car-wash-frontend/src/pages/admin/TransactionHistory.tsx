import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import api from '../../api/api';
import type { Transaction } from '../../types';

const COLUMN_HEADERS = ['Slot Time', 'Vehicle', 'Booking', 'Amount', 'Payment', 'Txn ID'];

const formatDateTime = (iso: string): string => {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleString();
};

const formatAmount = (amount: number | null): string =>
  amount == null ? '—' : `RM ${Number(amount).toFixed(2)}`;

const paymentBadgeClass = (status: string | null): string => {
  switch (status) {
    case 'COMPLETED': return 'border border-success/40 bg-success/10 text-success';
    case 'PENDING':   return 'border border-warning/40 bg-warning/10 text-warning';
    case 'FAILED':    return 'border border-danger/40 bg-danger/10 text-danger';
    case 'REFUNDED':  return 'border border-info/40 bg-info/10 text-info';
    default:          return 'border border-border bg-surface-raised text-secondary';
  }
};

export const TransactionHistory: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const res = await api.get<Transaction[]>(`/v1/owner/users/${id}/transactions`);
        if (active) setTransactions(res.data);
      } catch (err: any) {
        if (active) {
          setError(
            err?.response?.status === 403
              ? 'You do not have permission to view this history.'
              : 'Failed to load transaction history.'
          );
        }
      } finally {
        if (active) setLoading(false);
      }
    };
    load();
    return () => { active = false; };
  }, [id]);

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <Link
        to="/admin/users"
        className="mb-5 inline-flex min-h-[44px] items-center gap-1.5 rounded-lg text-sm font-medium text-secondary transition-colors hover:text-cyan focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cyan"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden="true" />
        Back to directory
      </Link>

      <h1 className="mb-1 font-display text-2xl font-bold text-primary">Transaction history</h1>
      <p className="mb-6 font-mono text-xs text-muted">Customer ID: {id}</p>

      {error && (
        <div
          role="alert"
          className="mb-4 hex-border-danger rounded-xl px-4 py-3 text-sm text-danger"
          style={{ backgroundColor: 'rgba(255,68,102,0.08)' }}
        >
          {error}
        </div>
      )}

      <div className="overflow-hidden rounded-xl hex-border bg-surface">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-border" aria-label="Transaction history">
            <thead className="bg-surface-raised">
              <tr>
                {COLUMN_HEADERS.map((h) => (
                  <th
                    key={h}
                    scope="col"
                    className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wide text-muted"
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-border-subtle bg-surface">
              {loading ? (
                Array.from({ length: 5 }, (_, i) => (
                  <tr key={i}>
                    {COLUMN_HEADERS.map((h) => (
                      <td key={h} className="px-4 py-3.5">
                        <div className="h-4 animate-pulse rounded bg-surface-raised motion-reduce:animate-none" />
                      </td>
                    ))}
                  </tr>
                ))
              ) : transactions.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center text-sm text-muted">
                    No transactions yet — this customer hasn't completed a booking.
                  </td>
                </tr>
              ) : (
                transactions.map((tx) => (
                  <tr key={tx.bookingId} className="transition-colors duration-150 hover:bg-surface-hover">
                    <td className="px-4 py-3 text-sm text-primary">{formatDateTime(tx.slotTime)}</td>
                    <td className="px-4 py-3 text-sm text-primary">{tx.vehicleClass ?? '—'}</td>
                    <td className="px-4 py-3 text-sm text-secondary">{tx.bookingStatus ?? '—'}</td>
                    <td className="px-4 py-3 text-sm font-medium tabular-nums text-primary">{formatAmount(tx.amount)}</td>
                    <td className="px-4 py-3 text-sm">
                      <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${paymentBadgeClass(tx.paymentStatus)}`}>
                        {tx.paymentStatus ?? 'N/A'}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-mono text-xs text-muted">{tx.transactionId ?? '—'}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
