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


if __name__ == "__main__":
    unittest.main()
