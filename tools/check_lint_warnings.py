#!/usr/bin/env python3
"""Fail when Android lint produces more warnings than the reviewed budget."""
from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPORT = Path("app/build/reports/lint-results-debug.xml")


def main() -> int:
    limit = int(sys.argv[1]) if len(sys.argv) > 1 else 114
    if not REPORT.is_file():
        print(f"lint report not found: {REPORT}", file=sys.stderr)
        return 2
    root = ET.parse(REPORT).getroot()
    warnings = [issue for issue in root.findall("issue")
                if issue.get("severity") == "Warning"]
    errors = [issue for issue in root.findall("issue")
              if issue.get("severity") == "Error"]
    print(f"lint: {len(errors)} errors, {len(warnings)} warnings "
          f"(reviewed budget: {limit})")
    if errors or len(warnings) > limit:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
