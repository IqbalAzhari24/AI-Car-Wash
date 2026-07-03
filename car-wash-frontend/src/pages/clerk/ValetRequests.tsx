import React, { useCallback, useEffect, useState } from 'react';
import api from '../../api/api';
import { cardPanel, inputBase, statusBadge } from '../../components/ui';
import { fmtSlot } from '../../utils/format';

interface LocationDto {
  id: string;
  name: string;
}

interface ValetRequest {
  id: string;
  customerEmail: string;
  distanceKm: number | null;
  radiusKm: number | null;
  status: string;
  pickupTime: string | null;
  vehicleClass: string | null;
  vehicleModel: string | null;
  customerAddress: string | null;
}

/** Clerk actions available per current status (mirrors backend transition rules). */
const STATUS_ACTIONS: Record<string, { label: string; next: string; danger?: boolean }[]> = {
  PENDING: [
    { label: 'Accept', next: 'ACCEPTED' },
    { label: 'Reject', next: 'REJECTED', danger: true },
  ],
  ACCEPTED: [
    { label: 'Start pick-up', next: 'IN_PROGRESS' },
    { label: 'Cancel', next: 'CANCELLED', danger: true },
  ],
  IN_PROGRESS: [
    { label: 'Complete', next: 'COMPLETED' },
    { label: 'Cancel', next: 'CANCELLED', danger: true },
  ],
};

export const ValetRequests: React.FC = () => {
  const [locations, setLocations] = useState<LocationDto[]>([]);
  const [locationId, setLocationId] = useState('');
  const [requests, setRequests] = useState<ValetRequest[]>([]);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.get<LocationDto[]>('/v1/locations')
      .then(r => {
        setLocations(r.data);
        if (r.data[0]) setLocationId(r.data[0].id);
      })
      .catch(() => setError('Could not load branches.'));
  }, []);

  const load = useCallback((id: string) => {
    if (!id) return;
    setLoading(true);
    setError(null);
    api.get(`/v1/valet/requests?locationId=${id}`)
      .then(r => setRequests(r.data.data ?? []))
      .catch(() => setError('Could not load valet requests.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(locationId); }, [locationId, load]);

  async function updateStatus(id: string, next: string) {
    if (busy) return;
    setBusy(id);
    setError(null);
    try {
      await api.patch(`/v1/valet/requests/${id}/status`, { status: next });
      load(locationId);
    } catch (e: unknown) {
      const ax = e as { response?: { data?: { message?: string } } };
      setError(ax.response?.data?.message ?? 'Could not update the request.');
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="mx-auto w-full max-w-2xl px-4 py-8">
      <header className="mb-6">
        <h1 className="font-display text-2xl font-semibold tracking-tight text-[#E8E8F0]">Valet Requests</h1>
        <p className="mt-1 text-sm text-[#9090A8]">Pick-up requests submitted for each branch.</p>
      </header>

      <select value={locationId} onChange={e => setLocationId(e.target.value)} className={`${inputBase} mb-6`}>
        {locations.map(l => <option key={l.id} value={l.id}>{l.name}</option>)}
      </select>

      {error && <p role="alert" className="mb-4 text-sm text-[#FF4466]">{error}</p>}

      {loading && (
        <div className="space-y-3">
          {[1, 2, 3].map(i => <div key={i} className="h-24 animate-pulse rounded-2xl bg-[#1A1A24]" />)}
        </div>
      )}

      {!loading && !error && requests.length === 0 && (
        <p className="rounded-2xl border border-[#1E1E2D] bg-[#1A1A24] px-4 py-8 text-center text-sm text-[#5A5A72]">
          No valet requests for this branch.
        </p>
      )}

      {!loading && requests.length > 0 && (
        <ul className="space-y-3">
          {requests.map(r => (
            <li key={r.id} className={`${cardPanel} hex-corner p-4`}>
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="font-semibold text-[#E8E8F0]">{r.vehicleModel ?? 'Vehicle'}</p>
                  <p className="mt-0.5 text-sm text-[#9090A8]">{r.customerEmail}</p>
                  <p className="mt-1 text-xs text-[#5A5A72]">Pick-up {fmtSlot(r.pickupTime)}</p>
                  {r.customerAddress && <p className="mt-0.5 text-xs text-[#5A5A72]">{r.customerAddress}</p>}
                  {r.distanceKm != null && r.radiusKm != null && (
                    <p className="mt-0.5 text-xs text-[#5A5A72]">
                      {r.distanceKm.toFixed(1)} km away · limit {r.radiusKm.toFixed(0)} km
                    </p>
                  )}
                </div>
                <span className={statusBadge(r.status)}>{r.status}</span>
              </div>
              {STATUS_ACTIONS[r.status]?.length > 0 && (
                <div className="mt-3 flex gap-2 border-t border-[#1E1E2D] pt-3">
                  {STATUS_ACTIONS[r.status].map(a => (
                    <button
                      key={a.next}
                      type="button"
                      onClick={() => updateStatus(r.id, a.next)}
                      disabled={busy === r.id}
                      className={`min-h-[40px] flex-1 rounded-lg border px-3 py-2 text-sm font-medium transition-colors disabled:opacity-40 ${
                        a.danger
                          ? 'border-[#FF4466]/40 text-[#FF4466] hover:bg-[#FF4466]/10'
                          : 'border-[#00F0FF]/40 text-[#00F0FF] hover:bg-[#00F0FF]/10'
                      }`}
                    >
                      {busy === r.id ? 'Working…' : a.label}
                    </button>
                  ))}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};
