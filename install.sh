#!/usr/bin/env bash
# =============================================================================
#  TW Notification API - one-shot Docker install / update / logs
#
#    ./install.sh          install (or update) and follow logs
#    ./install.sh service  same, plus register a boot service (systemd / launchd) so the
#                          whole stack comes back by itself after a reboot
#    ./install.sh status   show service + container state
#    ./install.sh logs     just follow logs
#    ./install.sh stop     stop the containers AND remove the boot service
#    ./install.sh db-shell open psql on the database
#    ./install.sh reset-db wipe ./data/pg (asks for confirmation)
#
#  What it does: installs Docker if missing -> makes Docker start on boot ->
#  removes old launchd/systemd services -> frees port 8089 -> opens the port
#  in the firewall -> writes .env (random JWT_SECRET) if missing -> builds the
#  image -> starts postgres + app (restart: unless-stopped => survives reboot)
#  -> follows colored logs.
#  Ctrl+C leaves the logs only; the app keeps running.
# =============================================================================
set -uo pipefail
cd "$(dirname "$0")"

APP=app                                  # compose service name
DB=postgres                              # compose service name of the database
CONTAINER=tw-notification-api
PORT=8089
OS="$(uname -s)"
PROJECT_DIR="$(pwd)"
SERVICE_NAME=tw-notification
SYSTEMD_UNIT=/etc/systemd/system/$SERVICE_NAME.service
LAUNCHD_LABEL=com.trackingworld.notification
LAUNCHD_PLIST=/Library/LaunchDaemons/$LAUNCHD_LABEL.plist
INSTALL_SERVICE=0

# ---------- colors ----------
R=$'\e[0;31m'; G=$'\e[0;32m'; Y=$'\e[0;33m'; B=$'\e[0;34m'; M=$'\e[0;35m'; C=$'\e[0;36m'; W=$'\e[1;37m'; D=$'\e[2m'; N=$'\e[0m'
step() { echo; echo "${B}==>${N} ${W}$*${N}"; }
ok()   { echo "  ${G}✔${N} $*"; }
warn() { echo "  ${Y}!${N} $*"; }
info() { echo "  ${C}·${N} $*"; }
die()  { echo; echo "${R}✖ $*${N}"; exit 1; }
SUDO=""; [[ $EUID -ne 0 ]] && command -v sudo >/dev/null && SUDO="sudo"

follow_logs() {
  echo
  echo "${M}──────────────────────────────────────────────────────────────────${N}"
  echo "${M} LIVE LOGS  ${N}${D}container: $CONTAINER   port: $PORT   Ctrl+C = leave logs (app keeps running)${N}"
  echo "${M}──────────────────────────────────────────────────────────────────${N}"
  trap '
    echo
    echo "${Y}You left the log view. The app is still running in Docker.${N}"
    echo
    echo "  ${W}Re-enter logs:${N}   ${G}./install.sh logs${N}"
    echo "  ${W}Update/redeploy:${N} ${G}./install.sh${N}"
    echo "  ${W}Status:${N}          ${G}docker ps${N}"
    echo "  ${W}Status:${N}          ${G}./install.sh status${N}"
    echo "  ${W}Stop (+service):${N} ${G}./install.sh stop${N}"
    echo
    exit 0' INT TERM
  docker compose logs -f --tail=200 --timestamps "$APP"
  echo "${Y}Log stream ended (container stopped?). Re-enter with ./install.sh logs${N}"
}

# ---------- boot service (systemd / launchd) ----------
service_installed() {
  if [[ "$OS" == "Darwin" ]]; then [[ -f "$LAUNCHD_PLIST" ]]
  else systemctl list-unit-files 2>/dev/null | grep -q "^$SERVICE_NAME.service"; fi
}

