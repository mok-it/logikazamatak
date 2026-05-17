#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/restore-supabase-backup.sh --db-url <postgres-connection-string> --backup-dir <dir>

Options:
  --db-url <url>         Target database connection string. Can also be provided via NEW_DB_URL.
  --backup-dir <dir>     Directory containing roles/schema/data dump files.
  --keep-unzipped        Keep restored .sql files instead of deleting temporary unzipped copies.
  --with-roles           Also apply roles.sql before restoring schema and data.
  -h, --help             Show this help text.

Expected files inside the backup directory:
  roles.sql or roles.sql.gz
  schema.sql or schema.sql.gz
  data.sql or data.sql.gz

Default behavior restores only the app schema/data from `public`.
Managed Supabase schemas such as `auth` and `storage` are skipped even if they are present in the dump.
EOF
}

BACKUP_DIR=""
DB_URL="${NEW_DB_URL:-}"
KEEP_UNZIPPED="false"
WITH_ROLES="false"
TEMP_DIR=""

cleanup() {
  if [[ -n "${TEMP_DIR}" && -d "${TEMP_DIR}" ]]; then
    rm -rf "${TEMP_DIR}"
  fi
}

trap cleanup EXIT

while [[ $# -gt 0 ]]; do
  case "$1" in
    --db-url)
      DB_URL="${2:-}"
      shift 2
      ;;
    --backup-dir)
      BACKUP_DIR="${2:-}"
      shift 2
      ;;
    --keep-unzipped)
      KEEP_UNZIPPED="true"
      shift
      ;;
    --with-roles)
      WITH_ROLES="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "${DB_URL}" ]]; then
  echo "Missing target database URL. Use --db-url or set NEW_DB_URL." >&2
  exit 1
fi

if [[ -z "${BACKUP_DIR}" ]]; then
  echo "Missing backup directory. Use --backup-dir." >&2
  exit 1
fi

if [[ ! -d "${BACKUP_DIR}" ]]; then
  echo "Backup directory not found: ${BACKUP_DIR}" >&2
  exit 1
fi

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is required but was not found in PATH." >&2
  exit 1
fi

resolve_sql_file() {
  local base_name="$1"
  local sql_path="${BACKUP_DIR}/${base_name}.sql"
  local gzip_path="${sql_path}.gz"

  if [[ -f "${sql_path}" ]]; then
    printf '%s\n' "${sql_path}"
    return 0
  fi

  if [[ -f "${gzip_path}" ]]; then
    if [[ "${KEEP_UNZIPPED}" == "true" ]]; then
      gunzip -kf "${gzip_path}"
      printf '%s\n' "${sql_path}"
      return 0
    fi

    if [[ -z "${TEMP_DIR}" ]]; then
      TEMP_DIR="$(mktemp -d)"
    fi

    local temp_sql_path="${TEMP_DIR}/${base_name}.sql"
    gzip -dc "${gzip_path}" > "${temp_sql_path}"
    printf '%s\n' "${temp_sql_path}"
    return 0
  fi

  echo "Missing ${base_name}.sql or ${base_name}.sql.gz in ${BACKUP_DIR}" >&2
  return 1
}

SCHEMA_SQL="$(resolve_sql_file "schema")"
DATA_SQL="$(resolve_sql_file "data")"

prepare_public_data_sql() {
  local source_data_sql="$1"

  if [[ -z "${TEMP_DIR}" ]]; then
    TEMP_DIR="$(mktemp -d)"
  fi

  local filtered_data_sql="${TEMP_DIR}/data.public.sql"
  awk '
    BEGIN {
      in_copy = 0
      keep_copy = 0
      keep_block = 0
      seen_data_section = 0
    }

    /^COPY / {
      seen_data_section = 1
      in_copy = 1
      keep_copy = ($0 ~ /^COPY "public"\./)
      if (keep_copy) print
      next
    }

    in_copy {
      if (keep_copy) print
      if ($0 == "\\.") {
        in_copy = 0
        keep_copy = 0
      }
      next
    }

    seen_data_section == 0 {
      print
      next
    }

    /^-- Name: .*; Type: SEQUENCE SET; Schema: public;/ {
      keep_block = 1
      print
      next
    }

    keep_block {
      print
      if ($0 == "") {
        keep_block = 0
      }
    }
  ' "${source_data_sql}" > "${filtered_data_sql}"

  printf '%s\n' "${filtered_data_sql}"
}

PUBLIC_DATA_SQL="$(prepare_public_data_sql "${DATA_SQL}")"

ROLES_SQL=""
if [[ "${WITH_ROLES}" == "true" ]]; then
  ROLES_SQL="$(resolve_sql_file "roles")"
fi

echo "Restoring backup into target database..."
echo "Backup directory: ${BACKUP_DIR}"
if [[ "${WITH_ROLES}" == "true" ]]; then
  echo "Roles file: ${ROLES_SQL}"
fi
echo "Schema file: ${SCHEMA_SQL}"
echo "Data file: ${DATA_SQL}"
echo "Filtered public data file: ${PUBLIC_DATA_SQL}"

PSQL_ARGS=(
  --single-transaction
  --variable ON_ERROR_STOP=1
)

if [[ "${WITH_ROLES}" == "true" ]]; then
  PSQL_ARGS+=(--file "${ROLES_SQL}")
fi

PSQL_ARGS+=(
  --file "${SCHEMA_SQL}"
  --command 'SET session_replication_role = replica'
  --file "${PUBLIC_DATA_SQL}"
  --dbname "${DB_URL}"
)

psql "${PSQL_ARGS[@]}"

echo "Restore completed successfully."
