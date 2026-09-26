#!/usr/bin/env bash
set -euo pipefail

if grep -R $'\t' src --include='*.java'; then
  echo "Java sources contain tab characters" >&2
  exit 1
fi
