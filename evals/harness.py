import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
import time
from dataclasses import dataclass, field
from pathlib import Path

REPO_DIR = Path(__file__).resolve().parent.parent
EVALS_DIR = Path(__file__).resolve().parent
TASKS_DIR = EVALS_DIR / "tasks"
RESULTS_DIR = EVALS_DIR / "results"
WORKTREES_DIR = EVALS_DIR / "worktrees"
KB_CONTEXT_PATH = EVALS_DIR / "kb_context.md"

VALID_TYPES = {"retrieval", "coding"}
VALID_CHECK_KINDS = {"file_touched", "transcript_mentions", "test"}


@dataclass
class Task:
    id: str
    title: str
    type: str
    base_commit: str
    prompt: str
    gold_commit: str | None = None
    gold_files: list[str] = field(default_factory=list)
    checks: list[dict] = field(default_factory=list)
    rubric: list[str] = field(default_factory=list)
    timeout_min: int = 30


def load_task(path: Path) -> Task:
    data = json.loads(path.read_text(encoding="utf-8"))
    return Task(**data)


def validate_task(task: Task, repo: Path) -> list[str]:
    errors = []
    if not task.id or not task.title:
        errors.append("id and title are required")
    if task.type not in VALID_TYPES:
        errors.append(f"type must be one of {sorted(VALID_TYPES)}")
    if not re.fullmatch(r"[0-9a-f]{40}", task.base_commit):
        errors.append("base_commit must be a 40-char git sha")
    if task.gold_commit is not None and not re.fullmatch(r"[0-9a-f]{40}", task.gold_commit):
        errors.append("gold_commit must be a 40-char git sha or null")
    if not task.prompt:
        errors.append("prompt is required")
    for c in task.checks:
        if c.get("kind") not in VALID_CHECK_KINDS:
            errors.append(f"unknown check kind: {c.get('kind')}")
        if c.get("kind") in ("file_touched", "transcript_mentions") and not c.get("files"):
            errors.append(f"check {c.get('kind')} needs 'files'")
        if c.get("kind") == "test" and not c.get("command"):
            errors.append("test check needs 'command'")
    return errors


def find_opencode_bin() -> str:
    env_bin = os.environ.get("OPENCODE_BIN")
    candidates = [env_bin, "opencode"] if env_bin else ["opencode"]
    for c in candidates:
        if c and _which(c):
            return c
    raise FileNotFoundError(
        "opencode CLI not found. Install it or set OPENCODE_BIN to the binary "
        "path (the Desktop app alone is not a CLI)."
    )


def _which(cmd: str) -> bool:
    if os.path.isabs(cmd) or "/" in cmd or "\\" in cmd:
        return Path(cmd).exists()
    for d in os.environ.get("PATH", "").split(os.pathsep):
        if d and (Path(d) / cmd).exists():
            return True
        if d and (Path(d) / f"{cmd}.exe").exists():
            return True
    return False


def create_worktree(name: str, base_commit: str) -> Path:
    wt = WORKTREES_DIR / name
    if wt.exists():
        remove_worktree(name, wt)
    WORKTREES_DIR.mkdir(parents=True, exist_ok=True)
    rc, out, err = run_cmd(
        ["git", "worktree", "add", str(wt), base_commit], REPO_DIR, 120
    )
    if rc != 0:
        raise RuntimeError(f"worktree add failed: {err.strip()}")
    return wt


def remove_worktree(name: str, worktree: Path) -> None:
    if not worktree.exists():
        return
    run_cmd(["git", "worktree", "remove", "--force", str(worktree)], REPO_DIR, 60)


def inject_kb(worktree: Path) -> None:
    if not KB_CONTEXT_PATH.exists():
        raise FileNotFoundError(
            "kb_context.md missing; run `python evals/build_kb_context.py` first"
        )
    agents = worktree / "AGENTS.md"
    kb_text = KB_CONTEXT_PATH.read_text(encoding="utf-8")
    if agents.exists():
        agents.write_text(
            agents.read_text(encoding="utf-8").rstrip() + "\n\n" + kb_text,
            encoding="utf-8",
        )
    else:
        agents.write_text(kb_text, encoding="utf-8")


