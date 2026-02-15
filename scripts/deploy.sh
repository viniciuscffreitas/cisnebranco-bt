#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$HOME/www/cisnebranco-bt"

cd "$APP_DIR"

echo ">> Pulling latest code..."
git pull origin main

echo ">> Building and restarting containers..."
docker compose up -d --build --remove-orphans

echo ">> Waiting for health check..."
sleep 10

if docker compose ps | grep -q "healthy"; then
  echo ">> Deploy successful!"
else
  echo ">> Checking logs..."
  docker compose logs --tail=30 app
fi
