#!/usr/bin/env bash
# Prompt 08 §51 : sauvegarde PostgreSQL quotidienne avec rétention.
# Usage : ./backup.sh (à planifier via cron, ex. tous les jours à 2h du matin)
#   0 2 * * * /chemin/vers/backup.sh >> /var/log/transit-backup.log 2>&1
#
# NON EXÉCUTÉ dans cet environnement (aucune instance PostgreSQL disponible) — script
# écrit selon les pratiques standard pg_dump, à valider par un test réel de restauration
# avant de le considérer opérationnel (cf. restore.sh et §59 : "un backup qui n'a jamais
# été restauré n'est pas considéré comme validé").

set -euo pipefail

: "${DB_HOST:=localhost}"
: "${DB_PORT:=5432}"
: "${DB_NAME:=transit_platform}"
: "${DB_USERNAME:=transit_app}"
: "${BACKUP_DIR:=/var/backups/transit-platform}"
: "${RETENTION_DAYS:=14}"

mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/transit_platform_${TIMESTAMP}.dump"

# Format "custom" (-Fc) : compressé, restaurable sélectivement (table par table si besoin),
# contrairement à un simple dump SQL texte.
PGPASSWORD="${DB_PASSWORD:?Variable DB_PASSWORD requise}" pg_dump \
  -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -Fc "$DB_NAME" -f "$BACKUP_FILE"

echo "Sauvegarde créée : $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"

# Rétention : supprime les sauvegardes plus anciennes que RETENTION_DAYS jours.
find "$BACKUP_DIR" -name "transit_platform_*.dump" -mtime +"$RETENTION_DAYS" -delete

echo "Sauvegardes conservées :"
ls -lh "$BACKUP_DIR"

# Prompt 08 §51 : "stockage séparé du serveur principal lorsque possible" — copier
# $BACKUP_FILE vers un stockage distant (S3, autre serveur...) selon l'infrastructure
# réelle retenue. Non implémenté ici (dépend du choix d'hébergement, hors périmètre V1).
