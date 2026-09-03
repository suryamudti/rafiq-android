import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from report import _score, generate_json_report, get_task_type, load_results, render_report

SAMPLE = {
    "task_id": "rf-001",
    "variant": "baseline",
    "exit_code": 0,
    "timed_out": False,
    "checks": {"file_touched:a": True, "test:x": False},
    "diff_overlap": 0.5,
    "judge": {"score": 6},
}


class ReportTest(unittest.TestCase):
    def test_load_results_nested(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp)
            task_dir = d / "rf-001"
            task_dir.mkdir(parents=True)
            (task_dir / "baseline.json").write_text(json.dumps(SAMPLE), encoding="utf-8")
            results = load_results(d)
        self.assertIn("rf-001", results)
        self.assertIn("baseline", results["rf-001"])
        self.assertEqual(results["rf-001"]["baseline"]["diff_overlap"], 0.5)

    def test_render_report_table(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp)
            for v in ("baseline", "kb"):
                td = d / "rf-001"
                td.mkdir(parents=True, exist_ok=True)
                payload = dict(SAMPLE, variant=v, judge={"score": 6 if v == "kb" else 4})
                (td / f"{v}.json").write_text(json.dumps(payload), encoding="utf-8")
            results = load_results(d)
            text = render_report(results)
        self.assertIn("rf-001", text)
        self.assertIn("baseline", text)
        self.assertIn("kb", text)

    def test_render_report_detail(self):
        with tempfile.TemporaryDirectory() as tmp:
            d = Path(tmp)
            td = d / "rf-001"
            td.mkdir(parents=True)
            (td / "baseline.json").write_text(json.dumps(SAMPLE), encoding="utf-8")
            results = load_results(d)
            text = render_report(results, detail=True)
        self.assertIn("Per-Task Details", text)
        self.assertIn("[PASS] file_touched:a", text)
        self.assertIn("[FAIL] test:x", text)

    def test_generate_json_report(self):
        results = {"rf-001": {"baseline": SAMPLE}}
        data = generate_json_report(results)
        self.assertIn("tasks", data)
        self.assertIn("rf-001", data["tasks"])
        self.assertIsNotNone(data["tasks"]["rf-001"]["baseline_score"])

    def test_score_retrieval_without_judge_normalizes_to_checks(self):
        res = {
            "checks": {"c1": True, "c2": True},
            "diff_overlap": 0.0,
            "judge": {"skipped": True},
        }
        score = _score(res, task_type="retrieval")
        self.assertEqual(score, 1.0)

    def test_score_retrieval_with_judge(self):
        res = {
            "checks": {"c1": True, "c2": True},
            "diff_overlap": 0.0,
            "judge": {"score": 5},
        }
        # weights: checks 5.0 (score 1.0), judge 2.0 (score 0.5) => (5*1 + 2*0.5) / 7 = 6/7 ≈ 0.857
        score = _score(res, task_type="retrieval")
        self.assertAlmostEqual(score, 6.0 / 7.0)

    def test_score_coding_without_judge_normalizes_to_one(self):
        res = {
            "checks": {"c1": True},
            "diff_overlap": 1.0,
            "judge": {"skipped": True},
        }
        score = _score(res, task_type="coding")
        self.assertEqual(score, 1.0)

    def test_score_coding_all_components(self):
        res = {
            "checks": {"c1": True, "c2": False},  # 0.5 * 5.0 = 2.5
            "diff_overlap": 0.8,                  # 0.8 * 3.0 = 2.4
            "judge": {"score": 7},                # 0.7 * 2.0 = 1.4
        }
        # total_weight = 10.0, weighted_sum = 2.5 + 2.4 + 1.4 = 6.3 => 0.63
        score = _score(res, task_type="coding")
        self.assertAlmostEqual(score, 0.63)


if __name__ == "__main__":
    unittest.main()
