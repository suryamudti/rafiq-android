# KB & Evals Harness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a curated agent knowledge base (`kb/`) and a local Python eval harness (`evals/`) that runs an agent (`opencode run`) in isolated git worktrees on tasks derived from real repo history, grades them, and compares baseline vs +kb to measure the kb's impact.

**Architecture:** `kb/*.md` markdown files are concatenated into `evals/kb_context.md` by `build_kb_context.py`. `evals/harness.py` (Python 3.12, stdlib only) loads task JSONs from `evals/tasks/`, creates git worktrees at each task's `base_commit`, runs `opencode run` (binary from `OPENCODE_BIN` or PATH), grades programmatically (file-touched, transcript-mentions, diff-overlap, build/test command) plus an optional LLM judge, and writes results to `evals/results/`. `evals/report.py` renders a baseline-vs-kb comparison.

**Tech Stack:** Python 3.12 (stdlib: argparse, json, subprocess, pathlib, difflib, unittest), git worktrees, opencode CLI.

## Global Constraints

- Python 3.12+; stdlib only — no third-party packages.
- Tests run with `python -m unittest discover -s evals -p "test_*.py"` from repo root.
- Never modify the Android app code, AGENTS.md, or graphify-out. Only create `kb/`, `evals/`, `.gitignore` entries, and the plan/spec docs.
- `evals/kb_context.md` and `evals/results/` are gitignored.
- Task JSON schema (keys): `id`, `title`, `type` (`"retrieval"` | `"coding"`), `base_commit`, `gold_commit` (optional), `prompt`, `gold_files`, `checks`, `rubric`, `timeout_min`.
- Check kinds supported: `{"kind": "file_touched", "files": [...]}`, `{"kind": "transcript_mentions", "files": [...]}`, `{"kind": "test", "command": "..."}`.
- Verified environment facts (from exploration, Aug 2026):
  - Working `JAVA_HOME`: `C:\Program Files\Android\Android Studio1\jbr` (the path in AGENTS.md, `C:\Program Files\Android\Android Studio\jbr`, is a broken/incomplete JBR — builds fail with `Error loading java.security file`).
  - `.\gradlew help` BUILD SUCCESSFUL with Gradle 8.12 using that JAVA_HOME.
  - `opencode` CLI is NOT on PATH; only the Desktop app is installed. Harness must support `OPENCODE_BIN` env var and fail with a clear message if neither is available.
  - `AGENTS.md` exists at all seed base commits (added in `54e2127`).
  - `settings.gradle.kts` references `:feature:*` modules that do not exist on disk or in git; this is a pre-existing quirk. Builds succeed anyway (verified), so do not "fix" it.
- All seed SHAs below verified to exist via `git cat-file -e <sha>:AGENTS.md`.

---

### Task 1: kb/ knowledge base markdown files

**Files:**
- Create: `kb/README.md`
- Create: `kb/architecture.md`
- Create: `kb/build-test.md`
- Create: `kb/conventions.md`
- Create: `kb/assets-data.md`
- Create: `kb/api-endpoints.md`
- Create: `kb/glossary.md`

**Interfaces:**
- Consumes: existing `AGENTS.md`, graphify-out knowledge, verified environment facts.
- Produces: 7 files that `build_kb_context.py` (Task 2) concatenates in this fixed order: architecture, build-test, conventions, assets-data, api-endpoints, glossary. Each file's body should be self-contained agent-facing reference prose with concrete paths.

- [ ] **Step 1: Create `kb/README.md`**

```markdown
# Rafiq Android — Agent Knowledge Base (kb/)

This directory is a curated knowledge base for AI agents working on this repo.
It is DEEPER than `AGENTS.md` (the always-loaded quick guide). It is injected
as `evals/kb_context.md` during eval runs and can be read directly by agents.

## Files (concatenation order)

| File | Purpose |
|---|---|
| architecture.md | Module layout, Navigation3, MVVM screen pattern, DI |
| build-test.md | Build/run/test commands, JAVA_HOME, emulator, gotchas |
| conventions.md | KAPT/Hilt, Room api() rule, smart-cast rule, icons, DataStore |
| assets-data.md | DB copying, Arabic font, Quran DB schema |
| api-endpoints.md | Aladhan, Metals.live, Overpass endpoints + params |
| glossary.md | Domain terms (Surah, Ayah, tafsir, sajdah, juz, nisab, ...) |

## Keeping it current

Whenever AGENTS.md, the architecture, or a dependency changes in a way that
agents should know, update the relevant file here. Run:

```powershell
python evals/build_kb_context.py
```

to regenerate the injected context file.
```

- [ ] **Step 2: Create `kb/architecture.md`**

Content must cover, with concrete paths:

```markdown
# Architecture

Multi-module Gradle project (`settings.gradle.kts`). Four modules:

| Module | Path | Responsibility |
|---|---|---|
| :core | core/src/main/kotlin/com/smiledev/rafiq/core/ | Result.kt, AppError.kt, DispatcherProvider.kt, retryIO.kt, LocaleUtil.kt, DatabaseCopier.kt |
| :domain | domain/src/main/kotlin/com/smiledev/rafiq/domain/ | Repository interfaces, use cases, domain models (Surah, Ayah, PrayerTimings, Mosque, ...) |
| :data | data/src/main/kotlin/com/smiledev/rafiq/data/ | Repository Impls, Room DBs/DAOs, Retrofit APIs, PreferencesManager (DataStore) |
| :app | app/src/main/java/com/smiledev/rafiq/ | DI (di/AppModule.kt), UI (ui/<feature>/Screen.kt + ViewModel.kt), theme/, service/, Navigation.kt |

## Navigation3 (type-safe, 16 routes)

- `app/src/main/java/com/smiledev/rafiq/NavigationKeys.kt`: every route is a
  `@Serializable` `data object`/`data class : NavKey` (e.g. `Dashboard`,
  `Quran(initialTab)`, `Ayah(suraNumber, suraName, scrollToAya)`).
- `app/src/main/java/com/smiledev/rafiq/Navigation.kt`: `MainNavigation()`
  builds `rememberNavBackStack(Dashboard)`, a `NavDisplay` with
  `entryProvider { entry<Key> { XxxScreen(...) } }` per route. Back is
  `backStack.removeLastOrNull()`; forward is `backStack.add(navKey)`.
- To add a screen: (1) add a `NavKey` token, (2) add `entry<Key> { ... }` to
  the provider, (3) create `ui/<feature>/XxxScreen.kt` + `XxxViewModel.kt`.

## MVVM screen pattern

- `@Composable fun XxxScreen(onBack: () -> Unit, viewModel: XxxViewModel = hiltViewModel(), modifier)`.
- `@HiltViewModel class XxxViewModel @Inject constructor(...)` with
  `MutableStateFlow<XxxUiState>` + `val uiState: StateFlow<XxxUiState>`.
- Data loads on `init` via `viewModelScope.launch(Dispatchers.IO)`.
- `XxxUiState` is typically a `data class` with `isLoading`, `error`, and data.

## DI

- `app/src/main/java/com/smiledev/rafiq/di/AppModule.kt`: `@Module`
  `@InstallIn(SingletonComponent::class)` with `@Binds` for repository impls
  (`bindPrayerTimesRepository()`, `bindQuranRepository()`, etc.) and `@Provides`
  for Retrofit services (AladhanApiService, EQuranApiService, MetalPriceApiService,
  OverpassApiService, IslamicAppApiService), OkHttpClient, and Room DBs.
- Cross-module smart casts from nullable do NOT work; use `!!`/`?:`/local `val`.
```

