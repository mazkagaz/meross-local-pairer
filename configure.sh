#!/usr/bin/env bash
# Replace repository placeholders with a GitHub user/repo.
set -euo pipefail
USER="${1:?usage: $0 <github-user>}"
cd "$(dirname "$0")"
grep -rl '<GITHUB_USER>' README.md NOTICE MODIFICATIONS.md push.sh 2>/dev/null | while read -r f; do
  sed -i "s|https://github.com/<GITHUB_USER>/meross-local-pairer|https://github.com/${USER}/meross-local-pairer|g" "$f"
done
echo "Configured for https://github.com/${USER}/meross-local-pairer"