install_service() {
  step "Registering boot service '$SERVICE_NAME'"
  chmod +x deploy/boot.sh
  if [[ "$OS" == "Darwin" ]]; then
    $SUDO tee "$LAUNCHD_PLIST" >/dev/null <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>Label</key><string>$LAUNCHD_LABEL</string>
  <key>ProgramArguments</key><array><string>/bin/bash</string><string>$PROJECT_DIR/deploy/boot.sh</string></array>
  <key>WorkingDirectory</key><string>$PROJECT_DIR</string>
  <key>UserName</key><string>$USER</string>
  <key>RunAtLoad</key><true/>
  <key>EnvironmentVariables</key><dict><key>PATH</key><string>/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin</string></dict>
  <key>StandardOutPath</key><string>$PROJECT_DIR/data/logs/boot.log</string>
  <key>StandardErrorPath</key><string>$PROJECT_DIR/data/logs/boot.log</string>
</dict></plist>
PLIST
    $SUDO chown root:wheel "$LAUNCHD_PLIST"; $SUDO chmod 644 "$LAUNCHD_PLIST"
    $SUDO launchctl bootout system "$LAUNCHD_PLIST" >/dev/null 2>&1 || true
    $SUDO launchctl bootstrap system "$LAUNCHD_PLIST" && ok "launchd daemon registered: $LAUNCHD_PLIST (runs at boot)"
    info "macOS note: Docker Desktop starts at login; the service waits for it (up to 5 min) and then starts the stack"
  else
    $SUDO tee "$SYSTEMD_UNIT" >/dev/null <<UNIT
[Unit]
Description=TW Notification API (docker compose stack)
Requires=docker.service
After=docker.service network-online.target
Wants=network-online.target

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$PROJECT_DIR
ExecStart=/bin/bash $PROJECT_DIR/deploy/boot.sh
ExecStop=/usr/bin/docker compose stop app postgres
TimeoutStartSec=600

[Install]
WantedBy=multi-user.target
UNIT
    $SUDO systemctl daemon-reload
    $SUDO systemctl enable "$SERVICE_NAME" >/dev/null 2>&1 && ok "systemd unit enabled: $SYSTEMD_UNIT (starts at boot)"
    $SUDO systemctl start "$SERVICE_NAME" >/dev/null 2>&1 || true
  fi
}

remove_service() {
  if ! service_installed; then return 0; fi
  if [[ "$OS" == "Darwin" ]]; then
    $SUDO launchctl bootout system "$LAUNCHD_PLIST" >/dev/null 2>&1 || true
    $SUDO rm -f "$LAUNCHD_PLIST" && ok "boot service removed ($LAUNCHD_LABEL)"
  else
    $SUDO systemctl disable "$SERVICE_NAME" >/dev/null 2>&1 || true
    $SUDO rm -f "$SYSTEMD_UNIT"; $SUDO systemctl daemon-reload && ok "boot service removed ($SERVICE_NAME.service)"
  fi
}

show_status() {
  step "Service"
  if service_installed; then ok "boot service installed - stack starts automatically after reboot"
  else info "no boot service - containers restart with Docker only (run ./install.sh service to add one)"; fi
  step "Containers"
  docker compose ps 2>/dev/null || warn "docker not reachable"
  step "API"
  if curl -fs -o /dev/null "http://localhost:$PORT/actuator/health" 2>/dev/null; then ok "UP  http://localhost:$PORT/admin"; else warn "not answering on :$PORT"; fi
}

# =============================================================================
# Load .env (if present) so sub-commands see POSTGRES_* values.
if [[ -f .env ]]; then set -a; # shellcheck disable=SC1091
  . ./.env; set +a; fi

case "${1:-}" in
  logs) follow_logs; exit 0 ;;
  service) INSTALL_SERVICE=1 ;;
  status) show_status; exit 0 ;;
  stop)
    step "Stopping"
    docker compose stop "$APP" "$DB" && ok "containers stopped"
    remove_service
    info "nothing will start at reboot now. Start again with:  ./install.sh   (or ./install.sh service)"
    exit 0 ;;
  db-shell) docker compose exec "$DB" psql -U "${POSTGRES_USER:-tw}" -d "${POSTGRES_DB:-tw_notification}"; exit 0 ;;
  reset-db)
    read -r -p "This DELETES ./data/pg (all alerts, users, history). Type RESET to continue: " ans
    [[ "$ans" == "RESET" ]] || { warn "cancelled"; exit 0; }
    docker compose down "$APP" "$DB" 2>/dev/null || true
    $SUDO rm -rf ./data/pg && ok "data/pg removed - run ./install.sh to recreate"; exit 0 ;;
esac

