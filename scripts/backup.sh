#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source .env
mkdir -p backups
FILE="backups/${DB_NAME}_$(date +%Y%m%d_%H%M%S).dump"
docker compose exec -T db pg_dump -U "$DB_USER" -Fc "$DB_NAME" > "$FILE"
echo "Backup saved: $FILE"