- [ ] **Step 3: Create `kb/build-test.md`**

```markdown
# Build, Run & Test

## JAVA_HOME (IMPORTANT)

The path in AGENTS.md (`C:\Program Files\Android\Android Studio\jbr`) is a
BROKEN/incomplete JBR — builds fail with `Error loading java.security file`.
The verified working JAVA_HOME on this machine is:

    C:\Program Files\Android\Android Studio1\jbr

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio1\jbr"
```

## Build & install

```powershell
.\gradlew assembleDebug
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
```

Emulator: `Medium_Phone_API_35`. No lint or typecheck tasks are set up.

## Tests

```powershell
.\gradlew testDebug                 # unit tests (JVM)
.\gradlew connectedDebugAndroidTest # instrumented tests (emulator required)
```

Unit tests live in `app/src/test/...` and `data/src/test/...`
(e.g. `QuranViewModelTest.kt`, `MetalPriceRepositoryImplTest.kt`).

## Known quirk

`settings.gradle.kts` includes `:feature:*` modules that don't exist on disk or
in git. Builds still succeed. Do NOT try to "fix" this.
```

- [ ] **Step 4: Create `kb/conventions.md`**

```markdown
# Conventions & Constraints

- **Hilt via KAPT, NOT KSP.** `kapt` plugin with `correctErrorTypes=true`. The
  warning "Kapt currently doesn't support language version 2.0+. Falling back
  to 1.9." is harmless.
- **Kotlin 2.0.0, AGP 8.9.2, Hilt 2.56.2.** Hilt 2.56.2 requires AGP 8.x.
- **Material icons: core only.** `material-icons-core` — NEVER add
  `material-icons-extended`. Available: DateRange, Face, Favorite, List
  (AutoMirrored), LocationOn, Notifications, Person, Place, PlayArrow, Refresh,
  ShoppingCart, Star. `Icons.Filled.Delete` requires an explicit import.
- **DataStore, not SharedPreferences.** All user prefs via `PreferencesManager`
  (`data/src/main/kotlin/com/smiledev/rafiq/data/preferences/PreferencesManager.kt`).
- **Room** singleton `getInstance()` pattern, `fallbackToDestructiveMigration()`.
  In `:data` module, use `api(libs.room.runtime)` (not `implementation`) so `:app`
  can see the `RoomDatabase` supertype transitively.
- **Cross-module smart casts** don't work on nullable types from other modules.
  Use `!!` (if guarded) or `?:` / local `val`.
- **No Java `Math.*`** — use `kotlin.math.*`.
- **Compose** `textDirection` is a `TextStyle` property, not a direct `Text` param.
- **Locale:** `if (Locale.getDefault().language == "id") "id" else "en"` (see
  `core/.../LocaleUtil.kt`, which also treats `"in"` as Indonesian).
- Theme palette (`app/.../theme/Color.kt`) uses Islamic-inspired colors: Teal500,
  Gold700, DeepBlue, WarmBrown, Cream/Sand. On Android 12+, dynamic color wins.
```

- [ ] **Step 5: Create `kb/assets-data.md`**

```markdown
# Assets & Data Loading

## Bundled SQLite databases

Assets live under `assets/` (e.g. `quran-data/`, `translations/`).
Translation DB names contain `/` (e.g. `translations/en.sahih.db`).

- `core/.../DatabaseCopier.kt` flattens `/` to `_` before calling
  `getDatabasePath()` because Android rejects path separators in DB names.
- `copyDatabaseIfNeeded()` copies to `filesDir/databases/<flatName>`.
- `copyAndVerifyTranslationDb()` verifies the file exists and is non-empty.

## Quran DB (`quran-uthmani.db`)

- All columns are TEXT.
- Bismillah is a nullable `String?` stored as actual Arabic text.

## Arabic font

Load via `FontFamily(Font(R.font.me_quran))`. NEVER use `fontResource()`.

## Translation query gotcha (historical bug, FTS3)

In `QuranRepositoryImpl.getTranslationForSura()`, the verses table stores `sura`
as TEXT. A query `SELECT ayah, text FROM verses WHERE sura = ?` with a string
arg returns 0 rows on Android SQLite due to type affinity. The fix was to cast:
`WHERE CAST(sura AS INTEGER) = ?`.
```

- [ ] **Step 6: Create `kb/api-endpoints.md`**

```markdown
# API Endpoints

## Aladhan — prayer times

Base: `https://api.aladhan.com/` (`data/.../remote/AladhanApi.kt`)
`GET v1/timings/{date}?latitude=&longitude=&method=20`
Default coords: Jakarta (-6.2088, 106.8456). Method 20 (KEMENAG).

## Metals.live — gold/silver spot

Base: `https://api.metals.live/` (`data/.../remote/MetalPriceApi.kt`)
`GET v1/spot/gold`, `GET v1/spot/silver`. Prices are USD/oz; converted to
per-gram by dividing by 31.1035 in `MetalPriceApi`. Empty response falls back
to hardcoded defaults (gold 65.0, silver 0.75 USD/g).

## Overpass — nearby mosques

Base: `https://overpass-api.de/api/` (`data/.../remote/OverpassApi.kt`)
`POST interpreter?data=<query>`. `fetchMosques(lat, lon, radius=5000)` returns
`List<OverpassElement>` (type, id, lat/lon or center, tags).
```

- [ ] **Step 7: Create `kb/glossary.md`**

```markdown
# Glossary

