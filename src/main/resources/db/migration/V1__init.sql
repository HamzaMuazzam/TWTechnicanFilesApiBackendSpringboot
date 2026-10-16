-- TW Notification - initial schema (PostgreSQL 16)

CREATE TABLE app_user (
    id              bigserial PRIMARY KEY,
    phone_e164      text        NOT NULL UNIQUE,
    display_name    text,
    status          text        NOT NULL DEFAULT 'ACTIVE',          -- ACTIVE | DISABLED
    created_at      timestamptz NOT NULL DEFAULT now(),
    last_login_at   timestamptz,
    CONSTRAINT chk_app_user_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE user_device (
    id                          bigserial PRIMARY KEY,
    user_id                     bigint      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    device_id                   text        NOT NULL,                -- app-generated UUID, stable per install
    fcm_token                   text,
    manufacturer                text,
    model                       text,
    os_version                  text,
    app_version                 text,
    active                      boolean     NOT NULL DEFAULT true,
    last_seen_at                timestamptz,
    battery_opt_ignored         boolean,
    unused_restrictions_disabled boolean,
    notifications_enabled       boolean,
    created_at                  timestamptz NOT NULL DEFAULT now(),
    updated_at                  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_device UNIQUE (user_id, device_id)
);

CREATE TABLE refresh_token (
    id              bigserial PRIMARY KEY,
    subject_type    text        NOT NULL,                            -- USER | ADMIN
    subject_id      bigint      NOT NULL,
    device_id       text,
    token_hash      text        NOT NULL UNIQUE,
    expires_at      timestamptz NOT NULL,
    revoked_at      timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_refresh_token_subject CHECK (subject_type IN ('USER', 'ADMIN'))
);

CREATE TABLE ingest_source (
    id              bigserial PRIMARY KEY,
    name            text        NOT NULL UNIQUE,
    api_key_hash    text        NOT NULL,
    api_key_prefix  text        NOT NULL,                            -- first 8 chars, for display
    active          boolean     NOT NULL DEFAULT true,
    created_at      timestamptz NOT NULL DEFAULT now(),
    last_used_at    timestamptz
);

CREATE TABLE alert_type (
    code            text PRIMARY KEY,
    label           text        NOT NULL,
    severity        text        NOT NULL DEFAULT 'INFO',             -- INFO | WARNING | CRITICAL
    icon            text,
    color           text,
    channel_id      text,
    active          boolean     NOT NULL DEFAULT true,
    sort_order      integer     NOT NULL DEFAULT 0,
    CONSTRAINT chk_alert_type_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL'))
);

CREATE TABLE alert (
    id                   bigserial PRIMARY KEY,
    source_id            bigint      NOT NULL REFERENCES ingest_source (id),
    source_alert_id      text        NOT NULL,
    uid                  text        NOT NULL,
    alert_type_code      text        NOT NULL REFERENCES alert_type (code),
    message              text,
    lat                  double precision,
    lon                  double precision,
    dbdt                 timestamptz,                                -- S3's own timestamp, stored as sent
    sdt                  timestamptz NOT NULL DEFAULT now(),         -- when OUR server received it
    extra                jsonb,
    recipients_total     integer     NOT NULL DEFAULT 0,
    recipients_accepted  integer     NOT NULL DEFAULT 0,
    recipients_discarded integer     NOT NULL DEFAULT 0,
    CONSTRAINT uq_alert_source UNIQUE (source_id, source_alert_id)
);

CREATE TABLE notification (
    id              bigserial PRIMARY KEY,
    alert_id        bigint      NOT NULL REFERENCES alert (id) ON DELETE CASCADE,
    user_id         bigint      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    status          text        NOT NULL DEFAULT 'PENDING',          -- PENDING | SENT | DELIVERED | READ | FAILED | NO_DEVICE
    sent_at         timestamptz,
    delivered_at    timestamptz,
    read_at         timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_notification_alert_user UNIQUE (alert_id, user_id),
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED', 'NO_DEVICE'))
);

CREATE TABLE notification_delivery (
    id              bigserial PRIMARY KEY,
    notification_id bigint      NOT NULL REFERENCES notification (id) ON DELETE CASCADE,
    device_id       bigint      NOT NULL REFERENCES user_device (id) ON DELETE CASCADE,
    status          text        NOT NULL DEFAULT 'QUEUED',           -- QUEUED | SENT | DELIVERED | FAILED | INVALID_TOKEN
    attempts        integer     NOT NULL DEFAULT 0,
    fcm_message_id  text,
    last_error      text,
    queued_at       timestamptz NOT NULL DEFAULT now(),
    sent_at         timestamptz,
    delivered_at    timestamptz,
    next_attempt_at timestamptz,
    CONSTRAINT uq_delivery_notification_device UNIQUE (notification_id, device_id),
    CONSTRAINT chk_delivery_status CHECK (status IN ('QUEUED', 'SENT', 'DELIVERED', 'FAILED', 'INVALID_TOKEN'))
);

CREATE TABLE discarded_recipient (
    id              bigserial PRIMARY KEY,
    alert_id        bigint      NOT NULL REFERENCES alert (id) ON DELETE CASCADE,
    raw_phone       text,
    phone_e164      text,
    reason          text        NOT NULL,                            -- NO_USER | INVALID_PHONE | USER_DISABLED
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_discard_reason CHECK (reason IN ('NO_USER', 'INVALID_PHONE', 'USER_DISABLED'))
);

CREATE TABLE admin_user (
    id              bigserial PRIMARY KEY,
    username        text        NOT NULL UNIQUE,
    password_hash   text        NOT NULL,
    role            text        NOT NULL DEFAULT 'VIEWER',           -- ADMIN | VIEWER
    active          boolean     NOT NULL DEFAULT true,
    created_at      timestamptz NOT NULL DEFAULT now(),
    last_login_at   timestamptz,
    CONSTRAINT chk_admin_role CHECK (role IN ('ADMIN', 'VIEWER'))
);

CREATE TABLE app_setting (
    key             text PRIMARY KEY,
    value           text        NOT NULL,
    updated_at      timestamptz NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_notification_user_id_desc      ON notification (user_id, id DESC);
CREATE INDEX idx_notification_user_status       ON notification (user_id, status);
CREATE INDEX idx_notification_alert             ON notification (alert_id);
CREATE INDEX idx_alert_uid                      ON alert (uid);
CREATE INDEX idx_alert_sdt_desc                 ON alert (sdt DESC);
CREATE INDEX idx_alert_type_code                ON alert (alert_type_code);
CREATE INDEX idx_delivery_status_next_attempt   ON notification_delivery (status, next_attempt_at);
CREATE INDEX idx_delivery_device                ON notification_delivery (device_id);
CREATE INDEX idx_user_device_fcm_token          ON user_device (fcm_token);
CREATE INDEX idx_user_device_user               ON user_device (user_id);
CREATE INDEX idx_refresh_token_subject          ON refresh_token (subject_type, subject_id);
CREATE INDEX idx_discarded_alert                ON discarded_recipient (alert_id);
