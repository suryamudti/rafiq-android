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


from harness import diff_overlap, gold_diff, judge_llm, run_checks


class GradingTest(unittest.TestCase):
    def test_run_checks_file_touched(self):
        with tempfile.TemporaryDirectory() as tmp:
            wt = Path(tmp)
            (wt / "b.kt").write_text("new file", encoding="utf-8")
            task = Task(
                id="t", title="t", type="coding",
                base_commit="a78852fc00dbc1bbd9ecc9ce9b513cbf8da522a5",
                prompt="p",
                checks=[
                    {"kind": "file_touched", "files": ["b.kt"]},
                    {"kind": "file_touched", "files": ["missing.kt"]},
                    {"kind": "transcript_mentions", "files": ["data/x.kt"]},
                ],
            )
            with mock.patch(
                "harness._changed_paths", return_value={"b.kt", "data/x.kt"}
            ):
                results = run_checks(task, wt, transcript="the answer is data/x.kt")
        self.assertTrue(results["file_touched:b.kt"])
        self.assertFalse(results["file_touched:missing.kt"])
        self.assertTrue(results["transcript_mentions:data/x.kt"])

    def test_diff_overlap(self):
        agent = "+line one\n+line two\n+other line\n context\n"
        gold = "+line one\n+line two\n+line three\n"
        self.assertAlmostEqual(diff_overlap(agent, gold), 2 / 3)

    def test_diff_overlap_empty_gold(self):
        self.assertEqual(diff_overlap("+x\n", ""), 0.0)

    def test_gold_diff_returns_added_lines(self):
        task = Task(
            id="t", title="t", type="coding",
            base_commit="a78852fc00dbc1bbd9ecc9ce9b513cbf8da522a5",
            gold_commit="096b94d2191997e73938eb526152779cdc6659f3",
            prompt="p",
        )
        d = gold_diff(task)
        self.assertIn("CAST(sura AS INTEGER)", d)

    def test_judge_llm_returns_dict(self):
        with tempfile.TemporaryDirectory() as tmp:
            wt = Path(tmp)
            fake = wt / "fake_judge.py"
            fake.write_text(
                "import json\nprint(json.dumps({'score': 7, 'reason': 'ok'}))\n",
                encoding="utf-8",
            )
            task = Task(id="t", title="t", type="retrieval",
                        base_commit="0" * 40, prompt="p", rubric=["r"])
            result = judge_llm(task, "transcript", "diff", sys.executable + " " + str(fake))
        self.assertEqual(result["score"], 7)


import json as _json

from harness import RESULTS_DIR, _resolve_task_arg, run_variant, write_result


class OrchestrationTest(unittest.TestCase):
    def test_write_result_roundtrip(self):
        with tempfile.TemporaryDirectory() as tmp:
            results = Path(tmp)
            result = {"task_id": "rf-zz", "variant": "baseline", "exit_code": 0}
            path = write_result("rf-zz", "baseline", result, results)
            self.assertTrue(path.exists())
            data = _json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual(data["task_id"], "rf-zz")
            self.assertEqual(data["variant"], "baseline")

    def test_resolve_task_arg_matches_by_id(self):
        with tempfile.TemporaryDirectory() as tmp:
            seed = {
                "id": "rf-001",
                "title": "t",
                "type": "retrieval",
                "base_commit": "a78852fc00dbc1bbd9ecc9ce9b513cbf8da522a5",
                "prompt": "p",
            }
            (Path(tmp) / "rf-001-fts3-translation-query.json").write_text(
                _json.dumps(seed), encoding="utf-8"
            )
            with mock.patch("harness.TASKS_DIR", Path(tmp)):
                tasks = _resolve_task_arg("rf-001")
        self.assertEqual(len(tasks), 1)
        self.assertEqual(tasks[0].id, "rf-001")

    def test_resolve_task_arg_matches_by_stem(self):
        with tempfile.TemporaryDirectory() as tmp:
            seed = {
                "id": "rf-001",
                "title": "t",
                "type": "retrieval",
                "base_commit": "a78852fc00dbc1bbd9ecc9ce9b513cbf8da522a5",
                "prompt": "p",
            }
            (Path(tmp) / "rf-001-fts3-translation-query.json").write_text(
                _json.dumps(seed), encoding="utf-8"
            )
            with mock.patch("harness.TASKS_DIR", Path(tmp)):
                tasks = _resolve_task_arg("rf-001-fts3-translation-query")
        self.assertEqual(len(tasks), 1)
        self.assertEqual(tasks[0].id, "rf-001")

    def test_resolve_task_arg_unknown_returns_empty(self):
        with tempfile.TemporaryDirectory() as tmp:
            with mock.patch("harness.TASKS_DIR", Path(tmp)):
                tasks = _resolve_task_arg("nope")
        self.assertEqual(tasks, [])

    def test_run_variant_with_fake_agent(self):
        with tempfile.TemporaryDirectory() as tmp:
            wt = Path(tmp)
            fake = wt / "fake_agent.py"
            fake.write_text("print('done')\n", encoding="utf-8")
            task = Task(
                id="rf-smoke", title="t", type="retrieval",
                base_commit="a78852fc00dbc1bbd9ecc9ce9b513cbf8da522a5",
                prompt="Summarize the repo.",
                checks=[{"kind": "transcript_mentions", "files": ["AGENTS.md"]}],
            )
            result = run_variant(
                task, "baseline",
                opencode_bin=sys.executable + " " + str(fake),
                timeout_min=1, no_judge=True, keep_worktrees=False,
                results_dir=Path(tmp) / "res", use_kb=False,
            )
        self.assertEqual(result["task_id"], "rf-smoke")
        self.assertEqual(result["variant"], "baseline")
        self.assertIn("checks", result)
        self.assertIn("transcript", result)
        self.assertFalse(result["timed_out"])


if __name__ == "__main__":
    unittest.main()
