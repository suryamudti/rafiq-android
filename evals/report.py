import argparse
import json
import sys
from pathlib import Path

EVALS_DIR = Path(__file__).resolve().parent
DEFAULT_RESULTS = EVALS_DIR / "results"


def load_results(results_dir: Path) -> dict[str, dict[str, dict]]:
    results = {}
    if not results_dir.exists():
        return results
    for task_dir in sorted(results_dir.iterdir()):
        if not task_dir.is_dir():
            continue
        variants = {}
        for vf in sorted(task_dir.glob("*.json")):
            variants[vf.stem] = json.loads(vf.read_text(encoding="utf-8"))
        if variants:
            results[task_dir.name] = variants
    return results


def _score(result: dict) -> float:
    checks = result.get("checks") or {}
    passed = [v for v in checks.values() if v]
    check_score = (sum(passed) / len(checks)) if checks else 0.0
    judge = result.get("judge") or {}
    judge_score = (judge.get("score") or 0) / 10.0 if judge.get("score") is not None else 0.0
    return 0.5 * check_score + 0.3 * (result.get("diff_overlap") or 0.0) + 0.2 * judge_score


def render_report(results: dict[str, dict[str, dict]]) -> str:
    lines = [
        "# Agent Eval Report (baseline vs kb)",
        "",
        "| task | baseline score | kb score | delta |",
        "|---|---|---|---|",
    ]
    for task_id in sorted(results):
        variants = results[task_id]
        base = _score(variants["baseline"]) if "baseline" in variants else None
        kb = _score(variants["kb"]) if "kb" in variants else None
        delta = (kb - base) if (base is not None and kb is not None) else None
        b_str = f"{base:.2f}" if base is not None else "-"
        k_str = f"{kb:.2f}" if kb is not None else "-"
        d_str = f"{delta:+.2f}" if delta is not None else "-"
        lines.append(f"| {task_id} | {b_str} | {k_str} | {d_str} |")
    if len(results) > 1:
        bases = [_score(v["baseline"]) for v in results.values() if "baseline" in v]
        kbs = [_score(v["kb"]) for v in results.values() if "kb" in v]
        if bases:
            lines.append(f"| **aggregate** | {sum(bases)/len(bases):.2f} | {sum(kbs)/len(kbs):.2f} | {sum(kbs)/len(kbs) - sum(bases)/len(bases):+.2f} |")
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser(description="Render baseline-vs-kb eval report")
    parser.add_argument("--results-dir", type=Path, default=DEFAULT_RESULTS)
    args = parser.parse_args()
    results = load_results(args.results_dir)
    print(render_report(results))
    if not results:
        print("(no results yet; run harness first)", file=sys.stderr)


if __name__ == "__main__":
    main()
