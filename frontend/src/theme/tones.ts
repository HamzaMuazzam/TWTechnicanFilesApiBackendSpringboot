import type { ToneKey } from './theme';

/**
 * One colour map for every enum shown in the dashboard (SPEC §5), so a
 * status looks the same in a grid cell, a drawer and a KPI tile.
 */
const TONES: Record<string, ToneKey> = {// severity
  INFO: 'INFO',
  WARNING: 'WARNING',
  CRITICAL: 'CRITICAL',
  // notification
  PENDING: 'NEUTRAL',
  SENT: 'INFO',
  DELIVERED: 'SUCCESS',
  READ: 'SUCCESS',
  FAILED: 'CRITICAL',
  NO_DEVICE: 'WARNING',
  // delivery
  QUEUED: 'NEUTRAL',
  INVALID_TOKEN: 'CRITICAL',
  // user
  ACTIVE: 'SUCCESS',
  DISABLED: 'NEUTRAL',
  // discard reasons
  NO_USER: 'WARNING',
  INVALID_PHONE: 'CRITICAL',
  USER_DISABLED: 'NEUTRAL',
  // roles
  ADMIN: 'INFO',
  VIEWER: 'NEUTRAL',
  // booleans
  TRUE: 'SUCCESS',
  FALSE: 'NEUTRAL',
  YES: 'SUCCESS',
  NO: 'NEUTRAL',
};

export function toneFor(value: string | null | undefined): ToneKey {
  if (!value) return 'NEUTRAL';
  return TONES[value.toUpperCase()] ?? 'NEUTRAL';
}
