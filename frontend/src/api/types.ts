/**
 * TypeScript mirrors of the SPEC §5 / §7.5 contract.
 * Field names are contractual — do not rename.
 */

export interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  timestamp: string;
  data: T;
}

// ---------- enums ----------
export type AdminRole = 'ADMIN' | 'VIEWER';
export type UserStatus = 'ACTIVE' | 'DISABLED';
export type Severity = 'INFO' | 'WARNING' | 'CRITICAL';
export type NotificationStatus = 'PENDING' | 'SENT' | 'DELIVERED' | 'READ' | 'FAILED' | 'NO_DEVICE';
export type DeliveryStatus = 'QUEUED' | 'SENT' | 'DELIVERED' | 'FAILED' | 'INVALID_TOKEN';
export type DiscardReason = 'NO_USER' | 'INVALID_PHONE' | 'USER_DISABLED';

export const SEVERITIES: Severity[] = ['INFO', 'WARNING', 'CRITICAL'];
export const NOTIFICATION_STATUSES: NotificationStatus[] = ['PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED', 'NO_DEVICE'];
export const DELIVERY_STATUSES: DeliveryStatus[] = ['QUEUED', 'SENT', 'DELIVERED', 'FAILED', 'INVALID_TOKEN'];
export const DISCARD_REASONS: DiscardReason[] = ['NO_USER', 'INVALID_PHONE', 'USER_DISABLED'];
export const USER_STATUSES: UserStatus[] = ['ACTIVE', 'DISABLED'];
export const ADMIN_ROLES: AdminRole[] = ['ADMIN', 'VIEWER'];

// ---------- auth ----------
export interface AdminPrincipal {
  id: number;
  username: string;
  role: AdminRole;
  active: boolean;
  createdAt: string | null;
  lastLoginAt: string | null;
}

export interface AdminAuthResponse {
  accessToken: string;
  accessExpiresInSec: number;
  refreshToken: string;
  admin: AdminPrincipal;
}

// ---------- overview ----------
export interface OverviewPerDay {
  date: string;
  alerts: number;
  delivered: number;
  read: number;
  failed: number;
}
export interface OverviewByType {
  code: string;
  label: string;
  count: number;
}
export interface Overview {
  alertsTotal: number;
  notificationsTotal: number;
  sent: number;
  delivered: number;
  read: number;
  failed: number;
  noDevice: number;
  discarded: number;
  usersTotal: number;
  usersNew: number;
  devicesActive: number;
  devicesSilent: number;
  deliveryRatePct: number;
  readRatePct: number;
  perDay: OverviewPerDay[];
  byType: OverviewByType[];
}

/** `GET /dispatch/status` */
export interface DispatchStatus {
  deliveriesByStatus: Partial<Record<DeliveryStatus, number>>;
  dueNow: number;
  inFlight: number;
  notificationsByStatus: Partial<Record<NotificationStatus, number>>;
  firebaseEnabled: boolean;
}

// ---------- generic query ----------
export type FilterOp =
  | 'eq'
  | 'neq'
  | 'contains'
  | 'startsWith'
  | 'gt'
  | 'gte'
  | 'lt'
  | 'lte'
  | 'between'
  | 'in'
  | 'isNull'
  | 'notNull';

export type FilterValue = string | number | boolean | null | Array<string | number | boolean | null>;

export interface FilterClause {
  field: string;
  op: FilterOp;
  value?: FilterValue;
}
export type FilterLogic = 'AND' | 'OR';
export interface SortClause {
  field: string;
  dir: 'asc' | 'desc';
}
export interface FilterRequest {
  filters: FilterClause[];
  logic: FilterLogic;
  sort: SortClause[];
  page: number;
  size: number;
  search?: string;
}
export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

export type EntityName =
  | 'configurations'
  | 'alerts'
  | 'notifications'
  | 'deliveries'
  | 'users'
  | 'devices'
  | 'discards'
  | 'sources'
  | 'alert-types'
  | 'admins';

// ---------- rows ----------
export interface AlertRow {
  id: number;
  sourceId?: number | null;
  sourceName?: string | null;
  sourceAlertId?: string | null;
  uid: string;
  alertTypeCode: string;
  severity?: Severity | null;
  message: string | null;
  lat: number | null;
  lon: number | null;
  dbdt: string | null;
  sdt: string;
  extra?: Record<string, unknown> | null;
  recipientsTotal: number | null;
  recipientsAccepted: number | null;
  recipientsDiscarded: number | null;
}

