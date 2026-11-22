# Documentation translations

French is the canonical documentation language. English is the published
default for international readers.

## Layout

- English lives at the canonical path: `README.md`, `info.md`, `docs/...`.
- French lives in a sibling file: `README.fr.md`, `info.fr.md`,
  `docs/....fr.md`.
- Each pair starts with a language switcher:
  `**English** · [Français](file.fr.md)` /
  `[English](file.md) · **Français**`.

## Keeping pairs in sync

Every pair is listed in [`translations.json`](translations.json) with the
SHA-256 of both files.

1. Edit the French canonical file.
2. Translate the English counterpart in the same change and review it.
3. Record the new hashes:
   ```bash
   python tools/check_docs_translations.py --update
   ```
4. Commit both files and the manifest update.

CI runs `python tools/check_docs_translations.py` and fails when a pair is
missing, a file changed without refreshing its hash, or the two sides drift.

Use the [translation glossary](TRANSLATION-GLOSSARY.md) for terminology.

## Scope

- User-facing documentation is translated first: README, quick start,
  installation, tuning, troubleshooting, compatibility.
- Design, ADR and historical release notes may stay in French initially; do
  not retro-translate old changelog entries.
- Legal documents (`LICENSE`, `NOTICE`, `LEGAL`) are not translated: the
  original text prevails.

## En bref (français)

Le français reste la source de référence ; l’anglais est publié par défaut.
Toute modification française doit être accompagnée de la version anglaise.
`--update` enregistre les empreintes et la CI échoue en cas de dérive.
