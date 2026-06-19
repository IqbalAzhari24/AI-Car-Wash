import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../../api/api';

// ─── Domain types ─────────────────────────────────────────────────────────────

type VehicleClass = 'MOTORCYCLE' | 'COMPACT' | 'SEDAN' | 'SUV_LUXURY' | 'MPV_LARGE';

interface SlotDto {
  slotTime: string;   // ISO-8601 from Spring (e.g. "2024-01-15T09:00:00")
  available: boolean;
  bookedCount: number;
  maxLimit: number;
}

interface ServiceDto {
  id: string;
  name: string;
  description: string;
  price: number;
  durationMinutes: number;
  vehicleSizeMultiplier: number;
}

interface BookingCreatedDto {
  id: string;
}

interface WizardState {
  date: string;              // YYYY-MM-DD
  slotTime: string | null;   // ISO-8601 datetime
  vehicleClass: VehicleClass | null;
  vehicleModel: string;
  serviceId: string | null;
  serviceName: string;
  servicePrice: number;
  serviceDuration: number;
}

// ─── Constants ────────────────────────────────────────────────────────────────

const VEHICLE_CLASSES: { value: VehicleClass; label: string; note: string }[] = [
  { value: 'MOTORCYCLE', label: 'Motorcycle',   note: '1 slot' },
  { value: 'COMPACT',    label: 'Compact',      note: '1 slot' },
  { value: 'SEDAN',      label: 'Sedan',        note: '1 slot' },
  { value: 'SUV_LUXURY', label: 'SUV / Luxury', note: '2 slots' },
  { value: 'MPV_LARGE',  label: 'MPV / Large',  note: '3 slots' },
];

const STEPS = ['Select Slot', 'Choose Service', 'Confirm'];

// ─── Utilities ────────────────────────────────────────────────────────────────

function todayStr(): string {
  return new Date().toISOString().split('T')[0];
}

/** "2024-01-15T09:00:00" → "09:00" */
function fmtTime(iso: string): string {
  return iso.split('T')[1]?.substring(0, 5) ?? iso;
}

/** "2024-01-15" → "Mon, 15 Jan 2024" */
function fmtDate(d: string): string {
  try {
    return new Date(d + 'T00:00:00').toLocaleDateString('en-MY', {
      weekday: 'short', day: 'numeric', month: 'short', year: 'numeric',
    });
  } catch {
    return d;
  }
}

// ─── Progress indicator ───────────────────────────────────────────────────────

const ProgressIndicator: React.FC<{ step: number }> = ({ step }) => (
  <div className="flex items-center px-6 py-4">
    {STEPS.map((label, i) => (
      <React.Fragment key={i}>
        <div className="flex flex-col items-center gap-1">
          <div
            className={[
              'flex h-8 w-8 items-center justify-center rounded-full text-xs font-semibold transition-all duration-200',
              i < step
                ? 'bg-[#00F0FF] text-[#0D0D11]'
                : i === step
                ? 'border-2 border-[#00F0FF] text-[#00F0FF]'
                : 'border border-[#1E1E2D] text-[#5A5A72]',
            ].join(' ')}
          >
            {i < step ? '✓' : i + 1}
          </div>
          <span
            className={[
              'hidden text-[10px] sm:block whitespace-nowrap',
              i === step ? 'text-[#00F0FF]' : i < step ? 'text-[#9090A8]' : 'text-[#5A5A72]',
            ].join(' ')}
          >
            {label}
          </span>
        </div>
        {i < STEPS.length - 1 && (
          <div
            className={[
              'h-px flex-1 mx-2 transition-colors duration-300',
              i < step ? 'bg-[#00F0FF]/40' : 'bg-[#1E1E2D]',
            ].join(' ')}
          />
        )}
      </React.Fragment>
    ))}
  </div>
);

// ─── Shared button styles ─────────────────────────────────────────────────────

const primaryBtn =
  'w-full rounded-xl bg-[#00F0FF] px-4 py-3.5 text-sm font-semibold text-[#0D0D11] ' +
  'transition-all duration-150 hover:bg-[#00B8C4] disabled:cursor-not-allowed disabled:opacity-40';

