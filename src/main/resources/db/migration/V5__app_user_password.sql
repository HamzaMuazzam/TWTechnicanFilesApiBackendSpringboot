-- App users sign in with phone + password (no OTP). Password stored as BCrypt hash; null = cannot sign in yet.
ALTER TABLE app_user ADD COLUMN password_hash text;
