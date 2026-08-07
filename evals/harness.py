import argparse
import json
import os
import re
import subprocess
import sys
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