const secondaryBtn =
  'rounded-xl border border-[#1E1E2D] bg-[#1A1A24] px-4 py-3.5 text-sm font-medium ' +
  'text-[#9090A8] transition-colors hover:bg-[#1F1F2E] disabled:opacity-40';

// ─── Step 1: Slot & vehicle ───────────────────────────────────────────────────

interface Step1Props {
  state: WizardState;
  onChange: (s: WizardState) => void;
  onNext: () => void;
}

const StepSlot: React.FC<Step1Props> = ({ state, onChange, onNext }) => {
  const [slots, setSlots]     = useState<SlotDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState<string | null>(null);

  useEffect(() => {
    if (!state.date) return;
    setLoading(true);
    setError(null);
    api
      .get<SlotDto[]>(`/v1/slots?date=${state.date}`)
      .then(r => setSlots(r.data))
      .catch(() => setError('Could not load slots. Please try again.'))
      .finally(() => setLoading(false));
  }, [state.date]);

  const canContinue =
    state.slotTime !== null &&
    state.vehicleClass !== null &&
    state.vehicleModel.trim().length > 0;

  return (
    <div className="space-y-6 px-4 py-2 pb-8">
      {/* Date */}
      <div className="space-y-1.5">
        <label htmlFor="book-date" className="block text-sm font-medium text-[#E8E8F0]">
          Date
        </label>
        <input
          id="book-date"
          type="date"
          min={todayStr()}
          value={state.date}
          onChange={e => onChange({ ...state, date: e.target.value, slotTime: null })}
          className="w-full rounded-lg border border-[#1E1E2D] bg-[#1A1A24] px-4 py-3 text-[#E8E8F0] focus:outline-none focus:border-[#00F0FF] transition-colors duration-150"
          style={{ colorScheme: 'dark', minHeight: '44px' }}
        />
      </div>

      {/* Slot grid */}
      <div className="space-y-2">
        <p className="text-sm font-medium text-[#E8E8F0]">Available slots</p>

        {loading && (
          <div className="grid grid-cols-4 gap-2 sm:grid-cols-6">
            {Array.from({ length: 12 }).map((_, i) => (
              <div key={i} className="h-11 animate-pulse rounded-lg bg-[#1A1A24]" />
            ))}
          </div>
        )}

        {!loading && error && (
          <p role="alert" className="text-sm text-[#FF4466]">{error}</p>
        )}

        {!loading && !error && slots.length === 0 && state.date && (
          <div className="rounded-xl border border-[#1E1E2D] bg-[#1A1A24] px-4 py-8 text-center">
            <p className="text-sm text-[#5A5A72]">No slots available for this date.</p>
            <p className="mt-1 text-xs text-[#5A5A72]">Try a different date.</p>
          </div>
        )}

        {!loading && slots.length > 0 && (
          <div className="grid grid-cols-4 gap-2 sm:grid-cols-6">
            {slots.map(slot => {
              const isSelected = state.slotTime === slot.slotTime;
              return (
                <button
                  key={slot.slotTime}
                  type="button"
                  disabled={!slot.available}
                  aria-pressed={isSelected}
                  onClick={() => onChange({ ...state, slotTime: slot.slotTime })}
                  className={[
                    'flex min-h-[44px] items-center justify-center rounded-lg text-xs font-medium transition-all duration-150',
                    slot.available
                      ? isSelected
                        ? 'hex-border-active bg-[#00F0FF]/10 text-[#00F0FF]'
                        : 'hex-border bg-[#1A1A24] text-[#E8E8F0] hover:bg-[#1F1F2E]'
                      : 'cursor-not-allowed border border-[#1E1E2D] bg-[#13131A] text-[#5A5A72] line-through',
                  ].join(' ')}
                >
                  {fmtTime(slot.slotTime)}
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Vehicle class */}
      <div className="space-y-2">
        <p className="text-sm font-medium text-[#E8E8F0]">Vehicle type</p>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
          {VEHICLE_CLASSES.map(vc => {
            const isSelected = state.vehicleClass === vc.value;
            return (
              <button
                key={vc.value}
                type="button"
                aria-pressed={isSelected}
                onClick={() => onChange({ ...state, vehicleClass: vc.value })}
                className={[
                  'flex min-h-[60px] flex-col items-start justify-center rounded-xl px-3 py-2.5 text-left transition-all duration-150',
                  isSelected
                    ? 'hex-border-active bg-[#00F0FF]/10'
                    : 'hex-border bg-[#1A1A24] hover:bg-[#1F1F2E]',
                ].join(' ')}
              >
                <span className={['text-sm font-medium', isSelected ? 'text-[#00F0FF]' : 'text-[#E8E8F0]'].join(' ')}>
                  {vc.label}
                </span>
                <span className="mt-0.5 text-xs text-[#5A5A72]">{vc.note}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Vehicle model */}
      <div className="space-y-1.5">
        <label htmlFor="vehicle-model" className="block text-sm font-medium text-[#E8E8F0]">
          Vehicle model <span className="text-[#FF4466]" aria-hidden="true">*</span>
        </label>
        <input
          id="vehicle-model"
          type="text"
          autoComplete="off"
          placeholder="e.g. Proton Saga, Toyota Vios"
          value={state.vehicleModel}
          onChange={e => onChange({ ...state, vehicleModel: e.target.value })}
          className="w-full rounded-lg border border-[#1E1E2D] bg-[#1A1A24] px-4 py-3 text-[#E8E8F0] placeholder:text-[#5A5A72] focus:outline-none focus:border-[#00F0FF] transition-colors duration-150"
          style={{ minHeight: '44px' }}
        />
      </div>

      <button
        type="button"
        disabled={!canContinue}
        onClick={onNext}
        className={primaryBtn}
        style={{ minHeight: '48px' }}
      >
        Continue to service →
      </button>
    </div>
  );
};

// ─── Step 2: Service ──────────────────────────────────────────────────────────

interface Step2Props {
  state: WizardState;
  onChange: (s: WizardState) => void;
  onNext: () => void;
  onBack: () => void;
}

const StepService: React.FC<Step2Props> = ({ state, onChange, onNext, onBack }) => {
  const [services, setServices] = useState<ServiceDto[]>([]);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState<string | null>(null);

  useEffect(() => {
    api
      .get<ServiceDto[]>('/v1/services')
      .then(r => setServices(r.data))
      .catch(() => setError('Could not load services. Please try again.'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-4 px-4 py-2 pb-8">
      <p className="text-sm text-[#9090A8]">
        Pick a wash package for your{' '}
        <span className="text-[#E8E8F0]">
          {state.vehicleClass?.replace('_', ' ').toLowerCase() ?? 'vehicle'}
        </span>.
      </p>

      {loading && (
        <div className="space-y-3">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-24 animate-pulse rounded-xl bg-[#1A1A24]" />
          ))}
        </div>
      )}

      {!loading && error && (
        <p role="alert" className="text-sm text-[#FF4466]">{error}</p>
      )}

      {!loading && services.length === 0 && !error && (
        <div className="rounded-xl border border-[#1E1E2D] bg-[#1A1A24] px-4 py-8 text-center">
          <p className="text-sm text-[#5A5A72]">No services available right now.</p>
        </div>
      )}

      {!loading && services.length > 0 && (
        <div className="space-y-3">
          {services.map(svc => {
            const isSelected = state.serviceId === svc.id;
            return (
              <button
                key={svc.id}
                type="button"
                aria-pressed={isSelected}
                onClick={() =>
                  onChange({
                    ...state,
                    serviceId:       svc.id,
                    serviceName:     svc.name,
                    servicePrice:    svc.price,
                    serviceDuration: svc.durationMinutes,
                  })
                }
                className={[
                  'hex-corner w-full rounded-xl p-4 text-left transition-all duration-150',
                  isSelected
                    ? 'hex-border-active'
                    : 'hex-border bg-[#1A1A24] hover:bg-[#1F1F2E]',
                ].join(' ')}
                style={isSelected ? { background: 'rgba(0,240,255,0.06)' } : {}}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className={['font-semibold', isSelected ? 'text-[#00F0FF]' : 'text-[#E8E8F0]'].join(' ')}>
                      {svc.name}
                    </p>
                    <p className="mt-0.5 text-sm text-[#9090A8] line-clamp-2">{svc.description}</p>
                    <p className="mt-1 text-xs text-[#5A5A72]">{svc.durationMinutes} min</p>
                  </div>
                  <div className="shrink-0 text-right">
                    <p className={['text-base font-semibold font-mono', isSelected ? 'text-[#00F0FF]' : 'text-[#E8E8F0]'].join(' ')}>
                      RM {Number(svc.price).toFixed(2)}
                    </p>
                    {isSelected && (
                      <span className="mt-1 inline-block rounded-full bg-[#00F0FF]/10 px-2 py-0.5 text-[10px] font-medium text-[#00F0FF]">
                        Selected
                      </span>
                    )}
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      )}

      <div className="flex gap-3 pt-2">
        <button
          type="button"
          onClick={onBack}
          className={`flex-1 ${secondaryBtn}`}
          style={{ minHeight: '48px' }}
        >
          ← Back
        </button>
        <button
          type="button"
          disabled={!state.serviceId}
          onClick={onNext}
          className={`flex-[2] ${primaryBtn}`}
          style={{ minHeight: '48px' }}
        >
          Review booking →
        </button>
      </div>
    </div>
  );
};

// ─── Step 3: Confirm ──────────────────────────────────────────────────────────

interface Step3Props {
  state: WizardState;
  onBack: () => void;
}

const StepConfirm: React.FC<Step3Props> = ({ state, onBack }) => {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError]           = useState<string | null>(null);

  async function handleConfirm() {
    if (submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      const res = await api.post<BookingCreatedDto>('/v1/bookings', {
        serviceId:    state.serviceId,
        slotTime:     state.slotTime,
        vehicleClass: state.vehicleClass,
        vehicleModel: state.vehicleModel,
      });
      navigate(`/checkout/${res.data.id}`);
    } catch (e: unknown) {
      const axiosErr = e as { response?: { data?: { message?: string } } };
      setError(axiosErr.response?.data?.message ?? 'Booking failed. Please try again.');
      setSubmitting(false);
    }
  }

  const vehicleLabel = VEHICLE_CLASSES.find(v => v.value === state.vehicleClass)?.label ?? state.vehicleClass ?? '';

  return (
    <div className="space-y-4 px-4 py-2 pb-8">
      {/* Summary card */}
      <div className="hex-corner hex-border rounded-xl bg-[#1A1A24] p-5">
        <p className="mb-3 text-[11px] font-semibold uppercase tracking-widest text-[#5A5A72]">
          Booking Summary
        </p>
        <dl className="space-y-3 text-sm">
          <div className="flex justify-between gap-2">
            <dt className="text-[#9090A8]">Date</dt>
            <dd className="text-right font-medium text-[#E8E8F0]">{fmtDate(state.date)}</dd>
          </div>

          <div className="flex justify-between gap-2">
            <dt className="text-[#9090A8]">Time</dt>
            <dd className="font-mono font-semibold text-[#00F0FF]">
              {state.slotTime ? fmtTime(state.slotTime) : '—'}
            </dd>
          </div>

          <div className="h-px bg-[#1E1E2D]" />

          <div className="flex justify-between gap-2">
            <dt className="text-[#9090A8]">Vehicle</dt>
            <dd className="text-right font-medium text-[#E8E8F0]">
              {state.vehicleModel}
              <span className="ml-1.5 text-xs text-[#5A5A72]">({vehicleLabel})</span>
            </dd>
          </div>

          <div className="h-px bg-[#1E1E2D]" />

          <div className="flex justify-between gap-2">
            <dt className="text-[#9090A8]">Service</dt>
            <dd className="text-right font-medium text-[#E8E8F0]">{state.serviceName}</dd>
          </div>

          <div className="flex justify-between gap-2">
            <dt className="text-[#9090A8]">Duration</dt>
            <dd className="text-[#E8E8F0]">{state.serviceDuration} min</dd>
          </div>

          <div className="h-px bg-[#1E1E2D]" />

          <div className="flex justify-between gap-2 pt-0.5">
            <dt className="font-semibold text-[#E8E8F0]">Est. price</dt>
            <dd className="font-bold font-mono text-lg text-[#00F0FF]">
              RM {Number(state.servicePrice).toFixed(2)}
            </dd>
          </div>
        </dl>
        <p className="mt-3 text-[11px] text-[#5A5A72]">
          * Final amount confirmed at payment step.
        </p>
      </div>

      {/* Error */}
      {error && (
        <div
          role="alert"
          className="rounded-lg border px-4 py-3 text-sm text-[#FF4466]"
          style={{
            borderColor: 'rgba(255,68,102,0.40)',
            background: 'rgba(255,68,102,0.06)',
          }}
        >
          {error}
        </div>
      )}

      <div className="flex gap-3 pt-1">
        <button
          type="button"
          onClick={onBack}
          disabled={submitting}
          className={`flex-1 ${secondaryBtn}`}
          style={{ minHeight: '48px' }}
        >
          ← Back
        </button>
        <button
          type="button"
          disabled={submitting}
          onClick={handleConfirm}
          className={`flex-[2] ${primaryBtn}`}
          style={{ minHeight: '48px' }}
        >
          {submitting ? 'Creating booking…' : 'Confirm booking →'}
        </button>
      </div>

      <p className="text-center text-xs text-[#5A5A72]">
        You will choose your payment method on the next screen.
      </p>
    </div>
  );
};

// ─── Main wizard ──────────────────────────────────────────────────────────────

const STEP_HEADINGS = ['Pick a slot', 'Choose service', 'Confirm booking'];

export const BookingFlow: React.FC = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [wizard, setWizard] = useState<WizardState>({
    date:            todayStr(),
    slotTime:        null,
    vehicleClass:    null,
    vehicleModel:    '',
    serviceId:       null,
    serviceName:     '',
    servicePrice:    0,
    serviceDuration: 0,
  });

  return (
    <div
      className="hex-grid-dense min-h-dvh"
      style={{ backgroundColor: '#0D0D11' }}
    >
      {/* Header */}
      <header className="flex items-center justify-between px-4 pt-4 pb-1">
        <button
          type="button"
          onClick={() => (step === 0 ? navigate(-1) : setStep(s => s - 1))}
          className="flex h-10 w-10 items-center justify-center rounded-lg border border-[#1E1E2D] text-[#9090A8] transition-colors hover:bg-[#1A1A24]"
          aria-label="Go back"
        >
          <svg viewBox="0 0 20 20" fill="currentColor" className="h-5 w-5" aria-hidden="true">
            <path
              fillRule="evenodd"
              d="M11.78 5.22a.75.75 0 0 1 0 1.06L8.06 10l3.72 3.72a.75.75 0 1 1-1.06 1.06l-4.25-4.25a.75.75 0 0 1 0-1.06l4.25-4.25a.75.75 0 0 1 1.06 0Z"
              clipRule="evenodd"
            />
          </svg>
        </button>

        <h1 className="font-display text-base font-semibold text-[#E8E8F0]">
          {STEP_HEADINGS[step]}
        </h1>

        {/* spacer to keep heading centred */}
        <div className="h-10 w-10" aria-hidden="true" />
      </header>

      {/* Progress */}
      <ProgressIndicator step={step} />

      {/* Step content */}
      <main>
        {step === 0 && (
          <StepSlot
            state={wizard}
            onChange={setWizard}
            onNext={() => setStep(1)}
          />
        )}
        {step === 1 && (
          <StepService
            state={wizard}
            onChange={setWizard}
            onNext={() => setStep(2)}
            onBack={() => setStep(0)}
          />
        )}
        {step === 2 && (
          <StepConfirm
            state={wizard}
            onBack={() => setStep(1)}
          />
        )}
      </main>
    </div>
  );
};
