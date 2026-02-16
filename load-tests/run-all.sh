#!/bin/bash
# Run all load tests sequentially
# Usage: ./run-all.sh [BASE_URL]
#
# Examples:
#   ./run-all.sh                                        # local (http://localhost:8091/api)
#   ./run-all.sh https://api.petshopcisnebranco.com.br/api  # production
#
# Prerequisites: k6 installed (https://k6.io/docs/get-started/installation/)
#   brew install k6          # macOS
#   apt install k6           # Ubuntu/Debian
#   choco install k6         # Windows

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
export BASE_URL="${1:-http://localhost:8091/api}"

echo "============================================"
echo "  Cisne Branco — Load Tests"
echo "  Target: $BASE_URL"
echo "============================================"
echo ""

TESTS=(
    "auth-load.js:Authentication flow"
    "os-workflow-load.js:OS workflow & browsing"
    "reports-load.js:Reports & exports"
    "appointments-load.js:Appointments & scheduling"
)

PASSED=0
FAILED=0

for entry in "${TESTS[@]}"; do
    IFS=":" read -r file desc <<< "$entry"
    echo "──────────────────────────────────────────"
    echo "  Running: $desc ($file)"
    echo "──────────────────────────────────────────"

    if k6 run "$SCRIPT_DIR/$file"; then
        PASSED=$((PASSED + 1))
    else
        FAILED=$((FAILED + 1))
    fi
    echo ""
done

echo "============================================"
echo "  Results: $PASSED passed, $FAILED failed"
echo "============================================"

exit $FAILED
