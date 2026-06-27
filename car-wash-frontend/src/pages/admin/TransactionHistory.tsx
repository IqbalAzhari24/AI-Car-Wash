import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import api from '../../api/api';
import { statusBadge } from '../../components/ui';
import type { Transaction } from '../../types';

const COLUMN_HEADERS = ['Slot Time', 'Vehicle', 'Booking', 'Amount', 'Payment', 'Txn ID'];

const formatDateTime = (iso: string): string => {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleString();
};

const formatAmount = (amount: number | null): string =>
  amount == null ? '—' : `RM ${Number(amount).toFixed(2)}`;

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
        className="mb-4 inline-flex items-center gap-1 rounded text-sm font-medium text-[#00F0FF] hover:text-[#00B8C4]"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to directory
      </Link>

      <h1 className="mb-1 font-display text-2xl font-semibold tracking-tight text-[#E8E8F0]">Transaction history</h1>
      <p className="mb-6 font-mono text-sm text-[#9090A8]">Customer ID: {id}</p>

      {error && (
        <div className="hex-border-danger mb-4 rounded-lg p-3 text-sm text-[#FF4466]" role="alert">
          {error}
        </div>
      )}

      <div className="hex-border overflow-hidden rounded-xl bg-[#13131A]">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-[#1E1E2D]">
            <thead className="bg-[#1A1A24]">
              <tr>
                {COLUMN_HEADERS.map((h) => (
                  <th key={h} scope="col" className="px-4 py-3 text-left font-mono text-xs font-semibold uppercase tracking-wider text-[#9090A8]">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#1E1E2D] bg-[#13131A]">
              {loading ? (
                Array.from({ length: 5 }, (_, i) => (
                  <tr key={i}>
                    {COLUMN_HEADERS.map((h) => (
                      <td key={h} className="px-4 py-3.5">
                        <div className="h-4 animate-pulse rounded bg-[#1A1A24] motion-reduce:animate-none" />
                      </td>
                    ))}
                  </tr>
                ))
              ) : transactions.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center text-sm text-[#5A5A72]">
                    No transactions yet — this customer hasn't completed a booking.
                  </td>
                </tr>
              ) : (
                transactions.map((tx) => (
                  <tr key={tx.bookingId} className="transition-colors duration-150 hover:bg-[#1F1F2E]">
                    <td className="px-4 py-3 text-sm text-[#E8E8F0]">{formatDateTime(tx.slotTime)}</td>
                    <td className="px-4 py-3 text-sm text-[#E8E8F0]">{tx.vehicleClass ?? '—'}</td>
                    <td className="px-4 py-3 text-sm text-[#E8E8F0]">{tx.bookingStatus ?? '—'}</td>
                    <td className="px-4 py-3 font-mono text-sm font-medium tabular-nums text-[#E8E8F0]">{formatAmount(tx.amount)}</td>
                    <td className="px-4 py-3 text-sm">
                      <span className={statusBadge(tx.paymentStatus)}>
                        {tx.paymentStatus ?? 'N/A'}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-mono text-sm text-[#5A5A72]">{tx.transactionId ?? '—'}</td>
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
