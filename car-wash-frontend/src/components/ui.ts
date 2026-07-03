// Shared Cyber-Honeycomb class strings (CLAUDE_DESIGN.MD v2).
// Plain strings + helpers, not a component layer — keeps the design idiom DRY
// across pages. Tailwind sees these literals via the ./src/**/*.{ts,tsx} glob.
// ponytail: strings, not components — upgrade only if per-instance variation appears.

/** Cyan-fill primary action. Cyan is action-only; dark ink on cyan. */
export const btnPrimary =
  'inline-flex items-center justify-center gap-2 rounded-lg bg-[#00F0FF] px-4 py-2.5 ' +
  'min-h-[44px] text-sm font-semibold text-[#0D0D11] shadow-cyan-glow transition-colors ' +
  'duration-150 hover:bg-[#00B8C4] focus-visible:outline-none focus-visible:ring-2 ' +
  'focus-visible:ring-[#00F0FF] focus-visible:ring-offset-2 focus-visible:ring-offset-[#0D0D11] ' +
  'disabled:cursor-not-allowed disabled:opacity-40';

/** Cyan-outline secondary action. */
export const btnSecondary =
  'inline-flex items-center justify-center gap-2 rounded-lg border border-[#00F0FF] px-4 py-2.5 ' +
  'min-h-[44px] text-sm font-medium text-[#00F0FF] transition-colors duration-150 ' +
  'hover:bg-[#00F0FF] hover:text-[#0D0D11] focus-visible:outline-none focus-visible:ring-2 ' +
  'focus-visible:ring-[#00F0FF] disabled:cursor-not-allowed disabled:opacity-40';

/** Neutral ghost button — dark surface, subtle border. */
export const btnGhost =
  'inline-flex items-center justify-center gap-2 rounded-lg border border-[#2A2A3D] bg-[#13131A] ' +
  'px-4 py-2.5 min-h-[44px] text-sm font-medium text-[#E8E8F0] transition-colors duration-150 ' +
  'hover:bg-[#1F1F2E] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#00F0FF] ' +
  'disabled:cursor-not-allowed disabled:opacity-40';

/** Honeycomb card surface (textured panel). */
export const cardPanel = 'hex-grid hex-border rounded-2xl';

/** Standard dark form input / select / textarea. */
export const inputBase =
  'w-full min-h-[44px] rounded-lg border border-[#2A2A3D] bg-[#13131A] px-4 py-2.5 text-sm ' +
  'text-[#E8E8F0] placeholder:text-[#5A5A72] transition-colors duration-150 ' +
  'focus:border-[#00F0FF] focus:outline-none focus:ring-1 focus:ring-[#00F0FF]';

export interface StatusTone {
  text: string;
  border: string;
  bg: string;
}

/** Maps a booking/payment status to a semantic Cyber-Honeycomb colour trio. */
export function statusClasses(status: string | null | undefined): StatusTone {
  switch ((status ?? '').toUpperCase()) {
    case 'COMPLETED':
    case 'CONFIRMED':
    case 'ACCEPTED':
    case 'PAID':
    case 'SUCCESS':
      return { text: 'text-[#00E5A0]', border: 'border-[#00E5A0]/40', bg: 'bg-[#00E5A0]/10' };
    case 'PENDING':
    case 'IN_PROGRESS':
      return { text: 'text-[#FFB800]', border: 'border-[#FFB800]/40', bg: 'bg-[#FFB800]/10' };
    case 'CANCELLED':
    case 'REJECTED':
    case 'NO_SHOW':
    case 'FAILED':
      return { text: 'text-[#FF4466]', border: 'border-[#FF4466]/40', bg: 'bg-[#FF4466]/10' };
    case 'REFUNDED':
      return { text: 'text-[#7B8CDE]', border: 'border-[#7B8CDE]/40', bg: 'bg-[#7B8CDE]/10' };
    default:
      return { text: 'text-[#9090A8]', border: 'border-[#2A2A3D]', bg: 'bg-[#1A1A24]' };
  }
}

/** Full badge className for a status pill. */
export function statusBadge(status: string | null | undefined): string {
  const t = statusClasses(status);
  return `inline-flex rounded-full border px-2 py-0.5 text-xs font-medium ${t.text} ${t.border} ${t.bg}`;
}
