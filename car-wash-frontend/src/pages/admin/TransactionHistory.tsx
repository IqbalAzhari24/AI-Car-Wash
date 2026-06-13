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

const statusBadge = (status: string | null): string => {
  switch (status) {
    case 'COMPLETED':
      return 'bg-success-soft text-success-soft-ink';
    case 'PENDING':
      return 'bg-warning-soft text-warning-soft-ink';
    case 'FAILED':
      return 'bg-danger-soft text-danger-soft-ink';
    case 'REFUNDED':
      return 'bg-accent-soft text-accent-soft-ink';
    default:
      return 'bg-surface-2 text-muted';
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
    return () => {
      active = false;
    };
  }, [id]);

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <Link
        to="/admin/users"
        className="mb-4 inline-flex items-center gap-1 rounded text-sm font-medium text-primary hover:text-primary-strong"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to directory
      </Link>

      <h1 className="mb-1 text-2xl font-semibold tracking-tight text-ink">Transaction history</h1>
      <p className="mb-6 text-sm text-muted">Customer ID: {id}</p>

      {error && (
        <div className="mb-4 rounded-lg border border-danger/20 bg-danger-soft p-3 text-sm text-danger-soft-ink" role="alert">
          {error}
        </div>
      )}

      <div className="overflow-hidden rounded-xl border border-border bg-bg">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-border">
            <thead className="bg-surface">
              <tr>
                {COLUMN_HEADERS.map((h) => (
                  <th key={h} scope="col" className="px-4 py-3 text-left text-xs font-semibold text-muted">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-border bg-bg">
              {loading ? (
                Array.from({ length: 5 }, (_, i) => (
                  <tr key={i}>
                    {COLUMN_HEADERS.map((h) => (
                      <td key={h} className="px-4 py-3.5">
                        <div className="h-4 animate-pulse rounded bg-surface-2 motion-reduce:animate-none" />
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
                  <tr key={tx.bookingId} className="transition-colors duration-150 hover:bg-surface">
                    <td className="px-4 py-3 text-sm text-ink">{formatDateTime(tx.slotTime)}</td>
                    <td className="px-4 py-3 text-sm text-ink">{tx.vehicleClass ?? '—'}</td>
                    <td className="px-4 py-3 text-sm text-ink">{tx.bookingStatus ?? '—'}</td>
                    <td className="px-4 py-3 text-sm font-medium tabular-nums text-ink">{formatAmount(tx.amount)}</td>
                    <td className="px-4 py-3 text-sm">
                      <span
                        className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${statusBadge(
                          tx.paymentStatus
                        )}`}
                      >
                        {tx.paymentStatus ?? 'N/A'}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-mono text-sm text-muted">{tx.transactionId ?? '—'}</td>
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
