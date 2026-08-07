import json
import os
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent))

from harness import REPO_DIR, Task, load_task, validate_task

TASK_JSON = {
    "id": "rf-001",
    "title": "t",
    "type": "retrieval",
    "base_commit": "0000000000000000000000000000000000000000",
    "gold_commit": "1111111111111111111111111111111111111111",
    "prompt": "Find X.",
    "gold_files": ["a/b.kt"],
    "checks": [{"kind": "file_touched", "files": ["a/b.kt"]}],
    "rubric": ["correct"],
    "timeout_min": 15,
}


class LoadTaskTest(unittest.TestCase):
    def test_load_task_parses_json(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp) / "task.json"
            p.write_text(json.dumps(TASK_JSON), encoding="utf-8")
            task = load_task(p)
        self.assertEqual(task.id, "rf-001")
        self.assertEqual(task.type, "retrieval")
        self.assertEqual(task.gold_files, ["a/b.kt"])
        self.assertEqual(task.timeout_min, 15)

    def test_validate_accepts_good_task(self):
        task = Task(**TASK_JSON)
        self.assertEqual(validate_task(task, REPO_DIR), [])

    def test_validate_rejects_missing_fields(self):
        task = Task(id="x", title="", type="bogus", base_commit="", gold_commit=None,
                    prompt="", gold_files=[], checks=[], rubric=[], timeout_min=0)
        errors = validate_task(task, REPO_DIR)
        self.assertGreaterEqual(len(errors), 3)
        self.assertTrue(any("base_commit" in e for e in errors))
        self.assertTrue(any("type" in e for e in errors))


from harness import (
    KB_CONTEXT_PATH,
    create_worktree,
    find_opencode_bin,
    inject_kb,
    remove_worktree,
    run_agent,
    run_cmd,
)


class WorktreeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.name = "test-wt"
        cls.wt = create_worktree(cls.name, "a78852fc00dbc1bbd9ecc9ce9b513cbf8da522a5")

    @classmethod
    def tearDownClass(cls):
        remove_worktree(cls.name, cls.wt)

    def test_create_worktree_checks_out_commit(self):
        self.assertTrue((self.wt / "AGENTS.md").exists())
        rc, out, _ = run_cmd(["git", "rev-parse", "--short", "HEAD"], self.wt, 30)
        self.assertEqual(rc, 0)
        self.assertTrue(out.strip())

    def test_inject_kb_appends_to_agents(self):
        if not KB_CONTEXT_PATH.exists():
            self.skipTest("kb_context.md not generated yet")
        inject_kb(self.wt)
        text = (self.wt / "AGENTS.md").read_text(encoding="utf-8")
        self.assertIn("## kb/architecture.md", text)


class AgentRunTest(unittest.TestCase):
    def test_run_agent_uses_binary_and_captures_transcript(self):
        with tempfile.TemporaryDirectory() as tmp:
            wt = Path(tmp)
            fake = wt / "fake_opencode.py"
            fake.write_text(
                "import sys\nprint('AGENT-OUTPUT')\nprint('stderr-here', file=sys.stderr)\n",
                encoding="utf-8",
            )
            rc, transcript = run_agent(
                wt, "prompt-text", 30, sys.executable + " " + str(fake)
            )
        self.assertEqual(rc, 0)
        self.assertIn("AGENT-OUTPUT", transcript)
        self.assertIn("stderr-here", transcript)

    def test_find_opencode_bin_raises_when_missing(self):
        with mock.patch.dict(os.environ, {}, clear=True):
            with self.assertRaises(FileNotFoundError):
                find_opencode_bin()


if __name__ == "__main__":
    unittest.main()
