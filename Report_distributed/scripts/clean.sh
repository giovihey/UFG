#!/usr/bin/env bash
# Remove generated build artifacts (local output dir and the strict-verify dir).
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

cd "$PROJECT_ROOT"

echo "Removing $OUTPUT_DIR and $VERIFY_OUT"
rm -rf "$OUTPUT_DIR" "$VERIFY_OUT"