def run_cmd(cmd: list[str], cwd: Path, timeout_s: int) -> tuple[int, str, str]:
    try:
        proc = subprocess.run(
            cmd, cwd=str(cwd), capture_output=True, text=True, timeout=timeout_s
        )
        return proc.returncode, proc.stdout, proc.stderr
    except subprocess.TimeoutExpired as e:
        return -1, (e.stdout or ""), (e.stderr or "") + "\nTIMEOUT"


def _opencode_cmd(opencode_bin: str, args: list[str]) -> list[str]:
    return opencode_bin.split() + args


def run_agent(worktree: Path, prompt: str, timeout_s: int, opencode_bin: str) -> tuple[int, str]:
    cmd = _opencode_cmd(opencode_bin, ["run", prompt])
    rc, out, err = run_cmd(cmd, worktree, timeout_s)
    return rc, out + err


def capture_diff(worktree: Path) -> str:
    rc, diff, _ = run_cmd(["git", "diff"], worktree, 60)
    rc2, status, _ = run_cmd(["git", "status", "--porcelain"], worktree, 60)
    return (diff or "") + "\n" + (status or "")


def _changed_paths(worktree: Path) -> set[str]:
    rc, status, _ = run_cmd(["git", "status", "--porcelain"], worktree, 60)
    paths = set()
    for line in (status or "").splitlines():
        parts = line.split(" ", 2)
        if len(parts) >= 2:
            paths.add(parts[-1].strip('"'))
    rc, diff, _ = run_cmd(["git", "diff", "--name-only"], worktree, 60)
    for line in (diff or "").splitlines():
        if line.strip():
            paths.add(line.strip())
    return paths


def run_checks(task: Task, worktree: Path, transcript: str) -> dict[str, bool]:
    results = {}
    changed = _changed_paths(worktree)
    for check in task.checks:
        kind = check["kind"]
        if kind == "file_touched":
            key = "file_touched:" + ",".join(check["files"])
            results[key] = any(f in changed for f in check["files"])
        elif kind == "transcript_mentions":
            key = "transcript_mentions:" + ",".join(check["files"])
            results[key] = any(f in transcript for f in check["files"])
        elif kind == "test":
            cmd = check["command"].split()
            rc, _, _ = run_cmd(cmd, worktree, task.timeout_min * 60)
            results["test:" + check["command"]] = rc == 0
    return results


def _added_lines(diff: str) -> set[str]:
    lines = set()
    for line in (diff or "").splitlines():
        if line.startswith("+") and not line.startswith("+++"):
            lines.add(line[1:].strip())
    return lines


def gold_diff(task: Task) -> str:
    if not task.gold_commit:
        return ""
    rc, diff, err = run_cmd(
        ["git", "diff", task.base_commit, task.gold_commit], REPO_DIR, 60
    )
    if rc != 0:
        raise RuntimeError(f"gold diff failed: {err.strip()}")
    return diff or ""


def diff_overlap(agent_diff: str, gold: str) -> float:
    agent_added = _added_lines(agent_diff)
    gold_added = _added_lines(gold)
    if not gold_added:
        return 0.0
    return len(agent_added & gold_added) / len(gold_added)


def judge_llm(task: Task, transcript: str, agent_diff: str, opencode_bin: str) -> dict:
    rubric = "\n".join(f"- {r}" for r in task.rubric) or "- correctness"
    payload = (
        "You are grading an AI agent's work on a coding task.\n"
        f"Task prompt: {task.prompt}\n"
        f"Rubric:\n{rubric}\n"
        f"Transcript:\n{transcript[-8000:]}\n"
        f"Diff:\n{agent_diff[-4000:]}\n"
        'Reply with ONLY JSON: {"score": <int 0-10>, "reason": "<string>"}'
    )
    with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False, encoding="utf-8") as f:
        f.write(payload)
        prompt_file = f.name
    try:
        cmd = _opencode_cmd(opencode_bin, ["run", f'Grade the work in this file: {prompt_file}. {payload}'])
        rc, out, err = run_cmd(cmd, EVALS_DIR, 300)
        if rc != 0:
            return {"score": 0, "reason": f"judge failed: {err.strip()}"}
        m = re.search(r"\{.*\}", out, re.DOTALL)
        if not m:
            return {"score": 0, "reason": "no JSON in judge output"}
        return json.loads(m.group(0))
    except (json.JSONDecodeError, ValueError, TypeError):
        return {"score": 0, "reason": "malformed judge JSON"}
    finally:
        Path(prompt_file).unlink(missing_ok=True)
