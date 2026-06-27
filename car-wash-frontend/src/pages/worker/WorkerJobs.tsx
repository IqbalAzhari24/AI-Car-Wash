import React, { useCallback, useEffect, useState } from 'react';
import { Loader2, Car } from 'lucide-react';
import api from '../../api/api';
import { btnPrimary, btnSecondary, cardPanel, statusBadge } from '../../components/ui';

interface Job {
  id: string;
  serviceName: string | null;
  slotTime: string;
  vehicleClass: string | null;
  vehicleModel: string;
  status: string; // CONFIRMED | IN_PROGRESS
}

/** "2024-01-15T09:00:00" → "15 Jan, 09:00" */
function fmtSlot(iso: string): string {
  try {
    return new Date(iso).toLocaleString('en-MY', {
      day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
    });
  } catch {
    return iso;
  }
}

export const WorkerJobs: React.FC = () => {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    api
      .get<Job[]>('/v1/bookings/jobs')
      .then(r => setJobs(r.data))
      .catch(() => setError('Could not load the job queue. Please try again.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  async function advance(id: string, status: 'IN_PROGRESS' | 'COMPLETED') {
    if (busyId) return;
    setBusyId(id);
    setActionError(null);
    try {
      await api.patch(`/v1/bookings/${id}/status`, { status });
      // Completed jobs drop off the active queue; started jobs flip to IN_PROGRESS.
      if (status === 'COMPLETED') {
        setJobs(prev => prev.filter(j => j.id !== id));
      } else {
        setJobs(prev => prev.map(j => (j.id === id ? { ...j, status } : j)));
      }
    } catch (e: unknown) {
      const ax = e as { response?: { data?: { message?: string } } };
      setActionError(ax.response?.data?.message ?? 'Could not update this job.');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="mx-auto w-full max-w-2xl px-4 py-8">
      <header className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl font-semibold tracking-tight text-[#E8E8F0]">Job Board</h1>
          <p className="mt-1 text-sm text-[#9090A8]">Confirmed and in-progress washes. Start them, then mark complete.</p>
        </div>
        <button type="button" onClick={load} className={btnSecondary}>Refresh</button>
      </header>

      {actionError && (
        <p role="alert" className="mb-4 rounded-lg border border-[#FF4466]/40 bg-[#FF4466]/10 px-4 py-3 text-sm text-[#FF4466]">
          {actionError}
        </p>
      )}

      {loading && (
        <div className="space-y-3">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-24 animate-pulse rounded-2xl bg-[#1A1A24]" />
          ))}
        </div>
      )}

      {!loading && error && <p role="alert" className="text-sm text-[#FF4466]">{error}</p>}

      {!loading && !error && jobs.length === 0 && (
        <div className={`${cardPanel} hex-corner p-8 text-center`}>
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full border border-[#00E5A0]/30 bg-[#00E5A0]/10">
            <Car className="h-7 w-7 text-[#00E5A0]" />
          </div>
          <p className="mt-4 text-sm text-[#9090A8]">No active jobs. All caught up.</p>
        </div>
      )}

      {!loading && jobs.length > 0 && (
        <ul className="space-y-3">
          {jobs.map(j => {
            const inProgress = j.status === 'IN_PROGRESS';
            return (
              <li key={j.id} className={`${cardPanel} hex-corner p-4`}>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="font-semibold text-[#E8E8F0]">{j.vehicleModel}</p>
                    <p className="mt-0.5 text-sm text-[#9090A8]">
                      {j.serviceName ?? 'Wash'}
                      {j.vehicleClass && (
                        <span className="ml-1.5 text-xs text-[#5A5A72]">
                          ({j.vehicleClass.replace('_', ' ').toLowerCase()})
                        </span>
                      )}
                    </p>
                    <p className="mt-1 text-xs text-[#5A5A72]">Slot {fmtSlot(j.slotTime)}</p>
                  </div>
                  <span className={statusBadge(j.status)}>{j.status.replace('_', ' ')}</span>
                </div>

                <div className="mt-4 border-t border-[#1E1E2D] pt-3">
                  {inProgress ? (
                    <button
                      type="button"
                      disabled={busyId === j.id}
                      onClick={() => advance(j.id, 'COMPLETED')}
                      className={`${btnPrimary} w-full`}
                    >
                      {busyId === j.id ? <Loader2 className="h-4 w-4 animate-spin" /> : 'Mark complete'}
                    </button>
                  ) : (
                    <button
                      type="button"
                      disabled={busyId === j.id}
                      onClick={() => advance(j.id, 'IN_PROGRESS')}
                      className={`${btnSecondary} w-full`}
                    >
                      {busyId === j.id ? <Loader2 className="h-4 w-4 animate-spin" /> : 'Start wash'}
                    </button>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
};
