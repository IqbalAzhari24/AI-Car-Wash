import React, { useCallback, useEffect, useState } from 'react';
import { PlusCircle } from 'lucide-react';
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
}

interface SlotDto {
  slotTime: string;
  available: boolean;
}

interface ServiceDto {
  id: string;
  name: string;
  price: number;
}

type VehicleClass = 'MOTORCYCLE' | 'COMPACT' | 'SEDAN' | 'SUV_LUXURY' | 'MPV_LARGE';

const VEHICLE_CLASSES: VehicleClass[] = ['MOTORCYCLE', 'COMPACT', 'SEDAN', 'SUV_LUXURY', 'MPV_LARGE'];

function todayStr(): string {
  return new Date().toISOString().split('T')[0];
}

function fmtSlot(iso: string): string {
  try {
    return new Date(iso).toLocaleString('en-MY', {
      day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
    });
  } catch {
    return iso;
  }
}

function fmtTime(iso: string): string {
  return iso.split('T')[1]?.substring(0, 5) ?? iso;
}

// ─── Walk-in form ───────────────────────────────────────────────────────────

const WalkInForm: React.FC<{ onCreated: () => void }> = ({ onCreated }) => {
  const [open, setOpen] = useState(false);
  const [email, setEmail] = useState('');
  const [date, setDate] = useState(todayStr());
  const [slotTime, setSlotTime] = useState('');
  const [serviceId, setServiceId] = useState('');
  const [vehicleClass, setVehicleClass] = useState<VehicleClass>('SEDAN');
  const [vehicleModel, setVehicleModel] = useState('');
  const [slots, setSlots] = useState<SlotDto[]>([]);
  const [services, setServices] = useState<ServiceDto[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    api.get<ServiceDto[]>('/v1/services').then(r => setServices(r.data)).catch(() => {});
  }, [open]);

  useEffect(() => {
    if (!open || !date) return;
    setSlotTime('');
    api.get<SlotDto[]>(`/v1/slots?date=${date}`).then(r => setSlots(r.data)).catch(() => setSlots([]));
  }, [open, date]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setError(null);
    if (!email.trim() || !slotTime || !serviceId || !vehicleModel.trim()) {
      setError('Fill in customer email, slot, service and vehicle model.');
      return;
    }
    setSubmitting(true);
    try {
      await api.post('/v1/bookings', {
        customerEmail: email.trim(),
        serviceId,
        slotTime,
        vehicleClass,
        vehicleModel: vehicleModel.trim(),
      });
      setEmail(''); setVehicleModel(''); setSlotTime('');
      setOpen(false);
      onCreated();
    } catch (e: unknown) {
      const ax = e as { response?: { data?: { message?: string } } };
      setError(ax.response?.data?.message ?? 'Could not create the walk-in booking.');
    } finally {
      setSubmitting(false);
    }
  }

  if (!open) {
    return (
      <button type="button" onClick={() => setOpen(true)} className={`${btnSecondary} mb-6`}>
        <PlusCircle className="h-4 w-4" /> New walk-in booking
      </button>
    );
  }

  return (
    <form onSubmit={submit} className={`${cardPanel} hex-corner mb-6 space-y-3 p-4`}>
      <p className="text-sm font-semibold text-[#E8E8F0]">New walk-in booking</p>

      <input
        type="email" placeholder="Customer email" value={email}
        onChange={e => setEmail(e.target.value)} className={inputBase}
      />

      <div className="grid grid-cols-2 gap-3">
        <input type="date" min={todayStr()} value={date}
          onChange={e => setDate(e.target.value)} className={inputBase} style={{ colorScheme: 'dark' }} />
        <select value={slotTime} onChange={e => setSlotTime(e.target.value)} className={inputBase}>
          <option value="">Select slot…</option>
          {slots.filter(s => s.available).map(s => (
            <option key={s.slotTime} value={s.slotTime}>{fmtTime(s.slotTime)}</option>
          ))}
        </select>
      </div>

      <select value={serviceId} onChange={e => setServiceId(e.target.value)} className={inputBase}>
        <option value="">Select service…</option>
        {services.map(s => (
          <option key={s.id} value={s.id}>{s.name} — RM {Number(s.price).toFixed(2)}</option>
        ))}
      </select>

      <div className="grid grid-cols-2 gap-3">
        <select value={vehicleClass} onChange={e => setVehicleClass(e.target.value as VehicleClass)} className={inputBase}>
          {VEHICLE_CLASSES.map(v => (
            <option key={v} value={v}>{v.replace('_', ' ')}</option>
          ))}
        </select>
        <input type="text" placeholder="Vehicle model" value={vehicleModel}
          onChange={e => setVehicleModel(e.target.value)} className={inputBase} />
      </div>

      {error && <p role="alert" className="text-sm text-[#FF4466]">{error}</p>}

      <div className="flex gap-2 pt-1">
        <button type="button" onClick={() => setOpen(false)} className={`${btnGhost} flex-1`}>Cancel</button>
        <button type="submit" disabled={submitting} className={`${btnPrimary} flex-1`}>
          {submitting ? 'Creating…' : 'Create booking'}
        </button>
      </div>
    </form>
  );
};

