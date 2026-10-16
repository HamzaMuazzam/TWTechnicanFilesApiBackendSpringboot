-- S3 "Configuration": which phone numbers receive alerts for a tracker (uid), optionally per alert type.
-- Pushed by S3 via POST /api/v1/ingest/configurations; alerts no longer carry recipients.
CREATE TABLE configuration (
    id          bigserial PRIMARY KEY,
    source_id   text UNIQUE,                       -- S3's Configuration.ID (upsert key); null when added by hand
    phone_raw   text NOT NULL,                     -- as received
    phone_e164  text NOT NULL,                     -- normalised (+92...)
    uid         text NOT NULL,
    alert_type  text NOT NULL DEFAULT '*',         -- '*' = every alert type for this uid
    dbdt        timestamptz,                       -- S3's own timestamp
    crt_by      text,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_configuration_uid ON configuration (uid, alert_type);
CREATE INDEX ix_configuration_phone ON configuration (phone_e164);