# ---------- 1. Docker ----------
step "Checking Docker"
if ! command -v docker >/dev/null 2>&1; then
  warn "Docker not found - installing"
  if [[ "$OS" == "Darwin" ]]; then
    command -v brew >/dev/null || die "Homebrew is required to install Docker on macOS: https://brew.sh"
    brew install --cask docker || die "brew install --cask docker failed"
  else
    curl -fsSL https://get.docker.com | $SUDO sh || die "Docker install failed"
    $SUDO usermod -aG docker "$USER" 2>/dev/null && warn "added $USER to docker group (re-login later to drop sudo)"
  fi
fi
ok "docker: $(docker --version 2>/dev/null || echo 'installed')"

if [[ "$OS" == "Darwin" ]]; then
  if ! docker info >/dev/null 2>&1; then
    info "starting Docker Desktop..."
    open -a Docker 2>/dev/null || open -a "Docker Desktop" 2>/dev/null
    for i in $(seq 1 60); do docker info >/dev/null 2>&1 && break; sleep 2; done
  fi
  docker info >/dev/null 2>&1 || die "Docker Desktop did not start. Open it once manually, then re-run."
  # start Docker Desktop at login so containers come back after reboot
  SETTINGS="$HOME/Library/Group Containers/group.com.docker/settings-store.json"
  [[ -f "$SETTINGS" ]] || SETTINGS="$HOME/Library/Group Containers/group.com.docker/settings.json"
  if [[ -f "$SETTINGS" ]] && command -v python3 >/dev/null; then
    python3 - "$SETTINGS" <<'PY' && ok "Docker Desktop set to start at login (survives reboot)"
import json,sys; p=sys.argv[1]; d=json.load(open(p))
for k in ("OpenUIOnStartupDisabled","openUIOnStartupDisabled"): d[k]=True
for k in ("AutoStart","autoStart"): d[k]=True
json.dump(d,open(p,"w"),indent=2)
PY
  else
    warn "Enable 'Start Docker Desktop when you sign in' in Docker settings so the app survives reboot"
  fi
else
  $SUDO systemctl enable --now docker >/dev/null 2>&1 && ok "docker daemon enabled at boot"
  docker info >/dev/null 2>&1 || die "Cannot talk to Docker daemon (try: sudo ./install.sh)"
fi
docker compose version >/dev/null 2>&1 || die "'docker compose' plugin missing - update Docker"

# ---------- 2. Remove old non-Docker services ----------
step "Removing old launchd/systemd services (if any)"
if [[ "$OS" == "Darwin" ]]; then
  for P in /Library/LaunchDaemons/com.trackingworld.technician-api.plist; do
    if [[ -f "$P" ]]; then $SUDO launchctl bootout system "$P" >/dev/null 2>&1; $SUDO rm -f "$P"; ok "removed $P"; fi
  done
else
  if systemctl list-unit-files 2>/dev/null | grep -q '^tw-technician-api'; then
    $SUDO systemctl disable --now tw-technician-api >/dev/null 2>&1; $SUDO rm -f /etc/systemd/system/tw-technician-api.service
    $SUDO systemctl daemon-reload; ok "removed tw-technician-api.service"
  fi
fi
# previous deployment of this project ran as the "tw-technician-files-api" container on the same port
if docker ps -a --format '{{.Names}}' 2>/dev/null | grep -qx tw-technician-files-api; then
  docker rm -f tw-technician-files-api >/dev/null 2>&1 && ok "removed legacy container tw-technician-files-api (uploads in ./data/files are kept)"
fi
info "nothing else to remove"

# ---------- 3. Free port ----------
step "Freeing port $PORT"
if [[ "$OS" == "Darwin" ]]; then PIDS="$($SUDO lsof -ti tcp:$PORT -sTCP:LISTEN 2>/dev/null || true)"
else PIDS="$($SUDO lsof -ti tcp:$PORT -sTCP:LISTEN 2>/dev/null || $SUDO fuser -n tcp $PORT 2>/dev/null || true)"; fi
for pid in $PIDS; do
  name="$(ps -p "$pid" -o comm= 2>/dev/null)"
  if [[ "$name" == *docker* || "$name" == *com.docker* ]]; then
    info "port held by Docker ($name) - will be replaced by the new container"
  else
    warn "killing pid $pid ($name) on port $PORT"; $SUDO kill -9 "$pid" 2>/dev/null || true
  fi
done
[[ -z "$PIDS" ]] && ok "port $PORT is free"

