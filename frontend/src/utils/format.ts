import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import utc from 'dayjs/plugin/utc';

dayjs.extend(relativeTime);
dayjs.extend(utc);

export { dayjs };

export const DATETIME_FMT = 'YYYY-MM-DD HH:mm:ss';
export const DATE_FMT = 'YYYY-MM-DD';

export function fmtDateTime(value: string | number | Date | null | undefined, fallback = '—'): string {
  if (value === null || value === undefined || value === '') return fallback;
  const d = dayjs(value);
  return d.isValid() ? d.format(DATETIME_FMT) : String(value);
}

/** `05 Oct 11:51` — for dense tables where the year is obvious. */
export function fmtShort(value: string | null | undefined, fallback = '—'): string {
  if (!value) return fallback;
  const d = dayjs(value);
  return d.isValid() ? d.format('DD MMM HH:mm') : value;
}

export function fmtDate(value: string | null | undefined, fallback = '—'): string {
  if (!value) return fallback;
  const d = dayjs(value);
  return d.isValid() ? d.format(DATE_FMT) : value;
}

export function fmtRelative(value: string | null | undefined): string {
  if (!value) return '—';
  const d = dayjs(value);
  return d.isValid() ? d.fromNow() : value;
}

export function fmtNumber(n: number | null | undefined, digits = 0): string {
  if (n === null || n === undefined || Number.isNaN(n)) return '—';
  return n.toLocaleString(undefined, { maximumFractionDigits: digits, minimumFractionDigits: digits });
}

export function fmtPct(n: number | null | undefined): string {
  if (n === null || n === undefined || Number.isNaN(n)) return '—';
  return `${n.toFixed(1)}%`;
}

export function fmtCoord(n: number | null | undefined): string {
  return n === null || n === undefined ? '—' : n.toFixed(5);
}

/** Convert a `datetime-local` input value to ISO-8601 with offset. */
export function localInputToIso(v: string): string | null {
  if (!v) return null;
  const d = dayjs(v);
  return d.isValid() ? d.format('YYYY-MM-DDTHH:mm:ssZ') : null;
}

/** Convert ISO to a value usable in `datetime-local`. */
export function isoToLocalInput(v: string | null | undefined): string {
  if (!v) return '';
  const d = dayjs(v);
  return d.isValid() ? d.format('YYYY-MM-DDTHH:mm') : '';
}

export function titleCase(code: string): string {
  return code
    .toLowerCase()
    .split(/[_\s-]+/)
    .map((w) => (w ? w[0].toUpperCase() + w.slice(1) : w))
    .join(' ');
}
