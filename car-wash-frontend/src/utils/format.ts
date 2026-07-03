/** Shared en-MY date formatters. All return the raw input (or '—' for null) on parse failure. */

function fmt(iso: string, opts: Intl.DateTimeFormatOptions): string {
  try {
    return new Date(iso).toLocaleString('en-MY', opts);
  } catch {
    return iso;
  }
}

/** "2024-01-15T09:00:00" → "15 Jan, 09:00" */
export function fmtSlot(iso: string | null | undefined): string {
  if (!iso) return '—';
  return fmt(iso, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
}

/** "2024-01-15T09:00:00" → "Mon, 15 Jan 2024, 09:00" */
export function fmtDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  return fmt(iso, {
    weekday: 'short', day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

/** "2024-01-15T09:00:00" → "15 Jan 2024" */
export function fmtDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  return fmt(iso, { day: 'numeric', month: 'short', year: 'numeric' });
}
