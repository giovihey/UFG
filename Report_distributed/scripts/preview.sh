#!/usr/bin/env bash
# Launch the live preview server (watch + auto-reload). Long-running; Ctrl-C to stop.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

require_quarkdown
cd "$PROJECT_ROOT"

echo "Starting live preview for $MAIN (watch mode). Press Ctrl-C to stop."
quarkdown c "$MAIN" -p -w
