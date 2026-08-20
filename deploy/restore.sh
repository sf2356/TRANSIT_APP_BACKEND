#!/usr/bin/env bash
# Prompt 08 §51/§59 : procédure de restauration PostgreSQL.
# Usage : ./restore.sh /var/backups/transit-platform/transit_platform_20260101_020000.dump
#
# NON EXÉCUTÉ dans cet environnement — à tester réellement (créer une base de test,
# restaurer, vérifier les données) AVANT de considérer la stratégie de sauvegarde validée.

set -euo pipefail

BACKUP_FILE="${1:?Usage: ./restore.sh <fichier.dump>}"
: "${DB_HOST:=localhost}"
: "${DB_PORT:=5432}"
: "${DB_NAME:=transit_platform}"
: "${DB_USERNAME:=transit_app}"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "Fichier de sauvegarde introuvable : $BACKUP_FILE" >&2
  exit 1
fi

echo "⚠️  Cette opération va ÉCRASER la base '$DB_NAME'. Ctrl+C pour annuler, Entrée pour continuer."
read -r

PGPASSWORD="${DB_PASSWORD:?Variable DB_PASSWORD requise}" pg_restore \
  -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" \
  --clean --if-exists --no-owner --no-privileges "$BACKUP_FILE"

echo "Restauration terminée. Vérifications recommandées :"
echo "  - SELECT COUNT(*) FROM entreprises;"
echo "  - SELECT COUNT(*) FROM dossiers;"
echo "  - SELECT COUNT(*) FROM factures;"
echo "  - Démarrer le backend et tester /actuator/health puis un login réel."
