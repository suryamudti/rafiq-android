# Eval Harness for the Rafiq Agent

Runs an AI agent on tasks derived from this repo's real history and measures
whether the knowledge base (`kb/`) improves its performance.

## Prerequisites

- Python 3.12+ (stdlib only)
- git
- opencode CLI on PATH, or set `OPENCODE_BIN` to its path
- (for build checks only) working JAVA_HOME:
  `C:\Program Files\Android\Android Studio1\jbr`

## One-time setup

```powershell
python evals/build_kb_context.py
```

Regenerate `evals/kb_context.md` whenever `kb/` changes.

## Authoring tasks

Add a JSON file to `evals/tasks/`. Schema:

- `id`, `title`, `type` (`retrieval` | `coding`)
- `base_commit` (40-char sha), `gold_commit` (optional)
- `prompt` — goal statement, no hints about paths
- `gold_files` — expected files (used by checks + diff overlap)
- `checks` — `file_touched` / `transcript_mentions` / `test`
- `rubric` — strings used by the optional LLM judge
- `timeout_min`

For coding tasks, derive base/gold from real history:

```powershell
git log --format="%H %P" -1 <commit>
```

## Running

```powershell
python evals/harness.py list
python evals/harness.py run --task rf-001 --no-judge --variants both
python evals/harness.py run --no-judge --variants both
```

- `--no-judge` skips the (token-costly) LLM judge.
- `--rerun` re-runs completed variants.
- `--keep-worktrees` leaves worktrees for inspection.
- Results land in `evals/results/<task_id>/<variant>.json` (gitignored).

## Reporting kb impact

```powershell
python evals/report.py
```

Prints a baseline-vs-kb table with per-task and aggregate deltas. A positive
aggregate delta means the kb helped.
