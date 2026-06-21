#!/usr/bin/env bash
# Shared helpers and config for the report build scripts.
set -euo pipefail

# Resolve project root (one level up from this scripts/ dir) regardless of CWD.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

MAIN="${MAIN:-main.qd}"
VERIFY_OUT="${VERIFY_OUT:-/tmp/quarkdown-verify}"
OUTPUT_DIR="${OUTPUT_DIR:-$PROJECT_ROOT/output}"

# Fail early with a clear message if quarkdown isn't installed.
require_quarkdown() {
    if ! command -v quarkdown >/dev/null 2>&1; then
        echo "error: 'quarkdown' not found on PATH. Install it (e.g. 'brew install quarkdown')." >&2
        exit 1
    fi
}
