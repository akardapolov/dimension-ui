#!/usr/bin/env bash
set -euo pipefail

# Read JSON from stdin (hook input)
INPUT="$(cat)"

# Extract file path from tool input
FILE_PATH="$(printf '%s' "$INPUT" | jq -r '.tool_input.file_path // ""')"

# Run only for Java files
if [[ "$FILE_PATH" != *.java ]]; then
  exit 0
fi

cd "$CLAUDE_PROJECT_DIR"

# Compile desktop module and its dependencies (main development module)
mvn -q -pl desktop -am compile -DskipTests 2>&1 | head -30