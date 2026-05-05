#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 /path/to/workout.json"
  exit 1
fi

WORKOUT_JSON="$1"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB_OUT="${ROOT_DIR}/app/src/main/assets/preloaded/workout_routines_database"

python3 "${ROOT_DIR}/tools/generate_preloaded_db.py" \
  --input "${WORKOUT_JSON}" \
  --schema "${ROOT_DIR}/app/schemas/com.noahjutz.gymroutines.data.AppDatabase/44.json" \
  --output "${DB_OUT}"

(
  cd "${ROOT_DIR}"
  ./gradlew assembleDebug
)

echo "APK: ${ROOT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