# ---------- 4. Firewall ----------
step "Opening port $PORT in the firewall"
if [[ "$OS" == "Darwin" ]]; then
  FW=/usr/libexec/ApplicationFirewall/socketfilterfw
  if $SUDO $FW --getglobalstate 2>/dev/null | grep -qi "enabled"; then
    for APPB in "/Applications/Docker.app" "/Applications/Docker.app/Contents/MacOS/com.docker.backend"; do
      [[ -e "$APPB" ]] && { $SUDO $FW --add "$APPB" >/dev/null 2>&1; $SUDO $FW --unblockapp "$APPB" >/dev/null 2>&1; }
    done
    ok "Docker allowed through macOS application firewall"
  else
    ok "macOS firewall is off - nothing to open"
  fi
else
  if command -v ufw >/dev/null && $SUDO ufw status 2>/dev/null | grep -q "Status: active"; then
    $SUDO ufw allow "$PORT/tcp" >/dev/null && ok "ufw: allowed $PORT/tcp"
  elif command -v firewall-cmd >/dev/null && $SUDO firewall-cmd --state >/dev/null 2>&1; then
    $SUDO firewall-cmd --permanent --add-port="$PORT/tcp" >/dev/null && $SUDO firewall-cmd --reload >/dev/null && ok "firewalld: opened $PORT/tcp"
  elif command -v iptables >/dev/null; then
    $SUDO iptables -C INPUT -p tcp --dport "$PORT" -j ACCEPT 2>/dev/null || $SUDO iptables -I INPUT -p tcp --dport "$PORT" -j ACCEPT
    ok "iptables: accepted $PORT/tcp"
  else
    ok "no host firewall detected"
  fi
fi
info "docker publishes 0.0.0.0:$PORT -> container:$PORT (see docker-compose.yml)"

# ---------- 5. Environment file ----------
step "Checking .env"
if [[ ! -f .env ]]; then
  SECRET="$(head -c 48 /dev/urandom | base64 | tr -d '\n/+=' | head -c 48)"
  DBPASS="$(head -c 24 /dev/urandom | base64 | tr -d '\n/+=' | head -c 24)"
  cat > .env <<ENV
POSTGRES_DB=tw_notification
POSTGRES_USER=tw
POSTGRES_PASSWORD=$DBPASS
JWT_SECRET=$SECRET
FIREBASE_ENABLED=false
AUTH_DEV_LOGIN_ENABLED=false
ADMIN_INITIAL_PASSWORD=ChangeMe123!
INGEST_INITIAL_API_KEY=
APP_TIMEZONE=Asia/Karachi
CORS_ALLOWED_ORIGINS=*
ENV
  ok "created .env with a random JWT_SECRET and DB password"
  warn "edit .env: set ADMIN_INITIAL_PASSWORD / INGEST_INITIAL_API_KEY, and FIREBASE_ENABLED=true once config/firebase-service-account.json is in place"
else
  ok ".env present"
fi
[[ -f config/firebase-service-account.json ]] || warn "config/firebase-service-account.json missing - push notifications stay in dev (logging) mode until FIREBASE_ENABLED=true and the file exists"

# ---------- 6. Build + (re)start the containers ----------
mkdir -p data/files data/logs data/pg config
if docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  step "Container already running - rebuilding and restarting '$CONTAINER'"
else
  step "Building image and starting '$CONTAINER' (+ postgres)"
