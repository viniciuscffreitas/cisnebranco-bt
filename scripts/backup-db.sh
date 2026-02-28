#!/usr/bin/env bash
set -euo pipefail

# --- Configuration ---
BACKUP_DIR="/backups"
RETENTION_DAYS=30
DB_NAME="${POSTGRES_DB:-cisnebranco}"
DB_USER="${POSTGRES_USER:-cisnebranco}"
DB_HOST="${DB_HOST:-db}"
TIMESTAMP=$(date +%Y-%m-%d_%H-%M)
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.dump"

# --- Cleanup on failure ---
cleanup_on_failure() {
  local exit_code=$?
  if [ -f "$BACKUP_FILE" ]; then
    echo "[$(date)] ERROR: Backup failed (exit code ${exit_code}). Removing partial dump: ${BACKUP_FILE}" >&2
    rm -f "$BACKUP_FILE"
  else
    echo "[$(date)] ERROR: Backup failed (exit code ${exit_code}). No dump file was created." >&2
  fi
  exit "$exit_code"
}
trap cleanup_on_failure ERR

# --- Ensure backup directory exists ---
mkdir -p "$BACKUP_DIR"

# --- Run backup ---
echo "[$(date)] Starting backup: ${BACKUP_FILE}"

pg_dump \
  --host="$DB_HOST" \
  --username="$DB_USER" \
  --format=custom \
  --no-password \
  --file="$BACKUP_FILE" \
  "$DB_NAME"

# --- Validate backup ---
if [ ! -s "$BACKUP_FILE" ]; then
  echo "[$(date)] ERROR: Backup file is empty: ${BACKUP_FILE}" >&2
  rm -f "$BACKUP_FILE"
  exit 1
fi

pg_restore --list "$BACKUP_FILE" > /dev/null 2>&1 || {
  echo "[$(date)] ERROR: Backup verification failed — dump may be corrupt: ${BACKUP_FILE}" >&2
  rm -f "$BACKUP_FILE"
  exit 1
}

FILESIZE=$(stat -c%s "$BACKUP_FILE" 2>/dev/null || stat -f%z "$BACKUP_FILE" 2>/dev/null || echo "unknown")
echo "[$(date)] Backup complete: ${BACKUP_FILE} (${FILESIZE} bytes)"

# --- Mark success for healthcheck ---
touch "${BACKUP_DIR}/.last_success"

# --- Prune old backups ---
DELETED=$(find "$BACKUP_DIR" -name "${DB_NAME}_*.dump" -mtime +${RETENTION_DAYS} -print -delete | wc -l)
if [ "$DELETED" -gt 0 ]; then
  echo "[$(date)] Pruned ${DELETED} backups older than ${RETENTION_DAYS} days"
fi

echo "[$(date)] Done. Current backups:"
ls -lh "$BACKUP_DIR"/${DB_NAME}_*.dump 2>/dev/null | tail -5