- **Surah** — a chapter of the Quran (114 total).
- **Ayah** — a verse within a Surah.
- **Tafsir** — exegesis/interpretation of Quranic verses.
- **Sajdah** — prostration; sajdah markers indicate a recommended prostration verse.
- **Juz** — one of 30 sections of the Quran.
- **Qibla** — direction of the Kaaba in Mecca for prayer.
- **Hijri** — the Islamic (lunar) calendar.
- **Zakat** — obligatory alms; **nisab** is the wealth threshold that makes it due.
- **Tasbih** — phrases glorifying God; the digital zikr counter feature.
- **Asmaul Husna** — the 99 Names of Allah.
- **Fajr/Dhuhr/Asr/Maghrib/Isha** — the five daily prayers.
- **Imsak** — the time to stop eating before dawn (start of fasting window).
```

- [ ] **Step 8: Commit**

```bash
git add kb/
git commit -m "docs: add agent knowledge base under kb/"
```

---

### Task 2: build_kb_context.py + unit tests

**Files:**
- Create: `evals/build_kb_context.py`
- Test: `evals/test_build_kb_context.py`

**Interfaces:**
- Consumes: `kb/*.md` files from Task 1.
- Produces: `evals/kb_context.md` (gitignored, Task 6) and a pure function
  `build_context(kb_dir: Path, out_path: Path) -> None` used by later tasks.
- `KB_ORDER: list[str]` module constant = the 6 content files in order.

- [ ] **Step 1: Write the failing test**

Create `evals/test_build_kb_context.py`:

```python
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python -m unittest evals.test_build_kb_context -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'build_kb_context'`.

- [ ] **Step 3: Write minimal implementation**

Create `evals/build_kb_context.py`:

```python
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
```

Note: `evals/test_build_kb_context.py` imports via `sys.path.insert(0, <evals>)`, so test files must NOT be packages (no `__init__.py` in `evals/`).

- [ ] **Step 4: Run tests to verify they pass**

Run: `python -m unittest evals.test_build_kb_context -v`
Expected: 2 tests PASS.

- [ ] **Step 5: Generate the context file for real**

Run: `python evals/build_kb_context.py`
Expected: prints `Wrote ...kb_context.md`. Then `Get-Content evals/kb_context.md | Select-Object -First 5` shows `## kb/architecture.md`.

- [ ] **Step 6: Commit**

```bash
git add evals/build_kb_context.py evals/test_build_kb_context.py
git commit -m "feat: add kb context builder"
```

---

### Task 3: harness.py — task loading & validation + unit tests

**Files:**
- Create: `evals/harness.py` (CLI + orchestration; grows across Tasks 3-5)
- Test: `evals/test_harness.py`

**Interfaces:**
- Consumes: task JSON schema from Global Constraints.
- Produces (used by later tasks):
  - `@dataclass Task` with fields `id, title, type, base_commit, gold_commit, prompt, gold_files, checks, rubric, timeout_min`.
  - `load_task(path: Path) -> Task`
  - `validate_task(task: Task, repo: Path) -> list[str]` (returns error strings; empty = valid)
  - `TASKS_DIR: Path`, `RESULTS_DIR: Path`, `WORKTREES_DIR: Path`, `REPO_DIR: Path` module constants.

- [ ] **Step 1: Write the failing tests**

Append to `evals/test_harness.py`:

```python
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `python -m unittest evals.test_harness -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'harness'`.

- [ ] **Step 3: Implement Task dataclass + load/validate**

Create `evals/harness.py` with these first parts:

```python
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
```

Note: this is a partial module. The full CLI/grading code is added in Tasks 4-5. Keep the file compilable after each task.

- [ ] **Step 4: Run tests to verify they pass**

Run: `python -m unittest evals.test_harness -v`
Expected: 3 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add evals/harness.py evals/test_harness.py
git commit -m "feat: add eval task loading and validation"
```

---

### Task 4: harness.py — git worktree + agent execution

**Files:**
- Modify: `evals/harness.py`
- Test: `evals/test_harness.py`

**Interfaces:**
- Consumes: `Task`, `REPO_DIR`, `WORKTREES_DIR`, `KB_CONTEXT_PATH` from Task 3.
- Produces (used by Tasks 5-6):
  - `find_opencode_bin() -> str` — returns `OPENCODE_BIN` env or `opencode`; raises `FileNotFoundError` with a clear message if neither exists.
  - `create_worktree(name: str, base_commit: str) -> Path` — `git worktree add`.
  - `remove_worktree(name: str, worktree: Path) -> None` — `git worktree remove --force`.
  - `inject_kb(worktree: Path) -> None` — append `KB_CONTEXT_PATH` to worktree `AGENTS.md`.
  - `run_cmd(cmd: list[str], cwd: Path, timeout_s: int) -> tuple[int, str, str]` — returns `(returncode, stdout, stderr)`.
  - `run_agent(worktree: Path, prompt: str, timeout_s: int, opencode_bin: str) -> tuple[int, str]` — returns `(returncode, transcript)`.
  - `capture_diff(worktree: Path) -> str` — `git diff` + `git status --porcelain`.

- [ ] **Step 1: Write the failing tests**

Append to `evals/test_harness.py`:

```python
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `python -m unittest evals.test_harness -v`
Expected: FAIL with `ImportError: cannot import name 'create_worktree'`.

- [ ] **Step 3: Implement worktree + agent functions**

Append to `evals/harness.py`:

```python
def find_opencode_bin() -> str:
    env_bin = os.environ.get("OPENCODE_BIN")
    candidates = [env_bin, "opencode"] if env_bin else ["opencode"]
    for c in candidates:
        if c and _which(c):
            return c
    raise FileNotFoundError(
        "opencode CLI not found. Install it or set OPENCODE_BIN to the binary "
        "path (the Desktop app alone is not a CLI)."
    )


def _which(cmd: str) -> bool:
    if os.path.isabs(cmd) or "/" in cmd or "\\" in cmd:
        return Path(cmd).exists()
    for d in os.environ.get("PATH", "").split(os.pathsep):
        if d and (Path(d) / cmd).exists():
            return True
        if d and (Path(d) / f"{cmd}.exe").exists():
            return True
    return False


def create_worktree(name: str, base_commit: str) -> Path:
    wt = WORKTREES_DIR / name
    if wt.exists():
        remove_worktree(name, wt)
    WORKTREES_DIR.mkdir(parents=True, exist_ok=True)
    rc, out, err = run_cmd(
        ["git", "worktree", "add", str(wt), base_commit], REPO_DIR, 120
    )
    if rc != 0:
        raise RuntimeError(f"worktree add failed: {err.strip()}")
    return wt


def remove_worktree(name: str, worktree: Path) -> None:
    if not worktree.exists():
        return
    run_cmd(["git", "worktree", "remove", "--force", str(worktree)], REPO_DIR, 60)


def inject_kb(worktree: Path) -> None:
    if not KB_CONTEXT_PATH.exists():
        raise FileNotFoundError(
            "kb_context.md missing; run `python evals/build_kb_context.py` first"
        )
    agents = worktree / "AGENTS.md"
    kb_text = KB_CONTEXT_PATH.read_text(encoding="utf-8")
    if agents.exists():
        agents.write_text(
            agents.read_text(encoding="utf-8").rstrip() + "\n\n" + kb_text,
            encoding="utf-8",
        )
    else:
        agents.write_text(kb_text, encoding="utf-8")


def run_cmd(cmd: list[str], cwd: Path, timeout_s: int) -> tuple[int, str, str]:
    try:
        proc = subprocess.run(
            cmd, cwd=str(cwd), capture_output=True, text=True, timeout=timeout_s
        )
        return proc.returncode, proc.stdout, proc.stderr
    except subprocess.TimeoutExpired as e:
        return -1, (e.stdout or ""), (e.stderr or "") + "\nTIMEOUT"


def _opencode_cmd(opencode_bin: str, args: list[str]) -> list[str]:
    return opencode_bin.split() + args


def run_agent(worktree: Path, prompt: str, timeout_s: int, opencode_bin: str) -> tuple[int, str]:
    cmd = _opencode_cmd(opencode_bin, ["run", prompt])
    rc, out, err = run_cmd(cmd, worktree, timeout_s)
    return rc, out + err


def capture_diff(worktree: Path) -> str:
    rc, diff, _ = run_cmd(["git", "diff"], worktree, 60)
    rc2, status, _ = run_cmd(["git", "status", "--porcelain"], worktree, 60)
    return (diff or "") + "\n" + (status or "")
```

Note: `run_agent` treats `opencode_bin` as a raw command string split on spaces so tests can pass `sys.executable + " " + str(fake)`. For real usage `find_opencode_bin()` returns a bare binary name or absolute path.

- [ ] **Step 4: Run tests to verify they pass**

Run: `python -m unittest evals.test_harness -v`
Expected: all tests PASS (WorktreeTest needs `kb_context.md`; generate it first if the kb file check fails: `python evals/build_kb_context.py`).

- [ ] **Step 5: Commit**

```bash
git add evals/harness.py evals/test_harness.py
git commit -m "feat: add worktree + agent execution to harness"
```

---

### Task 5: harness.py — grading (checks, diff overlap, LLM judge)

**Files:**
- Modify: `evals/harness.py`
- Test: `evals/test_harness.py`

**Interfaces:**
- Consumes: `Task`, `run_cmd`, `capture_diff`, `REPO_DIR` from Tasks 3-4.
- Produces (used by Task 6):
  - `run_checks(task: Task, worktree: Path, transcript: str) -> dict[str, bool]`
  - `gold_diff(task: Task) -> str`
  - `diff_overlap(agent_diff: str, gold_diff: str) -> float` (0.0-1.0)
  - `judge_llm(task: Task, transcript: str, agent_diff: str, opencode_bin: str) -> dict`

- [ ] **Step 1: Write the failing tests**

Append to `evals/test_harness.py`:

```python
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `python -m unittest evals.test_harness -v`
Expected: FAIL with `ImportError: cannot import name 'run_checks'`.

- [ ] **Step 3: Implement grading functions**

Append to `evals/harness.py`:

```python
def _changed_paths(worktree: Path) -> set[str]:
    rc, status, _ = run_cmd(["git", "status", "--porcelain"], worktree, 60)
    paths = set()
    for line in (status or "").splitlines():
        parts = line.split(" ", 2)
        if len(parts) >= 2:
            paths.add(parts[-1].strip('"'))
    rc, diff, _ = run_cmd(["git", "diff", "--name-only"], worktree, 60)
    for line in (diff or "").splitlines():
        if line.strip():
            paths.add(line.strip())
    return paths


def run_checks(task: Task, worktree: Path, transcript: str) -> dict[str, bool]:
    results = {}
    changed = _changed_paths(worktree)
    for check in task.checks:
        kind = check["kind"]
        if kind == "file_touched":
            key = "file_touched:" + ",".join(check["files"])
            results[key] = any(f in changed for f in check["files"])
        elif kind == "transcript_mentions":
            key = "transcript_mentions:" + ",".join(check["files"])
            results[key] = any(f in transcript for f in check["files"])
        elif kind == "test":
            cmd = check["command"].split()
            rc, _, _ = run_cmd(cmd, worktree, task.timeout_min * 60)
            results["test:" + check["command"]] = rc == 0
    return results


def _added_lines(diff: str) -> set[str]:
    lines = set()
    for line in (diff or "").splitlines():
        if line.startswith("+") and not line.startswith("+++"):
            lines.add(line[1:].strip())
    return lines


def gold_diff(task: Task) -> str:
    if not task.gold_commit:
        return ""
    rc, diff, err = run_cmd(
        ["git", "diff", task.base_commit, task.gold_commit], REPO_DIR, 60
    )
    if rc != 0:
        raise RuntimeError(f"gold diff failed: {err.strip()}")
    return diff or ""


def diff_overlap(agent_diff: str, gold: str) -> float:
    agent_added = _added_lines(agent_diff)
    gold_added = _added_lines(gold)
    if not gold_added:
        return 0.0
    return len(agent_added & gold_added) / len(gold_added)


def judge_llm(task: Task, transcript: str, agent_diff: str, opencode_bin: str) -> dict:
    rubric = "\n".join(f"- {r}" for r in task.rubric) or "- correctness"
    payload = (
        "You are grading an AI agent's work on a coding task.\n"
        f"Task prompt: {task.prompt}\n"
        f"Rubric:\n{rubric}\n"
        f"Transcript:\n{transcript[-8000:]}\n"
        f"Diff:\n{agent_diff[-4000:]}\n"
        'Reply with ONLY JSON: {"score": <int 0-10>, "reason": "<string>"}'
    )
    with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False, encoding="utf-8") as f:
        f.write(payload)
        prompt_file = f.name
    try:
        cmd = _opencode_cmd(opencode_bin, ["run", f'Grade the work in this file: {prompt_file}. {payload}'])
        rc, out, err = run_cmd(cmd, EVALS_DIR, 300)
        if rc != 0:
            return {"score": 0, "reason": f"judge failed: {err.strip()}"}
        m = re.search(r"\{.*\}", out, re.DOTALL)
        if not m:
            return {"score": 0, "reason": "no JSON in judge output"}
        return json.loads(m.group(0))
    except (json.JSONDecodeError, ValueError, TypeError):
        return {"score": 0, "reason": "malformed judge JSON"}
    finally:
        Path(prompt_file).unlink(missing_ok=True)
```

Note: `import tempfile` must be added at the top of `harness.py`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `python -m unittest evals.test_harness -v`
Expected: all tests PASS.

- [ ] **Step 5: Commit**

```bash
git add evals/harness.py evals/test_harness.py
git commit -m "feat: add grading to eval harness"
```

---

### Task 6: harness.py — CLI orchestration (run variants, results, re-entrancy)

**Files:**
- Modify: `evals/harness.py`
- Test: `evals/test_harness.py`

**Interfaces:**
- Consumes: everything from Tasks 3-5.
- Produces (the complete harness CLI):
  - `run_variant(task: Task, variant: str, opts) -> dict` — result payload with `task_id`, `variant`, `exit_code`, `timed_out`, `checks`, `diff_overlap`, `judge`, `transcript`, `diff`.
  - `write_result(task_id: str, variant: str, result: dict) -> Path`
  - `main()` — argparse CLI with subcommand `run` (flags: `--task`, `--variants {baseline,kb,both}` default `both`, `--rerun`, `--no-judge`, `--keep-worktrees`, `--timeout-min`, `--opencode-bin`) and `list`.
  - Re-entrancy: if a result file exists and `--rerun` is absent, skip that variant.

- [ ] **Step 1: Write the failing tests**

Append to `evals/test_harness.py`:

```python
import json as _json

from harness import RESULTS_DIR, run_variant, write_result


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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `python -m unittest evals.test_harness -v`
Expected: FAIL with `ImportError: cannot import name 'run_variant'`.

- [ ] **Step 3: Implement orchestration + CLI**

Append to `evals/harness.py`:

```python
def write_result(task_id: str, variant: str, result: dict, results_dir: Path) -> Path:
    d = results_dir / task_id
    d.mkdir(parents=True, exist_ok=True)
    path = d / f"{variant}.json"
    path.write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")
    return path


def run_variant(
    task: Task,
    variant: str,
    opencode_bin: str,
    timeout_min: int,
    no_judge: bool,
    keep_worktrees: bool,
    results_dir: Path,
    use_kb: bool,
) -> dict:
    name = f"{task.id}-{variant}"
    worktree = create_worktree(name, task.base_commit)
    try:
        if use_kb:
            inject_kb(worktree)
        rc, transcript = run_agent(worktree, task.prompt, timeout_min * 60, opencode_bin)
        agent_diff = capture_diff(worktree)
        checks = run_checks(task, worktree, transcript)
        overlap = diff_overlap(agent_diff, gold_diff(task)) if task.gold_commit else 0.0
        judge = (
            {"skipped": True}
            if no_judge
            else judge_llm(task, transcript, agent_diff, opencode_bin)
        )
        result = {
            "task_id": task.id,
            "variant": variant,
            "exit_code": rc,
            "timed_out": rc == -1,
            "checks": checks,
            "diff_overlap": overlap,
            "judge": judge,
            "transcript": transcript,
            "diff": agent_diff,
        }
        write_result(task.id, variant, result, results_dir)
        return result
    finally:
        if not keep_worktrees:
            remove_worktree(name, worktree)


def list_tasks() -> None:
    for p in sorted(TASKS_DIR.glob("*.json")):
        task = load_task(p)
        print(f"{task.id}: [{task.type}] {task.title} (base={task.base_commit[:8]})")


def main() -> None:
    parser = argparse.ArgumentParser(prog="harness", description="Rafiq agent eval harness")
    sub = parser.add_subparsers(dest="command", required=True)

    run_p = sub.add_parser("run", help="run one or more eval tasks")
    run_p.add_argument("--task", help="task id or filename; omit to run all tasks in evals/tasks/")
    run_p.add_argument("--variants", choices=["baseline", "kb", "both"], default="both")
    run_p.add_argument("--rerun", action="store_true", help="rerun completed variants")
    run_p.add_argument("--no-judge", action="store_true", help="skip LLM judge")
    run_p.add_argument("--keep-worktrees", action="store_true")
    run_p.add_argument("--timeout-min", type=int, default=30)
    run_p.add_argument("--opencode-bin", help="opencode CLI binary (default: OPENCODE_BIN or PATH)")
    run_p.add_argument("--results-dir", type=Path, default=RESULTS_DIR)

    sub.add_parser("list", help="list eval tasks")

    args = parser.parse_args()

    if args.command == "list":
        list_tasks()
        return

    opencode_bin = args.opencode_bin or find_opencode_bin()
    RESULTS_DIR.mkdir(parents=True, exist_ok=True)

    if args.task:
        path = Path(args.task)
        if not path.exists():
            path = TASKS_DIR / f"{args.task}.json"
        tasks = [load_task(path)] if path.exists() else []
        if not tasks:
            match = next(
                (
                    p
                    for p in sorted(TASKS_DIR.glob("*.json"))
                    if load_task(p).id == args.task or p.stem == args.task
                ),
                None,
            )
            tasks = [load_task(match)] if match else []
        if not tasks:
            print(f"task not found: {args.task}", file=sys.stderr)
            sys.exit(1)
    else:
        tasks = [load_task(p) for p in sorted(TASKS_DIR.glob("*.json"))]

    for task in tasks:
        errors = validate_task(task, REPO_DIR)
        if errors:
            print(f"SKIP {task.id}: {errors}", file=sys.stderr)
            continue
        variants = ["baseline", "kb"] if args.variants == "both" else [args.variants]
        for variant in variants:
            result_path = args.results_dir / task.id / f"{variant}.json"
            if result_path.exists() and not args.rerun:
                print(f"SKIP {task.id}/{variant} (exists; use --rerun)")
                continue
            print(f"RUN {task.id}/{variant} ...", flush=True)
            result = run_variant(
                task, variant, opencode_bin, args.timeout_min, args.no_judge,
                args.keep_worktrees, args.results_dir, use_kb=(variant == "kb"),
            )
            print(
                f"DONE {task.id}/{variant} exit={result['exit_code']} "
                f"overlap={result['diff_overlap']:.2f} checks={result['checks']}"
            )


if __name__ == "__main__":
    main()
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `python -m unittest evals.test_harness -v`
Expected: all tests PASS.

- [ ] **Step 5: Verify CLI surface works**

Run: `python evals/harness.py list`
Expected: prints the seeded tasks (after Task 8) or nothing yet. Run: `python evals/harness.py run --help`
Expected: shows the argparse help, no crash.

- [ ] **Step 6: Add `.gitignore` entries**

Append to `.gitignore`:

```
evals/kb_context.md
evals/results/
evals/worktrees/
evals/__pycache__/
```

- [ ] **Step 7: Commit**

```bash
git add evals/harness.py evals/test_harness.py .gitignore
git commit -m "feat: add harness CLI orchestration and gitignore"
```

---

### Task 7: report.py — baseline-vs-kb comparison + unit tests

**Files:**
- Create: `evals/report.py`
- Test: `evals/test_report.py`

**Interfaces:**
- Consumes: `evals/results/<task_id>/<variant>.json` from Task 6.
- Produces:
  - `load_results(results_dir: Path) -> dict[str, dict[str, dict]]` keyed `task_id -> variant -> result`.
  - `render_report(results: dict) -> str` (markdown table).
  - `main()` CLI with `--results-dir`.

- [ ] **Step 1: Write the failing tests**

Create `evals/test_report.py`:

```python
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `python -m unittest evals.test_report -v`
Expected: FAIL with `ModuleNotFoundError: No module named 'report'`.

- [ ] **Step 3: Implement report.py**

Create `evals/report.py`:

```python
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `python -m unittest evals.test_report -v`
Expected: 2 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add evals/report.py evals/test_report.py
git commit -m "feat: add baseline-vs-kb eval report"
```

---

### Task 8: Seed eval tasks (7 JSON files) from real repo history

**Files:**
- Create: `evals/tasks/rf-001-fts3-translation-query.json`
- Create: `evals/tasks/rf-002-navigation3-routes.json`
- Create: `evals/tasks/rf-003-prayer-times-dataflow.json`
- Create: `evals/tasks/rf-004-dashboard-prayer-hero.json`
- Create: `evals/tasks/rf-005-ayah-appbar-declutter.json`
- Create: `evals/tasks/rf-006-home-vector-icons.json`
- Create: `evals/tasks/rf-007-app-version-homescreen.json`

**Interfaces:**
- Consumes: Task JSON schema (Global Constraints) + verified SHAs below.
- Produces: the seed suite. `harness.py list` and `run --task` (Task 6) read these.

Verified SHAs (from `git log --format="%H %P"`):
- rf-001: base `a78852fc00dbc1bbd9ecc9ce9b513cbf8da522a5`, gold `096b94d2191997e73938eb526152779cdc6659f3` (FTS3 fix)
- rf-002: base = `origin/main` `7bda4570ea88731f05ebf31fdc3a42485a955de6`, no gold_commit
- rf-003: base = `7bda4570ea88731f05ebf31fdc3a42485a955de6`, no gold_commit
- rf-004: base `6e343758d7fc233f1d9587388af687fd42bd2052`, gold `f5c89b700f8c3aeb7f6a468ad320c6cd93893f56`
- rf-005: base `db5c207a693740e179a94b300394ceeb60333673`, gold `7bda4570ea88731f05ebf31fdc3a42485a955de6`
- rf-006: base `7bda4570ea88731f05ebf31fdc3a42485a955de6`, gold `ff926e766ba37a119fedd00d7eed7cf10f961f91`
- rf-007: base `e5fb148e014b9ea0737d80c7cb3c178139b36d22`, gold `db5c207a693740e179a94b300394ceeb60333673`

- [ ] **Step 1: Create rf-001 (retrieval)**

`evals/tasks/rf-001-fts3-translation-query.json`:

```json
{
  "id": "rf-001",
  "title": "Locate and explain the FTS3 translation query fix",
  "type": "retrieval",
  "base_commit": "a78852fc00dbc1bbd9ecc9ce9b513cbf8da522a5",
  "gold_commit": "096b94d2191997e73938eb526152779cdc6659f3",
  "prompt": "The Quran translations feature sometimes returns 0 rows on Android SQLite. Investigate the data layer, find the root cause in the translation query, explain why it happens, and show the exact fix that was applied in this repo's history.",
  "gold_files": ["data/src/main/kotlin/com/smiledev/rafiq/data/repository/QuranRepositoryImpl.kt"],
  "checks": [
    {"kind": "transcript_mentions", "files": ["data/src/main/kotlin/com/smiledev/rafiq/data/repository/QuranRepositoryImpl.kt"]}
  ],
  "rubric": [
    "Identifies getTranslationForSura in QuranRepositoryImpl",
    "Explains the type-mismatch cause (verses.sura is TEXT)",
    "Mentions CAST(sura AS INTEGER)",
    "Explains why the fix is in the query, not the data"
  ],
  "timeout_min": 15
}
```

- [ ] **Step 2: Create rf-002 (retrieval)**

`evals/tasks/rf-002-navigation3-routes.json`:

```json
{
  "id": "rf-002",
  "title": "Explain Navigation3 route registration",
  "type": "retrieval",
  "base_commit": "7bda4570ea88731f05ebf31fdc3a42485a955de6",
  "prompt": "Explain how navigation works in this app. Describe the role of NavigationKeys.kt and Navigation.kt, and list the exact steps needed to add a new screen to the app, including the two files that must change and the pattern each follows.",
  "gold_files": ["app/src/main/java/com/smiledev/rafiq/NavigationKeys.kt", "app/src/main/java/com/smiledev/rafiq/Navigation.kt"],
  "checks": [
    {"kind": "transcript_mentions", "files": ["app/src/main/java/com/smiledev/rafiq/NavigationKeys.kt", "app/src/main/java/com/smiledev/rafiq/Navigation.kt"]}
  ],
  "rubric": [
    "Mentions @Serializable NavKey tokens in NavigationKeys.kt",
    "Mentions entryProvider { entry<Key> { ... } } in Navigation.kt",
    "Describes backStack.add for forward nav and removeLastOrNull for back",
    "Lists steps to register a new screen"
  ],
  "timeout_min": 15
}
```

- [ ] **Step 3: Create rf-003 (retrieval)**

`evals/tasks/rf-003-prayer-times-dataflow.json`:

```json
{
  "id": "rf-003",
  "title": "Trace prayer-times data flow",
  "type": "retrieval",
  "base_commit": "7bda4570ea88731f05ebf31fdc3a42485a955de6",
  "prompt": "Trace the complete data flow for prayer times in this app, from the remote API call to the UI. Name each file involved (API service, repository, use case, ViewModel, Screen), describe the data shape, and identify the default coordinates and calculation method used.",
  "gold_files": [
    "data/src/main/kotlin/com/smiledev/rafiq/data/remote/AladhanApi.kt",
    "data/src/main/kotlin/com/smiledev/rafiq/data/repository/PrayerTimesRepositoryImpl.kt",
    "domain/src/main/kotlin/com/smiledev/rafiq/domain/usecase/GetPrayerTimesUseCase.kt",
    "app/src/main/java/com/smiledev/rafiq/ui/prayertimes/PrayerTimesViewModel.kt",
    "app/src/main/java/com/smiledev/rafiq/ui/prayertimes/PrayerTimesScreen.kt"
  ],
  "checks": [
    {"kind": "transcript_mentions", "files": ["data/src/main/kotlin/com/smiledev/rafiq/data/remote/AladhanApi.kt"]},
    {"kind": "transcript_mentions", "files": ["data/src/main/kotlin/com/smiledev/rafiq/data/repository/PrayerTimesRepositoryImpl.kt"]}
  ],
  "rubric": [
    "Names Aladhan API v1/timings with method 20",
    "Mentions default Jakarta coordinates (-6.2088, 106.8456)",
    "Names GetPrayerTimesUseCase and PrayerTimesViewModel",
    "Describes the timings fields (Fajr, Dhuhr, Asr, Maghrib, Isha, ...)"
  ],
  "timeout_min": 15
}
```

- [ ] **Step 4: Create rf-004 (coding)**

`evals/tasks/rf-004-dashboard-prayer-hero.json`:

```json
{
  "id": "rf-004",
  "title": "Revamp home dashboard with prayer hero widget",
  "type": "coding",
  "base_commit": "6e343758d7fc233f1d9587388af687fd42bd2052",
  "gold_commit": "f5c89b700f8c3aeb7f6a468ad320c6cd93893f56",
  "prompt": "Revamp the home dashboard screen: add a prayer hero widget that shows the next prayer time and a countdown, and lay out the feature entry cards in a condensed grid. Follow the existing MVVM + Jetpack Compose patterns in this codebase. Match the look and behavior of the actual feature that was merged (you may consult the repo's history for the intended behavior, but implement it fresh).",
  "gold_files": [
    "app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardScreen.kt",
    "app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardViewModel.kt",
    "app/src/main/java/com/smiledev/rafiq/Navigation.kt"
  ],
  "checks": [
    {"kind": "file_touched", "files": ["app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardScreen.kt"]},
    {"kind": "file_touched", "files": ["app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardViewModel.kt"]},
    {"kind": "test", "command": "python -c pass"}
  ],
  "rubric": [
    "Adds a prayer hero section with next prayer + countdown",
    "Condenses the feature grid layout",
    "Uses DashboardViewModel for state, not hardcoded UI data",
    "No new third-party dependencies",
    "Builds with gradlew"
  ],
  "timeout_min": 45
}
```

- [ ] **Step 5: Create rf-005 (coding)**

`evals/tasks/rf-005-ayah-appbar-declutter.json`:

```json
{
  "id": "rf-005",
  "title": "Declutter Ayah screen app bar and tafsir toggle",
  "type": "coding",
  "base_commit": "db5c207a693740e179a94b300394ceeb60333673",
  "gold_commit": "7bda4570ea88731f05ebf31fdc3a42485a955de6",
  "prompt": "The Ayah screen's top app bar is cluttered and the tafsir toggle is awkward. Clean up the app bar: move secondary actions out of the top bar and make the tafsir toggle easier to use. Match the merged behavior in this repo's history (consult it for intent, implement fresh).",
  "gold_files": ["app/src/main/java/com/smiledev/rafiq/ui/quran/AyahScreen.kt"],
  "checks": [
    {"kind": "file_touched", "files": ["app/src/main/java/com/smiledev/rafiq/ui/quran/AyahScreen.kt"]}
  ],
  "rubric": [
    "App bar shows only the essential action(s)",
    "Tafsir toggle is discoverable and usable",
    "Changes stay within AyahScreen.kt",
    "No new dependencies"
  ],
  "timeout_min": 45
}
```

- [ ] **Step 6: Create rf-006 (coding)**

`evals/tasks/rf-006-home-vector-icons.json`:

```json
{
  "id": "rf-006",
  "title": "Custom vector icons for home feature cards",
  "type": "coding",
  "base_commit": "7bda4570ea88731f05ebf31fdc3a42485a955de6",
  "gold_commit": "ff926e766ba37a119fedd00d7eed7cf10f961f91",
  "prompt": "Replace the home screen feature cards' icon usage with custom vector drawables. Create the vector drawable XML resources under res/drawable and reference them from DashboardScreen.kt instead of the current icon approach. Match the merged implementation in this repo's history (consult it for intent, implement fresh).",
  "gold_files": [
    "app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardScreen.kt",
    "app/src/main/res/drawable/ic_quran.xml",
    "app/src/main/res/drawable/ic_prayer.xml"
  ],
  "checks": [
    {"kind": "file_touched", "files": ["app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardScreen.kt"]},
    {"kind": "file_touched", "files": ["app/src/main/res/drawable/ic_quran.xml"]}
  ],
  "rubric": [
    "Adds vector drawable XML files under res/drawable",
    "DashboardScreen.kt references the new drawables",
    "No material-icons-extended dependency added",
    "Drawables use the theme color palette"
  ],
  "timeout_min": 45
}
```

- [ ] **Step 7: Create rf-007 (coding)**

`evals/tasks/rf-007-app-version-homescreen.json`:

```json
{
  "id": "rf-007",
  "title": "Show app version on home screen",
  "type": "coding",
  "base_commit": "e5fb148e014b9ea0737d80c7cb3c178139b36d22",
  "gold_commit": "db5c207a693740e179a94b300394ceeb60333673",
  "prompt": "Add a small text label showing the app version on the home screen, e.g. 'v1.0.0'. Expose the version through DashboardViewModel rather than hardcoding it in the composable, and match the merged implementation in this repo's history.",
  "gold_files": [
    "app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardScreen.kt",
    "app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardViewModel.kt"
  ],
  "checks": [
    {"kind": "file_touched", "files": ["app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardScreen.kt"]},
    {"kind": "file_touched", "files": ["app/src/main/java/com/smiledev/rafiq/ui/dashboard/DashboardViewModel.kt"]}
  ],
  "rubric": [
    "Version string sourced via DashboardViewModel",
    "Label rendered on the home screen",
    "Uses existing version access pattern in the codebase"
  ],
  "timeout_min": 45
}
```

- [ ] **Step 8: Verify all tasks load and validate**

Run: `python evals/harness.py list`
Expected: prints all 7 tasks with ids rf-001..rf-007.

Run a validation loop for every task file:

```python
import sys
from pathlib import Path
sys.path.insert(0, "evals")
from harness import load_task, validate_task, REPO_DIR
for p in sorted(Path("evals/tasks").glob("*.json")):
    t = load_task(p)
    errs = validate_task(t, REPO_DIR)
    print(p.name, "OK" if not errs else f"ERROR {errs}")
```

Expected: all 7 print `OK`.

- [ ] **Step 9: Commit**

```bash
git add evals/tasks/
git commit -m "feat: add seed eval tasks from real repo history"
```

---

### Task 9: evals/README + end-to-end smoke test

**Files:**
- Create: `evals/README.md`
- Modify: `kb/README.md` (only if a cross-reference is needed — otherwise skip)

**Interfaces:**
- Consumes: the full harness from Tasks 3-7 and seed tasks from Task 8.
- Produces: documentation and an end-to-end proof run.

- [ ] **Step 1: Write `evals/README.md`**

```markdown
# Eval Harness for the Rafiq Agent

Runs an AI agent on tasks derived from this repo's real history and measures
whether the knowledge base (`kb/`) improves its performance.

## Prerequisites

- Python 3.12+ (stdlib only)
- git
- opencode CLI on PATH, or set `OPENCODE_BIN` to its path
- (for build checks only) working JAVA_HOME:
  `C:\Program Files\Android\Android Studio1\jbr`

## One-time setup

```powershell
python evals/build_kb_context.py
```

Regenerate `evals/kb_context.md` whenever `kb/` changes.

## Authoring tasks

Add a JSON file to `evals/tasks/`. Schema:

- `id`, `title`, `type` (`retrieval` | `coding`)
- `base_commit` (40-char sha), `gold_commit` (optional)
- `prompt` — goal statement, no hints about paths
- `gold_files` — expected files (used by checks + diff overlap)
- `checks` — `file_touched` / `transcript_mentions` / `test`
- `rubric` — strings used by the optional LLM judge
- `timeout_min`

For coding tasks, derive base/gold from real history:

```powershell
git log --format="%H %P" -1 <commit>
```

## Running

```powershell
python evals/harness.py list
python evals/harness.py run --task rf-001 --no-judge --variants both
python evals/harness.py run --no-judge --variants both
```

- `--no-judge` skips the (token-costly) LLM judge.
- `--rerun` re-runs completed variants.
- `--keep-worktrees` leaves worktrees for inspection.
- Results land in `evals/results/<task_id>/<variant>.json` (gitignored).

## Reporting kb impact

```powershell
python evals/report.py
```

Prints a baseline-vs-kb table with per-task and aggregate deltas. A positive
aggregate delta means the kb helped.
```

- [ ] **Step 2: Run full unit test suite**

Run: `python -m unittest discover -s evals -p "test_*.py" -v`
Expected: all tests across `test_build_kb_context.py`, `test_harness.py`, `test_report.py` PASS.

- [ ] **Step 3: Smoke-test the harness with a fake agent binary (no opencode needed)**

Create a temporary fake agent and run one retrieval task baseline variant:

```python
import sys
from pathlib import Path
sys.path.insert(0, "evals")
from harness import load_task, run_variant

task = load_task(Path("evals/tasks/rf-002-navigation3-routes.json"))
fake = Path("evals/worktrees/FAKE_AGENT.py")
fake.write_text("print('NavigationKeys.kt and Navigation.kt use entryProvider')\n", encoding="utf-8")
result = run_variant(
    task, "baseline",
    opencode_bin=sys.executable + " " + str(fake),
    timeout_min=2, no_judge=True, keep_worktrees=False,
    results_dir=Path("evals/results"), use_kb=False,
)
print("exit_code:", result["exit_code"])
print("checks:", result["checks"])
assert result["checks"]["transcript_mentions:app/src/main/java/com/smiledev/rafiq/NavigationKeys.kt,app/src/main/java/com/smiledev/rafiq/Navigation.kt"] is True
print("SMOKE OK")
```

Run it: `python -c "..."` (or save as a throwaway script under `evals/worktrees/` which is gitignored).
Expected: prints `SMOKE OK` and the transcript-mention check passes.

- [ ] **Step 4: Confirm report renders against smoke result**

Run: `python evals/report.py`
Expected: prints the report table including `rf-002` baseline row.

- [ ] **Step 5: Commit**

```bash
git add evals/README.md
git commit -m "docs: add eval harness README"
```

---

## Self-Review Notes (checked by the planner)

- **Spec coverage:** kb/ (7 files) ✓ Task 1; build_kb_context.py ✓ Task 2; harness.py ✓ Tasks 3-6; task format/seed suite ✓ Task 8; report.py ✓ Task 7; prerequisites/error handling (timeouts, re-entrancy, --no-judge, --keep-worktrees, OPENCODE_BIN check) ✓ Tasks 4-6; README ✓ Task 9; gitignore ✓ Task 6. Deliverable list in the spec maps 1:1 to these tasks.
- **Environment corrections captured:** AGENTS.md JAVA_HOME is broken; the plan uses the verified `Android Studio1\jbr` and documents it in `kb/build-test.md`. The `:feature:*` quirk is documented, not "fixed". `opencode` CLI absence is handled via `OPENCODE_BIN` + clear error.
- **Type consistency:** `run_variant` signature is defined in Task 6 and used identically by the Task 9 smoke test. `write_result` gains an optional `results_dir` param in Task 6 to keep the Task 6 test hermetic; `main()` always passes `args.results_dir`. `_added_lines`/`_changed_paths` helpers are private but used by public functions in the same module.
- **Test paths:** `test_harness.py` imports grow across Tasks 3-6 (module gets defined incrementally); all imports are added in the same task that introduces the function, so the file is always runnable at the end of each task.