fi
export DOCKERFILE=Dockerfile
if ! docker compose build "$APP"; then
  warn "in-Docker build failed (usually: build container has no internet). Falling back to building the jar on this machine..."
  JAVA_OK=0
  for cand in "${JAVA_HOME:-}" /usr/lib/jvm/*17* /usr/lib/jvm/*21* /usr/lib/jvm/*25* "$(/usr/libexec/java_home -v 17 2>/dev/null || true)" ""; do
    if [[ -n "$cand" && -x "$cand/bin/java" ]]; then export JAVA_HOME="$cand"; export PATH="$JAVA_HOME/bin:$PATH"; JAVA_OK=1; break; fi
  done
  if [[ $JAVA_OK -eq 0 ]] && command -v java >/dev/null; then JAVA_OK=1; fi
  if [[ $JAVA_OK -eq 0 ]]; then
    if [[ "$OS" != "Darwin" ]]; then
      warn "no JDK on host - installing openjdk-17"
      ($SUDO apt-get update -qq && $SUDO apt-get install -y -qq openjdk-17-jdk-headless) >/dev/null 2>&1 \
        || $SUDO yum install -y java-17-openjdk-devel >/dev/null 2>&1 || die "could not install a JDK; install JDK 17 and re-run"
      JAVA_OK=1
    else
      die "no JDK 17 on this Mac: brew install openjdk@17 and re-run"
    fi
  fi
  JMAJ="$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')"
  (( JMAJ >= 17 && JMAJ <= 24 )) || die "host Java $JMAJ cannot run Gradle 8.14 (needs 17-24); set JAVA_HOME to a JDK 17"
  info "the build compiles with a JDK 17 toolchain (auto-downloaded by Gradle if the host JDK differs)"
  info "building with host JDK $(java -version 2>&1 | head -1)"
  chmod +x gradlew
  ./gradlew --no-daemon clean bootJar -x test || die "host build failed too - check internet/proxy on this server"
  ok "jar built on host: build/libs/app.jar"
  export DOCKERFILE=Dockerfile.prebuilt
  docker compose build "$APP" || die "image build failed"
fi
docker compose up -d "$DB" || die "postgres failed to start"
step "Ensuring database and role exist"
# Self-heal: if ./data/pg was initialised with another password (e.g. .env copied from another machine),
# set the password from .env via the container's local socket (trust auth inside the official image).
PGU="${POSTGRES_USER:-tw}"; PGP="${POSTGRES_PASSWORD:-tw_notification_change_me}"
for i in $(seq 1 30); do docker compose exec -T "$DB" pg_isready -U "$PGU" -d postgres -q 2>/dev/null && break; sleep 1; done
if ! docker compose exec -T -e PGPASSWORD="$PGP" "$DB" psql -h 127.0.0.1 -U "$PGU" -d postgres -qtAc "select 1" >/dev/null 2>&1; then
  if docker compose exec -T "$DB" psql -U "$PGU" -d postgres -qtAc "alter role \"$PGU\" with login password '$PGP'" >/dev/null 2>&1; then
    ok "password of role '$PGU' in ./data/pg synced with .env"
  elif docker compose exec -T "$DB" psql -U postgres -d postgres -qtAc "do \$\$ begin if exists (select 1 from pg_roles where rolname='$PGU') then execute format('alter role %I with login superuser password %L','$PGU','$PGP'); else execute format('create role %I with login superuser password %L','$PGU','$PGP'); end if; end \$\$" >/dev/null 2>&1; then
    ok "role '$PGU' created/updated in ./data/pg (volume was initialised with user 'postgres')"
  else
    warn "could not repair credentials automatically - if the data is disposable: ./install.sh reset-db"
  fi
fi
# Runs every time: creates the DB/role if ./data/pg was initialised earlier with other settings.
docker compose run --rm db-init || die "db-init failed - see the message above (usually credentials in .env do not match ./data/pg)"
ok "database ready"
docker compose up -d --force-recreate "$APP" || die "container failed to start"

# ---------- 7. Health ----------
step "Waiting for the API"
UP=0
for i in $(seq 1 60); do
  if curl -fs -o /dev/null "http://localhost:$PORT/actuator/health" 2>/dev/null; then UP=1; break; fi
  sleep 2
done
docker ps --filter "name=$CONTAINER" --format "  ${D}{{.Names}}  {{.Status}}  {{.Ports}}${N}"
if [[ $UP -eq 1 ]]; then
  ok "${G}API is UP${N}:  http://localhost:$PORT/technician/index.html  (swagger)   http://localhost:$PORT/admin  (dashboard)"
  IP="$(hostname -I 2>/dev/null | awk '{print $1}')"; [[ -z "$IP" ]] && IP="$(ipconfig getifaddr en0 2>/dev/null || true)"
  [[ -n "$IP" ]] && info "LAN:  http://$IP:$PORT/admin"
  info "default dashboard login: admin / ADMIN_INITIAL_PASSWORD from .env (ChangeMe123! if unset) - change it!"
else
  warn "API not answering yet - check the logs below"
fi
if [[ $INSTALL_SERVICE -eq 1 ]]; then
  install_service
elif service_installed; then
  ok "boot service '$SERVICE_NAME' already registered - stack starts automatically after reboot"
else
  ok "restart policy: unless-stopped  ->  comes back after crash and after reboot while Docker starts at boot"
  info "to also register a system service (survives reboot even if Docker was stopped): ./install.sh service"
fi

follow_logs
