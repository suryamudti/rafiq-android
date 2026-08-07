# Design: Knowledge Base & Agent Evaluation Harness for Rafiq Android

Date: 2026-08-07
Status: Approved in brainstorming; pending spec review

## Problem

The repo already has `AGENTS.md` (always-loaded quick guide) and a `graphify-out/`
knowledge graph, but there is no way to (a) provide agents a deeper, curated,
context-injectable knowledge base, or (b) measure whether that knowledge base
actually helps an agent complete real work on this codebase.

## Goal

1. **kb/** — a curated markdown knowledge base for agents, deeper than `AGENTS.md`.
2. **evals/** — a local, lightweight, automatable eval harness that runs an agent
   (`opencode run`) inside isolated git worktrees against tasks derived from real
   repo history, grades them (programmatic checks + optional LLM-as-judge), and
   reports a baseline-vs-kb comparison so the kb's impact is measurable.

## Approach

Python 3.12 harness (stdlib only), git worktrees for isolation, `opencode run`
for agent execution, JSON task definitions, hybrid grading, and a comparison
report. Chosen over a prepare-only shell script (no automation) and a Node harness
(Python is the more battle-tested fit for task harnesses; both runtimes exist).

## Repository layout

```
kb/                          # curated agent knowledge base
  README.md                  # index & how to keep it current
  architecture.md            # module layout, Navigation3 pattern, data flow
  build-test.md              # build/run/test commands, JAVA_HOME, emulator
  conventions.md             # KAPT/Hilt, Room api() rule, smart-cast gotchas, icons
  assets-data.md             # DB copying, Arabic font, Quran DB schema
  api-endpoints.md           # Aladhan, Metals.live, Overpass
  glossary.md                # domain terms (Surah, Ayah, tafsir, sajdah, juz, ...)

evals/
  README.md                  # how to author tasks, run the harness, read results
  harness.py                 # the runner (Python 3.12, stdlib only)
  build_kb_context.py        # concatenates kb/*.md -> evals/kb_context.md
  kb_context.md              # generated, gitignored
  tasks/                     # one JSON per eval task (seeded from real history)
    rf-001-dashboard-prayer-hero.json
    ...
  results/                   # per-task outputs, gitignored
  report.py                  # baseline-vs-kb comparison report
```

## Knowledge base

`AGENTS.md` stays as the always-loaded quick guide. The kb is the deeper optional
reference, injected only during eval runs (and usable manually by an agent that
reads `kb/`).

Each `kb/*.md` file uses a consistent renderable header:

```
## kb/<file>  — <one-line purpose>
<body>
```

| File | Content |
|---|---|
| `architecture.md` | 4 modules (`:core`, `:domain`, `:data`, `:app`), Navigation3 15-route pattern, MVVM screen pattern, DI via `AppModule.kt` |
| `build-test.md` | Build/run/test commands from AGENTS.md, JAVA_HOME path, emulator name, "no lint/typecheck" note |
| `conventions.md` | KAPT-not-KSP, Room `api()` rule, cross-module smart-cast rule, `material-icons-core` only, `LocaleUtil`, `PreferencesManager`/DataStore |
| `assets-data.md` | `DatabaseCopier` flattening, Arabic font via `FontFamily`, Quran DB all-TEXT columns, bismillah nullable |
| `api-endpoints.md` | Aladhan v1/timings (method 20, default Jakarta coords), Metals.live gold/silver per-gram conversion, Overpass mosque POIs |
| `glossary.md` | Domain terms: Surah, Ayah, tafsir, sajdah, juz, nisab, zakat, tasbih, hijri, qibla |

`build_kb_context.py` concatenates files in fixed order with headers into
`evals/kb_context.md`.

## Eval task format

One JSON file per task in `evals/tasks/`:

```json
{
  "id": "rf-001",
  "title": "Revamp home dashboard prayer hero widget",
  "type": "coding",
  "base_commit": "<sha before the feature>",
  "gold_commit": "<sha of the merged feature>",
  "prompt": "Implement the home dashboard revamp: add a prayer hero widget ... (goal statement, no hints about paths)",
  "gold_files": ["app/src/main/java/.../HomeScreen.kt", "..."],
  "checks": [
    {"kind": "test", "command": ".\\gradlew testDebug"},
    {"kind": "file_touched", "files": ["app/src/.../HomeViewModel.kt"]}
  ],
  "rubric": ["UI matches requested layout", "uses existing patterns", "no new deps"],
  "timeout_min": 30
}
```

### Task types

- **`retrieval`** — prompt asks the agent to find/explain something (e.g., the FTS3
  translation query fix). Graded by programmatic checks (correct file located) +
  LLM rubric. Fast.
- **`coding`** — prompt asks for a real change matching a real past PR. Graded by
  diff-vs-gold overlap, `gold_files` touched, tests passing, + LLM rubric.

## Harness flow

Per task, per variant (`baseline` and `+kb`):

1. Create an isolated **git worktree** at `base_commit`.
2. For the `+kb` variant, copy `evals/kb_context.md` into the worktree root as
   `AGENTS.kb.md` and reference it in the prompt/injection.
3. Run `opencode run "<prompt>"` inside the worktree (binary via `OPENCODE_BIN`,
   configurable since the CLI is not on PATH — only the Desktop app is installed),
   capturing transcript + final diff.
4. Grade: run `checks` (tests, file-touched), then optional LLM-as-judge on
   transcript + diff against `rubric`.
5. Write `evals/results/<task>/<variant>.json` — transcript, diff, check results,
   scores.

**Comparison report** (`report.py`): table per task of baseline vs +kb scores,
aggregate delta, pass/fail. This is the kb-impact measurement.

## Seed suite (~7 tasks) from real history

- rf-001 *retrieval*: locate FTS3 translation query bug fix (`fix/fts3-translation-query`)
- rf-002 *retrieval*: explain Navigation3 route registration + how a new screen gets added
- rf-003 *retrieval*: trace prayer-times data flow from Aladhan API to UI
- rf-004 *coding*: reproduce the home dashboard prayer-hero revamp
- rf-005 *coding*: reproduce ayah screen app-bar declutter
- rf-006 *coding*: reproduce the vector icon feature-card work
- rf-007 *coding*: add app version text on homescreen

Exact base/gold SHAs to be pinned during planning from `git log`.

## Prerequisites (checked at harness start)

- git, Python 3.12
- `opencode` CLI on PATH **or** `OPENCODE_BIN` env var pointing to it
  (Desktop app alone is insufficient — documented as a real gap)
- Java + Android SDK for `testDebug`/`assembleDebug` checks (JAVA_HOME from AGENTS.md)

## Error handling / robustness

- Per-task timeouts (default 30 min, overridable); killed process → task marked
  `failed`, never hangs.
- Harness is re-entrant: skips already-completed variants unless `--rerun`.
- LLM judge optional via `--no-judge` (programmatic-only), since it costs tokens.
- Worktrees cleaned up after run (`--keep-worktrees` to inspect failures).
- Results written incrementally so a crash mid-run doesn't lose completed tasks.

## Deliverables

- `kb/` — 7 markdown files + README
- `evals/` — harness.py, build_kb_context.py, report.py, 7 task JSONs, README
- `.gitignore` entries for `evals/kb_context.md` + `evals/results/`
- This spec document

## Out of scope

- Adding `material-icons-extended` or any app feature work
- CI integration of the harness (can be a follow-up)
- A large task suite (suite grows over time)
