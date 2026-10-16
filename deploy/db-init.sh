#!/bin/sh
# Runs once before the API starts (compose service "db-init").
# Makes the database + application role exist even when ./data/pg was initialised
# earlier with different settings (the postgres image only honours POSTGRES_DB on an empty volume).
set -eu

HOST="${PGHOST:-postgres}"
SUPER_USER="${POSTGRES_USER:-tw}"
SUPER_PASS="${POSTGRES_PASSWORD:-}"
DB="${POSTGRES_DB:-tw_notification}"
APP_USER="${APP_DB_USER:-$SUPER_USER}"
APP_PASS="${APP_DB_PASSWORD:-$SUPER_PASS}"

echo "[db-init] waiting for postgres at $HOST ..."
i=0
until pg_isready -h "$HOST" -U "$SUPER_USER" -d postgres -q; do
  i=$((i+1)); [ "$i" -gt 60 ] && { echo "[db-init] postgres not ready after 60s"; exit 1; }
  sleep 1
done

export PGPASSWORD="$SUPER_PASS"
run() { psql -h "$HOST" -U "$SUPER_USER" -d postgres -v ON_ERROR_STOP=1 -qtAX "$@"; }

if ! run -c "select 1" >/dev/null 2>&1; then
  cat <<MSG
[db-init] ERROR: cannot log in to postgres as '$SUPER_USER' with the password from .env.
          The volume ./data/pg was initialised with different credentials.
          Either restore the original POSTGRES_USER/POSTGRES_PASSWORD in .env, or (if the data is disposable)
          stop the stack, delete ./data/pg and run ./install.sh again.
MSG
  exit 1
fi

if [ "$APP_USER" != "$SUPER_USER" ]; then
  if [ "$(run -c "select 1 from pg_roles where rolname='$APP_USER'")" = "1" ]; then
    run -c "alter role \"$APP_USER\" with login password '$APP_PASS'"
    echo "[db-init] role '$APP_USER' exists - password synced"
  else
    run -c "create role \"$APP_USER\" with login password '$APP_PASS'"
    echo "[db-init] role '$APP_USER' created"
  fi
fi

if [ "$(run -c "select 1 from pg_database where datname='$DB'")" = "1" ]; then
  echo "[db-init] database '$DB' exists"
else
  run -c "create database \"$DB\" owner \"$APP_USER\" encoding 'UTF8'"
  echo "[db-init] database '$DB' created (owner $APP_USER)"
fi
run -c "grant all privileges on database \"$DB\" to \"$APP_USER\"" >/dev/null
psql -h "$HOST" -U "$SUPER_USER" -d "$DB" -v ON_ERROR_STOP=1 -q -c "grant all on schema public to \"$APP_USER\"" >/dev/null
echo "[db-init] done"
