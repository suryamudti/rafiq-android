import argparse
import json
import sys
from pathlib import Path

EVALS_DIR = Path(__file__).resolve().parent
DEFAULT_RESULTS = EVALS_DIR / "results"
TASKS_DIR = EVALS_DIR / "tasks"


def get_task_type(task_id: str, result: dict, tasks_dir: Path | None = None) -> str:
    if "task_type" in result:
        return result["task_type"]
    td = tasks_dir or TASKS_DIR
    if td.exists():
        for p in td.glob("*.json"):
            try:
                data = json.loads(p.read_text(encoding="utf-8"))
                if data.get("id") == task_id or p.stem == task_id:
                    return data.get("type", "coding")
            except Exception:
                continue
    return "coding"


def load_results(results_dir: Path) -> dict[str, dict[str, dict]]:
    results = {}
    if not results_dir.exists():
        return results
    for task_dir in sorted(results_dir.iterdir()):
        if not task_dir.is_dir():
            continue
        variants = {}
        for vf in sorted(task_dir.glob("*.json")):
            try:
                variants[vf.stem] = json.loads(vf.read_text(encoding="utf-8"))
            except Exception:
                continue
        if variants:
            results[task_dir.name] = variants
    return results


def _score(result: dict, task_type: str = "coding") -> float:
    weights = {}
    scores = {}

    checks = result.get("checks") or {}
    if checks:
        passed = [v for v in checks.values() if v]
        scores["checks"] = sum(passed) / len(checks)
        weights["checks"] = 5.0

    if task_type == "coding":
        scores["diff_overlap"] = float(result.get("diff_overlap") or 0.0)
        weights["diff_overlap"] = 3.0

    judge = result.get("judge") or {}
    if not judge.get("skipped") and judge.get("score") is not None:
        scores["judge"] = float(judge.get("score")) / 10.0
        weights["judge"] = 2.0

    total_weight = sum(weights.values())
    if total_weight == 0.0:
        return 0.0

    weighted_sum = sum(scores[k] * weights[k] for k in weights)
    return weighted_sum / total_weight


def render_report(
    results: dict[str, dict[str, dict]],
    tasks_dir: Path | None = None,
    detail: bool = False,
) -> str:
    lines = [
        "# Agent Eval Report (baseline vs kb)",
        "",
        "| task | type | baseline score | kb score | delta |",
        "|---|---|---|---|---|",
    ]
    detail_lines = []

    for task_id in sorted(results):
        variants = results[task_id]
        first_variant = next(iter(variants.values()))
        ttype = get_task_type(task_id, first_variant, tasks_dir)

        base = _score(variants["baseline"], ttype) if "baseline" in variants else None
        kb = _score(variants["kb"], ttype) if "kb" in variants else None
        delta = (kb - base) if (base is not None and kb is not None) else None
        b_str = f"{base:.2f}" if base is not None else "-"
        k_str = f"{kb:.2f}" if kb is not None else "-"
        d_str = f"{delta:+.2f}" if delta is not None else "-"
        lines.append(f"| {task_id} | {ttype} | {b_str} | {k_str} | {d_str} |")

        if detail:
            detail_lines.append(f"\n### {task_id} ({ttype})")
            for v_name in ("baseline", "kb"):
                if v_name in variants:
                    v_res = variants[v_name]
                    v_score = _score(v_res, ttype)
                    detail_lines.append(f"- **{v_name}** (score: {v_score:.2f}):")
                    detail_lines.append(f"  - Exit code: {v_res.get('exit_code')}, timed out: {v_res.get('timed_out')}")
                    if ttype == "coding":
                        detail_lines.append(f"  - Diff overlap: {v_res.get('diff_overlap', 0.0):.2f}")
                    judge = v_res.get("judge") or {}
                    if judge.get("skipped"):
                        detail_lines.append("  - Judge: skipped")
                    else:
                        detail_lines.append(f"  - Judge: {judge.get('score')}/10 ({judge.get('reason', '')})")
                    checks = v_res.get("checks") or {}
                    if checks:
                        detail_lines.append("  - Checks:")
                        for c_name, c_pass in checks.items():
                            mark = "PASS" if c_pass else "FAIL"
                            detail_lines.append(f"    - [{mark}] {c_name}")

    if len(results) > 1:
        bases = []
        kbs = []
        for task_id, v in results.items():
            first_v = next(iter(v.values()))
            ttype = get_task_type(task_id, first_v, tasks_dir)
            if "baseline" in v:
                bases.append(_score(v["baseline"], ttype))
            if "kb" in v:
                kbs.append(_score(v["kb"], ttype))
        if bases and kbs:
            b_avg = sum(bases) / len(bases)
            k_avg = sum(kbs) / len(kbs)
            lines.append(f"| **aggregate** | - | {b_avg:.2f} | {k_avg:.2f} | {k_avg - b_avg:+.2f} |")

    if detail and detail_lines:
        lines.append("\n## Per-Task Details")
        lines.extend(detail_lines)

    return "\n".join(lines)


def generate_json_report(
    results: dict[str, dict[str, dict]],
    tasks_dir: Path | None = None,
) -> dict:
    tasks_data = {}
    for task_id, variants in sorted(results.items()):
        first_variant = next(iter(variants.values()))
        ttype = get_task_type(task_id, first_variant, tasks_dir)
        base = _score(variants["baseline"], ttype) if "baseline" in variants else None
        kb = _score(variants["kb"], ttype) if "kb" in variants else None
        delta = (kb - base) if (base is not None and kb is not None) else None
        tasks_data[task_id] = {
            "type": ttype,
            "baseline_score": base,
            "kb_score": kb,
            "delta": delta,
            "variants": variants,
        }
    return {"tasks": tasks_data}


def main() -> None:
    parser = argparse.ArgumentParser(description="Render baseline-vs-kb eval report")
    parser.add_argument("--results-dir", type=Path, default=DEFAULT_RESULTS)
    parser.add_argument("--tasks-dir", type=Path, default=TASKS_DIR)
    parser.add_argument("--detail", "--verbose", action="store_true", help="Show detailed breakdown per task and check")
    parser.add_argument("--json", action="store_true", help="Output report as JSON")
    args = parser.parse_args()

    results = load_results(args.results_dir)
    if args.json:
        print(json.dumps(generate_json_report(results, args.tasks_dir), indent=2))
        return

    print(render_report(results, args.tasks_dir, detail=args.detail))
    if not results:
        print("(no results yet; run harness first)", file=sys.stderr)


if __name__ == "__main__":
    main()
