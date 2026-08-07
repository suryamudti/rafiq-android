import argparse
from pathlib import Path

KB_ORDER = [
    "architecture.md",
    "build-test.md",
    "conventions.md",
    "assets-data.md",
    "api-endpoints.md",
    "glossary.md",
]

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_KB = ROOT / "kb"
DEFAULT_OUT = Path(__file__).resolve().parent / "kb_context.md"


def build_context(kb_dir: Path, out_path: Path) -> None:
    parts = []
    for name in KB_ORDER:
        path = kb_dir / name
        if not path.exists():
            raise FileNotFoundError(f"Missing kb file: {path}")
        body = path.read_text(encoding="utf-8").strip()
        parts.append(f"## kb/{name}\n\n{body}")
    out_path.write_text("\n\n".join(parts) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="Concatenate kb/*.md into kb_context.md")
    parser.add_argument("--kb-dir", type=Path, default=DEFAULT_KB)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = parser.parse_args()
    build_context(args.kb_dir, args.out)
    print(f"Wrote {args.out}")


if __name__ == "__main__":
    main()
