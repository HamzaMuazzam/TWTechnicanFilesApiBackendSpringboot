-- Alert types are discovered dynamically from what S3 actually sends (auto-created on ingest).
-- Remove the guessed seed rows that were never used; rows already referenced by alerts are kept.
DELETE FROM alert_type t
WHERE NOT EXISTS (SELECT 1 FROM alert a WHERE a.alert_type_code = t.code);

-- Keyword rules used to pick a default severity for a newly discovered alert type (editable in the dashboard).
INSERT INTO app_setting (key, value) VALUES
    ('severity_keywords_critical', 'SOS,PANIC,EMERGENCY,POWER,TAMPER,ACCIDENT,CRASH,TOW,THEFT,JAM'),
    ('severity_keywords_warning',  'SPEED,GEOFENCE,FENCE,ZONE,BATTERY,HARSH,BRAK,ACCEL,DISCONNECT,DOOR,FUEL,TEMP')
ON CONFLICT (key) DO NOTHING;
