import { del, get, http, noAuthRetry, patch, post, put } from './client';
import type {
  AdminAuthResponse,
  AdminPrincipal,
  AdminRole,
  AdminRow,
  AlertDetail,
  AlertTypeRow,
  ConfigurationRow,
  DispatchStatus,
  EntityName,
  FilterRequest,
  Overview,
  PageResponse,
  Settings,
  SettingsWire,
  SourceRow,
  SourceWithKey,
  SqlResult,
  UserDetail,
  UserRow,
  UserStatus,
} from './types';

// ---------- auth ----------
export const authApi = {
  login: (username: string, password: string) =>
    post<AdminAuthResponse>('/auth/login', { username, password }, noAuthRetry),
  me: () => get<AdminPrincipal>('/me', noAuthRetry),
};

// ---------- overview ----------
export const overviewApi = {
  get: (from: string, to: string) => get<Overview>('/overview', { params: { from, to } }),
  dispatchStatus: () => get<DispatchStatus>('/dispatch/status'),
};

// ---------- generic query / export ----------
export const queryApi = {
  query: <T>(entity: EntityName, body: FilterRequest) => post<PageResponse<T>>(`/query/${entity}`, body),
  exportCsv: async (entity: EntityName, body: FilterRequest): Promise<Blob> => {
    const res = await http.post<Blob>(`/export/${entity}`, body, {
      responseType: 'blob',
      headers: { Accept: 'text/csv' },
      timeout: 120_000,
      _raw: true,
    });
    return res.data;
  },
};

// ---------- alerts ----------
export interface SendAlertInput {
  id?: string;
  uid: string;
  alertType: string;
  message?: string;
  lat?: number | null;
  lon?: number | null;
}
export interface SendAlertResult {
  id: string;
  alertId: number;
  result: 'ACCEPTED' | 'DUPLICATE' | 'NO_CONFIGURATION';
  configuredPhones: number;
  notified: number;
  discarded: number;
  discardedPhones: Array<{ phone: string; reason: string }>;
}
export const alertsApi = {
  detail: (id: number) => get<AlertDetail>(`/alerts/${id}`),
  /** Posts through the same ingest pipeline S3 uses (recipients come from the configurations). */
  send: (body: SendAlertInput) => post<SendAlertResult>('/alerts/send', body),
  resend: (notificationId: number) => post<unknown>(`/notifications/${notificationId}/resend`),
};

// ---------- users ----------
export const usersApi = {
  detail: (id: number) => get<UserDetail>(`/users/${id}`),
  /** Creates the app user with a hashed password; if the phone exists, resets its password. */
  create: (body: { phone: string; password: string; displayName?: string }) => post<UserRow>('/users', body),
  setPassword: (id: number, password: string) => post<unknown>(`/users/${id}/password`, { password }),
  update: (id: number, body: { status?: UserStatus; displayName?: string | null }) =>
    patch<UserRow>(`/users/${id}`, body),
  forceLogout: (id: number) => post<unknown>(`/users/${id}/force-logout`),
  removeDevice: (deviceId: number) => del<unknown>(`/devices/${deviceId}`),
};

// ---------- configurations ----------
export interface ConfigurationInput {
  id?: string;
  phoneNumber: string;
  uid: string;
  alertType?: string;
  crtBy?: string;
}
export const configurationsApi = {
  create: (body: ConfigurationInput) => post<ConfigurationRow>('/configurations', body),
  remove: (id: number) => del<boolean>(`/configurations/${id}`),
};

// ---------- alert types ----------
export const alertTypesApi = {
  list: () => get<AlertTypeRow[]>('/alert-types'),
  create: (body: AlertTypeRow) => post<AlertTypeRow>('/alert-types', body),
  update: (code: string, body: Partial<AlertTypeRow>) => put<AlertTypeRow>(`/alert-types/${encodeURIComponent(code)}`, body),
  remove: (code: string) => del<unknown>(`/alert-types/${encodeURIComponent(code)}`),
};

// ---------- sources ----------
export const sourcesApi = {
  list: () => get<SourceRow[]>('/sources'),
  /** The plaintext key is returned once in `apiKey`. */
  create: (body: { name: string; active?: boolean }) => post<SourceWithKey>('/sources', body),
  rotateKey: (id: number) => post<SourceWithKey>(`/sources/${id}/rotate-key`),
  setActive: (id: number, active: boolean) => patch<SourceRow>(`/sources/${id}`, { active }),
};

// ---------- admins ----------
export const adminsApi = {
  list: () => get<AdminRow[]>('/admins'),
  create: (body: { username: string; password: string; role: AdminRole; active?: boolean }) =>
    post<AdminRow>('/admins', body),
  update: (id: number, body: { role?: AdminRole; active?: boolean; password?: string }) =>
    patch<AdminRow>(`/admins/${id}`, body),
  remove: (id: number) => del<unknown>(`/admins/${id}`),
};

// ---------- settings ----------
const SETTING_DEFAULTS: Settings = {
  retention_days: 90,
  max_devices_per_user: 5,
  dispatch_max_attempts: 5,
  severity_keywords_critical: '',
  severity_keywords_warning: '',
};
function fromWire(raw: SettingsWire): Settings {
  const num = (k: 'retention_days' | 'max_devices_per_user' | 'dispatch_max_attempts') => {
    const n = Number(raw[k]);
    return Number.isFinite(n) && raw[k] !== undefined && raw[k] !== '' ? n : SETTING_DEFAULTS[k];
  };
  return {
    retention_days: num('retention_days'),
    max_devices_per_user: num('max_devices_per_user'),
    dispatch_max_attempts: num('dispatch_max_attempts'),
    severity_keywords_critical: raw.severity_keywords_critical ?? '',
    severity_keywords_warning: raw.severity_keywords_warning ?? '',
  };
}
function toWire(s: Settings): SettingsWire {
  return Object.fromEntries(Object.entries(s).map(([k, v]) => [k, String(v)]));
}
export const settingsApi = {
  get: async () => fromWire(await get<SettingsWire>('/settings')),
  put: async (s: Settings) => fromWire(await put<SettingsWire>('/settings', toWire(s))),
};

// ---------- sql ----------
export const sqlApi = {
  run: (sql: string) => post<SqlResult>('/sql', { sql }, { timeout: 60_000 }),
};