// ─── Console ────────────────────────────────────────────────────────────────

export const ClerkConsole: React.FC = () => {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    api
      .get<Booking[]>('/v1/bookings/manage')
      .then(r => setBookings(r.data))
      .catch(() => setError('Could not load bookings. Please try again.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  async function run(id: string, fn: () => Promise<unknown>) {
    if (busyId) return;
    setBusyId(id);
    setActionError(null);
    try {
      await fn();
      load();
    } catch (e: unknown) {
      const ax = e as { response?: { data?: { message?: string } } };
      setActionError(ax.response?.data?.message ?? 'Action failed. Please try again.');
    } finally {
      setBusyId(null);
    }
  }

  const confirmCash = (id: string) => run(id, () => api.post(`/v1/bookings/${id}/checkout`, { method: 'CASH' }));
  const start = (id: string) => run(id, () => api.patch(`/v1/bookings/${id}/status`, { status: 'IN_PROGRESS' }));
  const complete = (id: string) => run(id, () => api.patch(`/v1/bookings/${id}/status`, { status: 'COMPLETED' }));
  const cancel = (id: string) => run(id, () => api.post(`/v1/bookings/${id}/cancel`));

  return (
    <div className="mx-auto w-full max-w-2xl px-4 py-8">
      <header className="mb-6">
        <h1 className="font-display text-2xl font-semibold tracking-tight text-[#E8E8F0]">Clerk Console</h1>
        <p className="mt-1 text-sm text-[#9090A8]">Confirm payments, advance washes, handle walk-ins.</p>
      </header>

      <WalkInForm onCreated={load} />

      {actionError && (
        <p role="alert" className="mb-4 rounded-lg border border-[#FF4466]/40 bg-[#FF4466]/10 px-4 py-3 text-sm text-[#FF4466]">
          {actionError}
        </p>
      )}

      {loading && (
        <div className="space-y-3">
          {[1, 2, 3].map(i => <div key={i} className="h-28 animate-pulse rounded-2xl bg-[#1A1A24]" />)}
        </div>
      )}

      {!loading && error && <p role="alert" className="text-sm text-[#FF4466]">{error}</p>}

      {!loading && !error && bookings.length === 0 && (
        <p className="rounded-2xl border border-[#1E1E2D] bg-[#1A1A24] px-4 py-8 text-center text-sm text-[#5A5A72]">
          No bookings need attention right now.
        </p>
      )}

      {!loading && bookings.length > 0 && (
        <ul className="space-y-3">
          {bookings.map(b => {
            const busy = busyId === b.id;
            return (
              <li key={b.id} className={`${cardPanel} hex-corner p-4`}>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="font-semibold text-[#E8E8F0]">{b.vehicleModel}</p>
                    <p className="mt-0.5 text-sm text-[#9090A8]">
                      {b.serviceName ?? 'Wash'}
                      {b.vehicleClass && (
                        <span className="ml-1.5 text-xs text-[#5A5A72]">({b.vehicleClass.replace('_', ' ').toLowerCase()})</span>
                      )}
                    </p>
                    <p className="mt-1 text-xs text-[#5A5A72]">Slot {fmtSlot(b.slotTime)}</p>
                  </div>
                  <div className="shrink-0 text-right">
                    <span className={statusBadge(b.status)}>{b.status.replace('_', ' ')}</span>
                    <p className="mt-1.5 font-mono text-sm font-semibold text-[#E8E8F0]">RM {Number(b.totalPrice).toFixed(2)}</p>
                  </div>
                </div>

                <div className="mt-4 flex flex-wrap gap-2 border-t border-[#1E1E2D] pt-3">
                  {b.status === 'PENDING' && (
                    <button type="button" disabled={busy} onClick={() => confirmCash(b.id)} className={`${btnPrimary} flex-1`}>
                      {busy ? 'Working…' : 'Confirm cash'}
                    </button>
                  )}
                  {b.status === 'CONFIRMED' && (
                    <button type="button" disabled={busy} onClick={() => start(b.id)} className={`${btnSecondary} flex-1`}>
                      {busy ? 'Working…' : 'Start wash'}
                    </button>
                  )}
                  {b.status === 'IN_PROGRESS' && (
                    <button type="button" disabled={busy} onClick={() => complete(b.id)} className={`${btnPrimary} flex-1`}>
                      {busy ? 'Working…' : 'Mark complete'}
                    </button>
                  )}
                  {(b.status === 'PENDING' || b.status === 'CONFIRMED') && (
                    <button type="button" disabled={busy} onClick={() => cancel(b.id)} className={`${btnGhost} flex-1`}>
                      Cancel
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
