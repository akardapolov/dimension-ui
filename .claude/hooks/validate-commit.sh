#!/usr/bin/env bash
set -euo pipefail

INPUT="$(cat)"

# Extract bash command Claude is about to run
COMMAND="$(printf '%s' "$INPUT" | jq -r '.tool_input.command // ""')"

# Validate only commands that contain git commit
if ! printf '%s' "$COMMAND" | grep -qE '(^|[;&|[:space:]])git commit([[:space:]]|$)'; then
  exit 0
fi

# Extract commit message from: git commit -m "..."
# Works for commands like:
#   git commit -m "msg"
#   git add . && git commit -m "msg"
MSG="$(printf '%s' "$COMMAND" | sed -nE 's/.*git commit[[:space:]].*-m[[:space:]]+["'"'"'].*/\1/p' | head -n1)"

if [ -z "${MSG:-}" ]; then
  exit 0
fi

VALID_TYPES="build|ci|docs|feat|fix|perf|refactor|revert|style|test|backport|ai"
PATTERN="^(${VALID_TYPES})(\([a-z0-9._-]+\))?: [a-z0-9].+$"

if ! echo "$MSG" | grep -qP "$PATTERN"; then
  {
    echo "COMMIT_MESSAGE_INVALID"
    echo
    echo "Message: $MSG"
    echo
    echo "Expected format: <type>(<scope>): <description>"
    echo "Allowed types: build, ci, docs, feat, fix, perf, refactor, revert, style, test, backport, ai"
    echo
    echo "Examples:"
    echo "  feat(dashboard): add realtime filter"
    echo "  fix: resolve null pointer in workspace loader"
    echo "  ai: add claude code configuration files"
  } >&2
  exit 2
fi