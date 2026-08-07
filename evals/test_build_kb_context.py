import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from build_kb_context import KB_ORDER, build_context


class BuildContextTest(unittest.TestCase):
    def test_build_context_concatenates_in_order_with_headers(self):
        with tempfile.TemporaryDirectory() as tmp:
            kb = Path(tmp) / "kb"
            kb.mkdir()
            for name in KB_ORDER:
                (kb / name).write_text(f"BODY-OF-{name}", encoding="utf-8")
            out = Path(tmp) / "kb_context.md"
            build_context(kb, out)
            text = out.read_text(encoding="utf-8")
        self.assertIn("## kb/architecture.md", text)
        self.assertIn("## kb/glossary.md", text)
        self.assertIn("BODY-OF-architecture.md", text)
        self.assertIn("BODY-OF-glossary.md", text)
        self.assertLess(
            text.index("## kb/architecture.md"),
            text.index("## kb/api-endpoints.md"),
        )

    def test_build_context_raises_on_missing_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            kb = Path(tmp) / "kb"
            kb.mkdir()
            out = Path(tmp) / "kb_context.md"
            with self.assertRaises(FileNotFoundError):
                build_context(kb, out)


if __name__ == "__main__":
    unittest.main()
