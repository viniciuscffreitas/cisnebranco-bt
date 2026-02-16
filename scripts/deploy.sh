#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$HOME/www/cisnebranco-bt"
LOG_FILE="$APP_DIR/deploy.log"

cd "$APP_DIR"

git fetch origin main --quiet

LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse origin/main)

if [ "$LOCAL" = "$REMOTE" ]; then
  exit 0
fi

echo "[$(date)] Deploying $LOCAL → $REMOTE" >> "$LOG_FILE"

git pull origin main --quiet

docker compose up -d --build --remove-orphans >> "$LOG_FILE" 2>&1

sleep 15

if docker compose ps | grep -q "healthy"; then
  echo "[$(date)] Deploy successful" >> "$LOG_FILE"
else
  echo "[$(date)] Deploy WARNING — check container health" >> "$LOG_FILE"
  docker compose logs --tail=20 app >> "$LOG_FILE" 2>&1
fi