export interface UserRow {
  id: number;
  phoneE164: string;
  displayName: string | null;
  status: UserStatus;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface DeviceRow {
  id: number;
  userId: number;
  userPhone: string | null;
  deviceId: string;
  hasFcmToken: boolean;
  manufacturer: string | null;
  model: string | null;
  osVersion: string | null;
  appVersion: string | null;
  active: boolean;
  lastSeenAt: string | null;
  batteryOptIgnored: boolean | null;
  unusedRestrictionsDisabled: boolean | null;
  notificationsEnabled: boolean | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface NotificationRow {
  id: number;
  alertId: number;
  uid: string;
  alertTypeCode: string;
  alertMessage: string | null;
  alertSdt: string | null;
  userId: number;
  userPhone: string;
  userDisplayName: string | null;
  status: NotificationStatus;
  sentAt: string | null;
  deliveredAt: string | null;
  readAt: string | null;
  createdAt: string;
}

export interface DeliveryRow {
  id: number;
  notificationId: number;
  notificationStatus: NotificationStatus;
  alertId: number;
  uid: string;
  userId: number;
  userPhone: string;
  deviceId: number;
  deviceUuid: string;
  deviceModel: string | null;
  deviceManufacturer: string | null;
  status: DeliveryStatus;
  attempts: number;
  fcmMessageId: string | null;
  lastError: string | null;
  queuedAt: string | null;
  sentAt: string | null;
  deliveredAt: string | null;
  nextAttemptAt: string | null;
}

export interface DiscardRow {
  id: number;
  alertId: number;
  uid: string;
  alertTypeCode: string;
  rawPhone: string | null;
  phoneE164: string | null;
  reason: DiscardReason;
  createdAt: string;
}

export interface ConfigurationRow {
  id: number;
  sourceId: string | null;
  phoneRaw: string;
  phoneE164: string;
  uid: string;
  alertType: string;
  dbdt: string | null;
  crtBy: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SourceRow {
  id: number;
  name: string;
  apiKeyPrefix: string | null;
  active: boolean;
  createdAt: string;
  lastUsedAt: string | null;
}

export interface AlertTypeRow {
  code: string;
  label: string;
  severity: Severity;
  icon: string | null;
  color: string | null;
  channelId: string | null;
  active: boolean;
  sortOrder: number | null;
}

export interface AdminRow {
  id: number;
  username: string;
  role: AdminRole;
  active: boolean;
  createdAt: string;
  lastLoginAt: string | null;
}

// ---------- detail payloads ----------
export interface RecipientDelivery {
  id: number;
  deviceId: number;
  deviceUuid: string;
  manufacturer: string | null;
  model: string | null;
  deviceActive: boolean;
  status: DeliveryStatus;
  attempts: number;
  fcmMessageId: string | null;
  lastError: string | null;
  queuedAt: string | null;
  sentAt: string | null;
  deliveredAt: string | null;
  nextAttemptAt: string | null;
}
export interface AlertRecipient {
  notificationId: number;
  userId: number;
  phone: string;
  displayName: string | null;
  status: NotificationStatus;
  sentAt: string | null;
  deliveredAt: string | null;
  readAt: string | null;
  createdAt: string;
  deliveries: RecipientDelivery[];
}
export interface AlertDiscard {
  id: number;
  rawPhone: string | null;
  phoneE164: string | null;
  reason: DiscardReason;
  createdAt: string;
}
export interface AlertDetail {
  alert: AlertRow;
  recipients: AlertRecipient[];
  discards: AlertDiscard[];
}

export interface UserDetail {
  user: UserRow;
  devices: DeviceRow[];
  recentNotifications: NotificationRow[];
}

export interface SourceWithKey {
  source: SourceRow;
  apiKey: string;
}

// ---------- misc ----------
export interface SqlResult {
  columns: string[];
  rows: unknown[][];
  truncated: boolean;
  elapsedMs: number;
}

export interface Settings {
  retention_days: number;
  max_devices_per_user: number;
  dispatch_max_attempts: number;
  /** Comma-separated keywords: a newly discovered alert type whose code contains one defaults to CRITICAL. */
  severity_keywords_critical: string;
  /** Same for WARNING. Anything else defaults to INFO. */
  severity_keywords_warning: string;
}
/** Wire format of GET/PUT /settings: a flat map of string values. */
export type SettingsWire = Record<string, string>;
