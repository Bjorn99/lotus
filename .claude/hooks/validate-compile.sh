#!/bin/bash
# Post-edit compilation check
# Usage: ./validate-compile.sh <filepath>
set -euo pipefail

FILE="$1"
PROJECT_ROOT="$(git rev-parse --show-toplevel)"

# Only compile for Kotlin source changes
case "${FILE##*.}" in
  kt|kts) ;;
  *) echo "SKIP: not a Kotlin file"; exit 0 ;;
esac

REL_PATH="${FILE#$PROJECT_ROOT/}"
MODULE=":app"

# Compile only the affected build variant
case "$REL_PATH" in
  app/src/test/*)
    echo "COMPILE: $MODULE:compileDebugKotlin + compileTestDebugKotlin"
    cd "$PROJECT_ROOT"
    ./gradlew "$MODULE:compileDebugKotlin" "$MODULE:compileTestDebugKotlin" --quiet 2>&1 | tail -5
    ;;
  app/src/androidTest/*)
    echo "COMPILE: $MODULE:compileDebugAndroidTestKotlin"
    cd "$PROJECT_ROOT"
    ./gradlew "$MODULE:compileDebugAndroidTestKotlin" --quiet 2>&1 | tail -5
    ;;
  *)
    echo "COMPILE: $MODULE:compileDebugKotlin"
    cd "$PROJECT_ROOT"
    ./gradlew "$MODULE:compileDebugKotlin" --quiet 2>&1 | tail -5
    ;;
esac

EXIT_CODE=${PIPESTATUS[0]}
if [ $EXIT_CODE -eq 0 ]; then
  echo "PASS"
else
  echo "FAIL"
  exit 1
fi
