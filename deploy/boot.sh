#!/usr/bin/env bash
# Started at boot by the tw-notification service (systemd on Linux, launchd on macOS).
# Waits for the Docker daemon (Docker Desktop can take a while after login), then brings the stack up.
set -u
cd "$(dirname "$0")/.."
LOG=./data/logs/boot.log; mkdir -p ./data/logs
echo "[$(date '+%F %T')] boot: waiting for docker" >>"$LOG"
for i in $(seq 1 150); do
  if docker info >/dev/null 2>&1; then
    echo "[$(date '+%F %T')] boot: docker ready, starting stack" >>"$LOG"
    docker compose up -d postgres >>"$LOG" 2>&1
    docker compose run --rm db-init >>"$LOG" 2>&1
    docker compose up -d app >>"$LOG" 2>&1 && echo "[$(date '+%F %T')] boot: stack up" >>"$LOG"
    exit 0
  fi
  if [[ "$(uname -s)" == "Darwin" && $i -eq 1 ]]; then open -a Docker 2>/dev/null || open -a "Docker Desktop" 2>/dev/null; fi
  sleep 2
done
echo "[$(date '+%F %T')] boot: docker never became ready" >>"$LOG"
exit 1
