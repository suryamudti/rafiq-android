import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from report import load_results, render_report

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


if __name__ == "__main__":
    unittest.main()
