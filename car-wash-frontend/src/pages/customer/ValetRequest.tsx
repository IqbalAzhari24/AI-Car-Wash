import React, { useCallback, useEffect, useState } from 'react';
import { MapPin, Loader2 } from 'lucide-react';
import api from '../../api/api';
import { btnPrimary, cardPanel, inputBase, statusBadge } from '../../components/ui';
import { getPosition, reverseGeocode } from '../../utils/geo';
import { fmtSlot } from '../../utils/format';

interface LocationDto {
  id: string;
  name: string;
  address: string;
}

interface ValetRequest {
  id: string;
  locationName: string;
  distanceKm: number | null;
  radiusKm: number | null;
  status: string;
  pickupTime: string | null;
  vehicleModel: string | null;
  createdAt: string;
}

type VehicleClass = 'MOTORCYCLE' | 'COMPACT' | 'SEDAN' | 'SUV_LUXURY' | 'MPV_LARGE';
const VEHICLE_CLASSES: VehicleClass[] = ['MOTORCYCLE', 'COMPACT', 'SEDAN', 'SUV_LUXURY', 'MPV_LARGE'];

export const ValetRequest: React.FC = () => {
  const [locations, setLocations] = useState<LocationDto[]>([]);
  const [requests, setRequests] = useState<ValetRequest[]>([]);
  const [loading, setLoading] = useState(true);

  const [locationId, setLocationId] = useState('');
  const [pickupTime, setPickupTime] = useState('');
  const [vehicleClass, setVehicleClass] = useState<VehicleClass>('SEDAN');
  const [vehicleModel, setVehicleModel] = useState('');
  const [address, setAddress] = useState('');
  const [notes, setNotes] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [locating, setLocating] = useState(false);
  const [cancelling, setCancelling] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<{ ok: boolean; message: string } | null>(null);

  async function useMyLocation() {
    setLocating(true);
    setError(null);
    try {
      const pos = await getPosition();
      const addr = await reverseGeocode(pos.coords.latitude, pos.coords.longitude);
      setAddress(addr);
    } catch (e: unknown) {
      const geo = e as { code?: number; message?: string };
      setError(geo.code === 1 ? 'Location permission denied. Allow location access to pin your address.' : geo.message ?? 'Could not detect your address.');
    } finally {
      setLocating(false);
    }
  }

  const loadMine = useCallback(() => {
    api.get('/v1/valet/requests/mine')
      .then(r => setRequests(r.data.data ?? []))
      .catch(() => {});
  }, []);

  async function cancelRequest(id: string) {
    if (cancelling) return;
    setCancelling(id);
    setError(null);
    try {
      await api.patch(`/v1/valet/requests/${id}/cancel`);
      loadMine();
    } catch (e: unknown) {
      const ax = e as { response?: { data?: { message?: string } } };
      setError(ax.response?.data?.message ?? 'Could not cancel the request.');
    } finally {
      setCancelling(null);
    }
  }

  useEffect(() => {
    api.get<LocationDto[]>('/v1/locations')
      .then(r => {
        setLocations(r.data);
        if (r.data[0]) setLocationId(r.data[0].id);
      })
      .catch(() => setError('Could not load branches.'))
      .finally(() => setLoading(false));
    loadMine();
    // Refresh when a live status push arrives (see useUpdateToasts)
    window.addEventListener('app-update', loadMine);
    return () => window.removeEventListener('app-update', loadMine);
  }, [loadMine]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setError(null);
    setResult(null);
    if (!locationId || !pickupTime || !vehicleModel.trim()) {
      setError('Select a branch, pick-up time and enter your vehicle model.');
      return;
    }
    setSubmitting(true);
    try {
      const pos = await getPosition();
      const res = await api.post('/v1/valet/requests', {
        locationId,
        customerLat: pos.coords.latitude,
        customerLng: pos.coords.longitude,
        customerAddress: address.trim() || null,
        pickupTime,
        vehicleClass,
        vehicleModel: vehicleModel.trim(),
        notes: notes.trim() || null,
      });
      const accepted = res.data?.data?.status === 'ACCEPTED';
      setResult({ ok: accepted, message: res.data?.message ?? 'Request submitted.' });
      setVehicleModel(''); setNotes(''); setAddress(''); setPickupTime('');
      loadMine();
    } catch (e: unknown) {
      const geo = e as { code?: number; message?: string };
      if (geo.code === 1) {
        setError('Location permission denied. Allow location access to request a valet pick-up.');
      } else {
        const ax = e as { response?: { data?: { message?: string } } };
        setError(ax.response?.data?.message ?? geo.message ?? 'Could not submit the request.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto w-full max-w-2xl px-4 py-8">
      <header className="mb-6">
        <h1 className="font-display text-2xl font-semibold tracking-tight text-[#E8E8F0]">Valet Pick-up</h1>
        <p className="mt-1 text-sm text-[#9090A8]">We collect your car, wash it, and bring it back — if you're within range.</p>
      </header>

      {result && (
        <p role="status" className={`mb-4 rounded-lg border px-4 py-3 text-sm ${
          result.ok
            ? 'border-[#00E5A0]/40 bg-[#00E5A0]/10 text-[#00E5A0]'
            : 'border-[#FF4466]/40 bg-[#FF4466]/10 text-[#FF4466]'
        }`}>
          {result.message}
        </p>
      )}

      <form onSubmit={submit} className={`${cardPanel} hex-corner mb-8 space-y-3 p-4`}>
        <select value={locationId} onChange={e => setLocationId(e.target.value)} className={inputBase} disabled={loading}>
          {locations.map(l => <option key={l.id} value={l.id}>{l.name} — {l.address}</option>)}
        </select>

        <label className="block text-xs font-medium text-[#9090A8]">Pick-up time</label>
        <input type="datetime-local" value={pickupTime} onChange={e => setPickupTime(e.target.value)}
          className={inputBase} style={{ colorScheme: 'dark' }} />

        <div className="grid grid-cols-2 gap-3">
          <select value={vehicleClass} onChange={e => setVehicleClass(e.target.value as VehicleClass)} className={inputBase}>
            {VEHICLE_CLASSES.map(v => <option key={v} value={v}>{v.replace('_', ' ')}</option>)}
          </select>
          <input type="text" placeholder="Vehicle model" value={vehicleModel}
            onChange={e => setVehicleModel(e.target.value)} className={inputBase} />
        </div>

        <div className="flex gap-2">
          <input type="text" placeholder="Pick-up address (optional)" value={address}
            onChange={e => setAddress(e.target.value)} className={`${inputBase} flex-1`} />
          <button
            type="button"
            onClick={useMyLocation}
            disabled={locating}
            title="Use my current location"
            aria-label="Use my current location"
            className="flex shrink-0 items-center justify-center rounded-lg border border-[#1E1E2D] bg-[#1A1A24] px-3 text-[#9090A8] transition-colors hover:bg-[#1F1F2E] disabled:opacity-40"
          >
            {locating ? <Loader2 className="h-4 w-4 animate-spin" /> : <MapPin className="h-4 w-4" />}
          </button>
        </div>
        <input type="text" placeholder="Notes (optional)" value={notes}
          onChange={e => setNotes(e.target.value)} className={inputBase} />

        {error && <p role="alert" className="text-sm text-[#FF4466]">{error}</p>}

        <p className="flex items-center gap-1.5 text-xs text-[#5A5A72]">
          <MapPin className="h-3.5 w-3.5" /> We use your current GPS location to check you're inside the service area.
        </p>

        <button type="submit" disabled={submitting} className={`${btnPrimary} w-full`}>
          {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : 'Request valet pick-up'}
        </button>
      </form>

      <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-[#9090A8]">My valet requests</h2>
      {requests.length === 0 ? (
        <p className="rounded-2xl border border-[#1E1E2D] bg-[#1A1A24] px-4 py-6 text-center text-sm text-[#5A5A72]">
          No valet requests yet.
        </p>
      ) : (
        <ul className="space-y-3">
          {requests.map(r => (
            <li key={r.id} className={`${cardPanel} hex-corner p-4`}>
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="font-semibold text-[#E8E8F0]">{r.vehicleModel ?? 'Vehicle'}</p>
                  <p className="mt-0.5 text-sm text-[#9090A8]">{r.locationName}</p>
                  <p className="mt-1 text-xs text-[#5A5A72]">Pick-up {fmtSlot(r.pickupTime)}</p>
                  {r.distanceKm != null && r.radiusKm != null && (
                    <p className="mt-0.5 text-xs text-[#5A5A72]">
                      {r.distanceKm.toFixed(1)} km away · limit {r.radiusKm.toFixed(0)} km
                    </p>
                  )}
                </div>
                <span className={statusBadge(r.status)}>{r.status}</span>
              </div>
              {(r.status === 'PENDING' || r.status === 'ACCEPTED') && (
                <div className="mt-3 border-t border-[#1E1E2D] pt-3">
                  <button
                    type="button"
                    onClick={() => cancelRequest(r.id)}
                    disabled={cancelling === r.id}
                    className="min-h-[40px] rounded-lg border border-[#FF4466]/40 px-4 py-2 text-sm font-medium text-[#FF4466] transition-colors hover:bg-[#FF4466]/10 disabled:opacity-40"
                  >
                    {cancelling === r.id ? 'Cancelling…' : 'Cancel request'}
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};
