# Rafiq Android — Agent Knowledge Base (kb/)

This directory is a curated knowledge base for AI agents working on this repo.
It is DEEPER than `AGENTS.md` (the always-loaded quick guide). It is injected
as `evals/kb_context.md` during eval runs and can be read directly by agents.

## Files (concatenation order)

| File | Purpose |
|---|---|
| architecture.md | Module layout, Navigation3, MVVM screen pattern, DI |
| build-test.md | Build/run/test commands, JAVA_HOME, emulator, gotchas |
| conventions.md | KAPT/Hilt, Room api() rule, smart-cast rule, icons, DataStore |
| assets-data.md | DB copying, Arabic font, Quran DB schema |
| api-endpoints.md | Aladhan, Metals.live, Overpass endpoints + params |
| glossary.md | Domain terms (Surah, Ayah, tafsir, sajdah, juz, nisab, ...) |

## Keeping it current

Whenever AGENTS.md, the architecture, or a dependency changes in a way that
agents should know, update the relevant file here. Run:

```powershell
python evals/build_kb_context.py
```

to regenerate the injected context file.
