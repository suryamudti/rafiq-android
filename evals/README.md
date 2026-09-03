# Eval Harness for the Rafiq Agent

Runs an AI agent on tasks derived from this repo's real history and measures
whether the knowledge base (`kb/`) improves its performance.

## Prerequisites

- Python 3.12+ (stdlib only)
- git
- An agent CLI on PATH, or configure `--agent-cmd` / `OPENCODE_BIN`
- (for Android build/test checks) JDK 17:
  `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot` (see `AGENTS.md`)

## One-time setup

```powershell
python evals/build_kb_context.py
```

*Note: The harness will automatically detect when `kb/` has changed and refresh `evals/kb_context.md`.*

## Authoring tasks

Add a JSON file to `evals/tasks/`. Schema:

- `id`, `title`, `type` (`retrieval` | `coding`)
- `base_commit` (40-char sha), `gold_commit` (optional)
- `prompt` — goal statement, no hints about paths
- `gold_files` — expected files (used by checks + diff overlap)
- `checks` — `file_touched` / `transcript_mentions` / `content_contains` / `test`
- `rubric` — strings used by the optional LLM judge
- `timeout_min`

For coding tasks, derive base/gold from real history:

```powershell
git log --format="%H %P" -1 <commit>
```

Verify task definitions and git commit integrity:

```powershell
python evals/harness.py verify-tasks
```

## Running

```powershell
# List available tasks
python evals/harness.py list

# Run a specific task with default agent (opencode)
python evals/harness.py run --task rf-001 --no-judge --variants both

# Run with custom agent CLI (e.g. agy, claude, etc.)
python evals/harness.py run --task rf-001 --agent-cmd "agy run {prompt}" --no-judge
```

- `--no-judge` skips the (token-costly) LLM judge and normalizes scoring fairly.
- `--agent-cmd` sets custom agent command template (e.g. `"agy run {prompt}"`).
- `--rerun` re-runs completed variants.
- `--keep-worktrees` leaves worktrees for inspection.
- Results land in `evals/results/<task_id>/<variant>.json` (gitignored).

## Reporting kb impact

```powershell
python evals/report.py
python evals/report.py --detail
python evals/report.py --json
```

Prints a baseline-vs-kb table with task-aware scoring (properly normalized across retrieval and coding tasks) and aggregate deltas. A positive aggregate delta means the knowledge base improved agent performance.

