#!/usr/bin/env python3
"""Check that documentation translations are present and up to date.

French is the canonical language. Every pair listed in docs/translations.json
must keep both files in sync: when either side changes, the recorded hashes
must be refreshed after translating.

Usage:
    python tools/check_docs_translations.py            # CI check
    python tools/check_docs_translations.py --update   # record new hashes
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any, Dict, List

DEFAULT_MANIFEST = "docs/translations.json"


def file_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load_manifest(path: Path) -> Dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        data = json.load(handle)
    if not isinstance(data, dict):
        raise ValueError("manifest must be an object")
    pairs = data.get("pairs")
    if not isinstance(pairs, list) or not pairs:
        raise ValueError("manifest must contain a non-empty 'pairs' list")
    return data


def _pair_paths(root: Path, pair: Dict[str, Any]) -> tuple[Path, Path]:
    source = pair.get("source")
    translation = pair.get("translation")
    if not isinstance(source, str) or not isinstance(translation, str):
        raise ValueError("each pair needs string source and translation paths")
    if source == translation:
        raise ValueError(f"pair {source!r}: source and translation are equal")
    return root / source, root / translation


def check(root: Path, manifest_path: Path) -> List[str]:
    manifest = load_manifest(manifest_path)
    errors: List[str] = []
    for pair in manifest["pairs"]:
        try:
            source, translation = _pair_paths(root, pair)
        except ValueError as error:
            errors.append(str(error))
            continue
        if not source.is_file():
            errors.append(f"missing source file: {pair['source']}")
        if not translation.is_file():
            errors.append(f"missing translation file: {pair['translation']}")
        if not source.is_file() or not translation.is_file():
            continue
        if pair.get("source_sha256") != file_sha256(source):
            errors.append(
                f"{pair['source']} changed: update {pair['translation']} "
                "then run --update")
        if pair.get("translation_sha256") != file_sha256(translation):
            errors.append(
                f"{pair['translation']} changed: run --update after "
                "reviewing the translation")
    return errors


def update(root: Path, manifest_path: Path) -> None:
    manifest = load_manifest(manifest_path)
    for pair in manifest["pairs"]:
        source, translation = _pair_paths(root, pair)
        if not source.is_file() or not translation.is_file():
            raise ValueError(
                f"cannot hash missing pair: {pair.get('source')} / "
                f"{pair.get('translation')}")
        pair["source_sha256"] = file_sha256(source)
        pair["translation_sha256"] = file_sha256(translation)
    manifest_path.write_text(
        json.dumps(manifest, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8")


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check", action="store_true",
        help="verify pairs and hashes (default)")
    parser.add_argument(
        "--update", action="store_true",
        help="record the current source and translation hashes")
    parser.add_argument(
        "--root", default=str(Path(__file__).resolve().parents[1]),
        help="repository root (default: parent of tools/)")
    parser.add_argument(
        "--manifest", default=None,
        help=f"manifest path (default: {DEFAULT_MANIFEST})")
    args = parser.parse_args(argv)
    if args.check and args.update:
        parser.error("--check and --update are mutually exclusive")

    root = Path(args.root).resolve()
    manifest_path = (
        Path(args.manifest).resolve() if args.manifest
        else root / DEFAULT_MANIFEST)
    try:
        if args.update:
            update(root, manifest_path)
            print(f"Documentation translations updated in {manifest_path}")
            return 0
        errors = check(root, manifest_path)
    except (OSError, ValueError) as error:
        print(f"Documentation translation check failed: {error}",
              file=sys.stderr)
        return 2

    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        return 1
    print("Documentation translations are in sync.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
