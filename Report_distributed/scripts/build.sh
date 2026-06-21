#!/usr/bin/env bash
# Verify the report compiles in strict mode, keeping artifacts out of the project.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

require_quarkdown
cd "$PROJECT_ROOT"

echo "Compiling $MAIN (strict) -> $VERIFY_OUT"
quarkdown c "$MAIN" --strict --out "$VERIFY_OUT"
