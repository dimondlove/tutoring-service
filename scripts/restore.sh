#!/usr/bin/env bash
# usage: ./scripts/restore.sh backups/demo.dump
set -euo pipefail
cd "$(dirname "$0")/.."
source .env
[ -f "${1:?usage: restore.sh <dump-file>}" ]
docker compose exec -T db pg_restore -U "$DB_USER" -d "$DB_NAME" --clean --if-exists --no-owner < "$1"
echo "Restored from: $1"