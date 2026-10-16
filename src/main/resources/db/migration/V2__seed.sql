-- TW Notification - seed data.
-- The default admin user and the S3 ingest source are created by StartupSeeder
-- (they need hashed secrets taken from the environment).

INSERT INTO alert_type (code, label, severity, icon, color, channel_id, active, sort_order) VALUES
    ('OVERSPEED',     'Overspeed',     'WARNING',  'speed',          '#B26A00', 'alerts_warning',  true, 10),
    ('GEOFENCE_IN',   'Geofence in',   'INFO',     'login',          '#0B57D0', 'alerts_default',  true, 20),
    ('GEOFENCE_OUT',  'Geofence out',  'WARNING',  'logout',         '#B26A00', 'alerts_warning',  true, 30),
    ('IGNITION_ON',   'Ignition on',   'INFO',     'key',            '#0B57D0', 'alerts_default',  true, 40),
    ('IGNITION_OFF',  'Ignition off',  'INFO',     'key_off',        '#0B57D0', 'alerts_default',  true, 50),
    ('SOS',           'SOS',           'CRITICAL', 'sos',            '#B3261E', 'alerts_critical', true, 60),
    ('POWER_CUT',     'Power cut',     'CRITICAL', 'power_off',      '#B3261E', 'alerts_critical', true, 70),
    ('LOW_BATTERY',   'Low battery',   'WARNING',  'battery_alert',  '#B26A00', 'alerts_warning',  true, 80),
    ('IDLE',          'Idle',          'INFO',     'hourglass',      '#0B57D0', 'alerts_default',  true, 90),
    ('HARSH_BRAKING', 'Harsh braking', 'WARNING',  'warning',        '#B26A00', 'alerts_warning',  true, 100);

INSERT INTO app_setting (key, value) VALUES
    ('retention_days', '90'),
    ('max_devices_per_user', '5'),
    ('dispatch_max_attempts', '5');
