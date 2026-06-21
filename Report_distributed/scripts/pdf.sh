#!/usr/bin/env bash
# Export the report to PDF (spins up a headless browser, so it's slower).
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

require_quarkdown
cd "$PROJECT_ROOT"

echo "Exporting $MAIN to PDF -> $OUTPUT_DIR"
quarkdown c "$MAIN" --pdf --out "$OUTPUT_DIR"
