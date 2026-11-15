#!/usr/bin/env bash
# Initialize, tag and push this fork.
# Usage: ./push.sh <github-user>   (default branch: main, tag: v0.1.1)
set -euo pipefail
USER="${1:?usage: $0 <github-user>}"
cd "$(dirname "$0")"
if [ ! -d .git ]; then git init -b main; fi
git add .
git commit -m "Local Pairer: corrected GPL-2.0 fork" || true
git remote remove origin 2>/dev/null || true
git remote add origin "https://github.com/${USER}/meross-local-pairer.git"
git push -u origin main
if ! git rev-parse v0.1.1 >/dev/null 2>&1; then
  git tag -a v0.1.1 -m "Local Pairer 0.1.1"
fi
git push origin v0.1.1
