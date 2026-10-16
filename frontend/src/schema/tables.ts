/** SPEC §5 schema, hard-coded as a reference for the SQL console (no API needed). */
export interface TableRef {
  name: string;
  columns: string[];
  note?: string;
}

export const SCHEMA: TableRef[] = [
  { name: 'app_user', columns: ['id', 'phone_e164', 'display_name', 'status', 'created_at', 'last_login_at'], note: 'status: ACTIVE | DISABLED' },
  {
    name: 'user_device',
    columns: ['id', 'user_id', 'device_id', 'fcm_token', 'manufacturer', 'model', 'os_version', 'app_version', 'active', 'last_seen_at', 'battery_opt_ignored', 'unused_restrictions_disabled', 'notifications_enabled', 'created_at', 'updated_at'],
    note: 'unique(user_id, device_id)',
  },
  { name: 'refresh_token', columns: ['id', 'subject_type', 'subject_id', 'device_id', 'token_hash', 'expires_at', 'revoked_at', 'created_at'], note: "subject_type: 'USER' | 'ADMIN'" },
  { name: 'ingest_source', columns: ['id', 'name', 'api_key_hash', 'api_key_prefix', 'active', 'created_at', 'last_used_at'] },
  { name: 'alert_type', columns: ['code', 'label', 'severity', 'icon', 'color', 'channel_id', 'active', 'sort_order'], note: 'severity: INFO | WARNING | CRITICAL' },
  {
    name: 'alert',
    columns: ['id', 'source_id', 'source_alert_id', 'uid', 'alert_type_code', 'message', 'lat', 'lon', 'dbdt', 'sdt', 'extra', 'recipients_total', 'recipients_accepted', 'recipients_discarded'],
    note: 'dbdt = S3 time, sdt = received by server; extra jsonb',
  },
  { name: 'notification', columns: ['id', 'alert_id', 'user_id', 'status', 'sent_at', 'delivered_at', 'read_at', 'created_at'], note: 'status: PENDING | SENT | DELIVERED | READ | FAILED | NO_DEVICE' },
  {
    name: 'notification_delivery',
    columns: ['id', 'notification_id', 'device_id', 'status', 'attempts', 'fcm_message_id', 'last_error', 'queued_at', 'sent_at', 'delivered_at', 'next_attempt_at'],
    note: 'status: QUEUED | SENT | DELIVERED | FAILED | INVALID_TOKEN',
  },
  { name: 'discarded_recipient', columns: ['id', 'alert_id', 'raw_phone', 'phone_e164', 'reason', 'created_at'], note: 'reason: NO_USER | INVALID_PHONE | USER_DISABLED' },
  { name: 'admin_user', columns: ['id', 'username', 'password_hash', 'role', 'active', 'created_at', 'last_login_at'], note: 'role: ADMIN | VIEWER' },
  { name: 'app_setting', columns: ['key', 'value', 'updated_at'] },
];

export const SAMPLE_QUERIES: Array<{ label: string; sql: string }> = [
  { label: 'Alerts per type (7d)', sql: "SELECT alert_type_code, count(*) FROM alert WHERE sdt > now() - interval '7 days' GROUP BY 1 ORDER BY 2 DESC" },
  { label: 'Notification status breakdown', sql: 'SELECT status, count(*) FROM notification GROUP BY 1 ORDER BY 2 DESC' },
  { label: 'Failed deliveries with error', sql: "SELECT id, notification_id, attempts, last_error, next_attempt_at FROM notification_delivery WHERE status IN ('FAILED','INVALID_TOKEN') ORDER BY id DESC LIMIT 100" },
  { label: 'Silent devices (3+ days)', sql: "SELECT d.id, u.phone_e164, d.model, d.last_seen_at FROM user_device d JOIN app_user u ON u.id = d.user_id WHERE d.active AND d.last_seen_at < now() - interval '3 days' ORDER BY d.last_seen_at" },
];
