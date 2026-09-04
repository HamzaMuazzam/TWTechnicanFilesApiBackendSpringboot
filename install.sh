#!/usr/bin/env bash
# =============================================================================
#  TW Technician Files API - one-shot Docker install / update / logs
#
#    ./install.sh          install (or update) and follow logs
#    ./install.sh logs     just follow logs
#    ./install.sh stop     stop the container
#
#  What it does: installs Docker if missing -> makes Docker start on boot ->
#  removes old launchd/systemd services -> frees port 8088 -> opens the port
#  in the firewall -> builds the image -> (re)starts ONLY the app container
#  (restart: unless-stopped => survives reboot) -> follows colored logs.
#  Ctrl+C leaves the logs only; the app keeps running.
# =============================================================================
set -uo pipefail
cd "$(dirname "$0")"

APP=app                                  # compose service name
CONTAINER=tw-technician-files-api
PORT=8088
OS="$(uname -s)"

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
    echo "  ${W}Stop:${N}            ${G}./install.sh stop${N}"
    echo
    exit 0' INT TERM
  docker compose logs -f --tail=200 --timestamps "$APP"
  echo "${Y}Log stream ended (container stopped?). Re-enter with ./install.sh logs${N}"
}

# =============================================================================
case "${1:-}" in
  logs) follow_logs; exit 0 ;;
  stop) docker compose stop "$APP" && ok "stopped"; exit 0 ;;
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

# ---------- 5. Build + (re)start only the app container ----------
mkdir -p data/files data/logs
if docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  step "Container already running - rebuilding and restarting ONLY '$CONTAINER'"
else
  step "Building image and starting '$CONTAINER'"
fi
docker compose build "$APP" || die "image build failed"
docker compose up -d --no-deps --force-recreate "$APP" || die "container failed to start"

# ---------- 6. Health ----------
step "Waiting for the API"
UP=0
for i in $(seq 1 45); do
  if curl -fs -o /dev/null "http://localhost:$PORT/technician/v3/api-docs" 2>/dev/null; then UP=1; break; fi
  sleep 2
done
docker ps --filter "name=$CONTAINER" --format "  ${D}{{.Names}}  {{.Status}}  {{.Ports}}${N}"
if [[ $UP -eq 1 ]]; then
  ok "${G}API is UP${N}:  http://localhost:$PORT/technician/index.html  (swagger)"
  IP="$(hostname -I 2>/dev/null | awk '{print $1}')"; [[ -z "$IP" ]] && IP="$(ipconfig getifaddr en0 2>/dev/null || true)"
  [[ -n "$IP" ]] && info "LAN:  http://$IP:$PORT/technician/"
else
  warn "API not answering yet - check the logs below"
fi
ok "restart policy: unless-stopped  ->  comes back after crash and after reboot"

follow_logs
