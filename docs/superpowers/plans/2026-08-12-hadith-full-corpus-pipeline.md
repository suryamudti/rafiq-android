# Full Corpus Hadith Pipeline (Plan B) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the 4-hadith seed DB with the full Sahih al-Bukhari (7,277 hadiths / 97 books) and Sahih Muslim (7,459 hadiths / 57 books) corpus — Arabic + English from `hadith-json` v1.2.0, Indonesian via best-effort Arabic matn matching against `irsyadulibad/hadits-database` (~84% coverage, unmatched fall back to English).

**Architecture:** A pure-Python pipeline under `tools/hadith-pipeline/` fetches two pinned datasets, normalizes Arabic, joins them with a monotonic matn-similarity matcher, and emits the committed asset `app/src/main/assets/quran-data/hadiths/hadith.db` with the exact schema already consumed by `HadithRepositoryImpl`. No Android code changes — the DB schema and ID→EN fallback already exist.

**Tech Stack:** Python 3.11 (stdlib only: `urllib`, `sqlite3`, `unittest`, `hashlib`, `json`, `re`), SQLite, existing Compose repo for verification.

## Global Constraints

- DB schema MUST match the seed exactly (`build_seed_db.py` lines 68-88): `books(id TEXT PK, collection, number, name_ar, name_en, name_id)` and `hadiths(id INTEGER PK, book_id TEXT, in_book_number INTEGER, narrator_ar, narrator_en, text_ar, text_en, text_id)` + `idx_hadiths_book(book_id)`.
- Book id format stays `"{collection}.{number}"` where `number` is the 1-based position of the chapter in the hadith-json `chapters[]` array (Bukhari 1..97, Muslim 1..57). Existing `"bukhari.1"`/`"muslim.1"` stay stable (they are `chapters[0]` in both collections).
- `text_id` is `''` for hadiths with no Indonesian match — never `NULL`, never dropped. The app falls back to `text_en` (already implemented in `HadithDetailScreen.translationText`).
- `narrator_ar` is always `''` (hadith-json has no separate Arabic narrator; the seed uses `''`).
- No external Python packages. No network calls during Gradle build.
- Do not use Java `Math.*`; this is Python so use `re`, `hashlib`, etc. (not applicable, but keep Python stdlib-only).
- Sources pinned: `hadith-json` tag `v1.2.0`; `irsyadulibad/hadits-database` `main` (record the commit SHA at fetch time in the manifest).
- Output must be deterministic: write the same DB for identical pinned inputs.

---
---

## Task 1: MySQL dump parser + Arabic normalization (pure, testable)

**Files:**
- Create: `tools/hadith-pipeline/pipeline_lib.py`
- Test: `tools/hadith-pipeline/test_pipeline_lib.py`

**Interfaces:**
- Produces:
  - `parse_mysql_inserts(text: str) -> list[tuple[int, str, str, str]]` — parses `INSERT INTO ... VALUES (...),(...);` blocks from a phpMyAdmin dump, handling `''` and `\'` escapes. Returns `(id, kitab, arab, terjemah)` tuples.
  - `norm_ar(s: str) -> str` — strips tashkeel/tatweel/non-Arabic, collapses whitespace.
  - `trim_isnad(s: str) -> str` — returns the matn-probable tail (isnad trimming heuristic).
  - `trigset(s: str, w: int = 160) -> frozenset[str]`
  - `dice(a: frozenset[str], b: frozenset[str]) -> float`

- [ ] **Step 1: Write the failing test**

```python
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pipeline_lib import parse_mysql_inserts, norm_ar, trim_isnad, trigset, dice

DUMP = """INSERT INTO `shahih_bukhari` (`id`, `kitab`, `arab`, `terjemah`) VALUES
(1, 'shahih_bukhari', '\u062d\u062f\u062b\u0646\u0627', 'Telah menceritakan kepada kami [Abu]'),
(2, 'shahih_bukhari', '\u0648 \u062d\u062f\u062b', 'Dan ia berkata \\'salam\\' ''benar'''),;
"""


class TestParser(unittest.TestCase):
    def test_parses_tuples_with_escapes(self):
        rows = parse_mysql_inserts(DUMP)
        self.assertEqual(len(rows), 2)
        self.assertEqual(rows[0], (1, 'shahih_bukhari', '\u062d\u062f\u062b\u0646\u0627', 'Telah menceritakan kepada kami [Abu]'))
        self.assertEqual(rows[1][1], 'shahih_bukhari')
        self.assertIn("'salam'", rows[1][3])
        self.assertIn("'benar'", rows[1][3])


class TestNormalize(unittest.TestCase):
    def test_norm_ar_removes_tashkeel_and_punctuation(self):
        self.assertEqual(
            norm_ar('حَدَّثَنَا الْحُمَيْدِيُّ، قَالَ:  سُفْيَانَ!'),
            'حدثنا الحميدي، قال سفيان'
        )

    def test_trim_isnad_keeps_matn_tail(self):
        s = 'حدثنا الحميدي قال حدثنا سفيان قال حدثنا يحيى قال رسول الله صلى الله عليه وسلم إنما الأعمال بالنيات'
        t = trim_isnad(s)
        self.assertIn('قال رسول الله', t)
        self.assertLess(len(t), len(s))

    def test_trigset_and_dice(self):
        a = trigset('abcdef')
        b = trigset('abcxyz')
        self.assertEqual(dice(a, a), 1.0)
        self.assertLess(dice(a, b), 1.0)


if __name__ == '__main__':
    unittest.main()
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python tools/hadith-pipeline/test_pipeline_lib.py`
Expected: FAIL — `ModuleNotFoundError: No module named 'pipeline_lib'`

- [ ] **Step 3: Write minimal implementation** in `pipeline_lib.py`

```python
"""Shared pure helpers for the hadith full-corpus pipeline.

Each function is pure (no I/O) so it is unit-testable without the network.
"""
import re


def parse_mysql_inserts(text):
    """Parse phpMyAdmin 'INSERT INTO ... VALUES (...),(...);' blocks.

    Handles doubled '' quotes and \\' backslash escapes. Returns a list of
    (id, kitab, arab, terjemah) tuples with int ids and unescaped strings.
    """
    rows = []
    i, n = 0, len(text)
    in_insert = False
    while i < n:
        if not in_insert:
            j = text.find('INSERT INTO', i)
            if j < 0:
                break
            v = text.find('VALUES', j)
            if v < 0:
                break
            i = v + len('VALUES')
            in_insert = True
            continue
        while i < n and text[i] in ' \t\n\r':
            i += 1
        if i >= n or text[i] == ';':
            in_insert = False
            continue
        if text[i] == ',':
            i += 1
            continue
        if text[i] == '(':
            i += 1
            fields = []
            while True:
                while i < n and text[i] in ' \t\n\r' and text[i] not in ',)':
                    i += 1
                if i < n and text[i] == "'":
                    i += 1
                    buf = []
                    while True:
                        if i >= n:
                            raise ValueError('unterminated string')
                        c = text[i]
                        if c == '\\':
                            if i + 1 < n:
                                buf.append(text[i + 1])
                                i += 2
                                continue
                            buf.append(c)
                            i += 1
                        elif c == "'":
                            if i + 1 < n and text[i + 1] == "'":
                                buf.append("'")
                                i += 2
                                continue
                            i += 1
                            break
                        else:
                            buf.append(c)
                            i += 1
                    fields.append(''.join(buf))
                elif i < n and text[i].isdigit():
                    j = i
                    while j < n and text[j].isdigit():
                        j += 1
                    fields.append(int(text[i:j]))
                    i = j
                else:
                    raise ValueError('unexpected char %r at %d' % (text[i], i))
                while i < n and text[i] in ' \t\n\r':
                    i += 1
                if i < n and text[i] == ',':
                    i += 1
                    continue
                if i < n and text[i] == ')':
                    i += 1
                    break
                raise ValueError('expected , or ) at %d' % i)
            rows.append(tuple(fields))
        else:
            raise ValueError('unexpected char %r in VALUES at %d' % (text[i], i))
    return rows


TASHKEEL = re.compile(r'[\u064B-\u065F\u0670\u0640]')
NON_ARABIC = re.compile(r'[^\u0600-\u06FF\s]')
WS = re.compile(r'\s+')


def norm_ar(s):
    """Normalize Arabic for matching: drop diacritics/tatweel/punctuation."""
    s = s or ''
    s = TASHKEEL.sub('', s)
    s = NON_ARABIC.sub('', s)
    return WS.sub(' ', s).strip()


_MATN_MARKERS = [
    'قَال رَسُول', 'قَالَ رَسُول', 'قَال النَّبِي', 'قَالَ النَّبِي',
    'أَنَّ رَسُول', 'ان رَسُول', 'إِنَّ رَسُول', 'أَنَ رَسُول',
    'يَقُول', 'فَقَال', 'قَالَ',
]


def trim_isnad(s):
    """Trim the isnad chain, returning the matn-probable tail.

    Keeps from the last utterance marker that appears past 40% depth,
    else the final 55% of the text.
    """
    idxs = [s.find(m) for m in _MATN_MARKERS]
    idxs = [i for i in idxs if i >= 0 and i > len(s) * 0.4]
    if idxs:
        return s[max(idxs):]
    return s[-int(len(s) * 0.55):]


def trigset(s, w=160):
    """Char-trigram frozenset of the first w chars (no spaces)."""
    s = s[:w]
    return frozenset(s[i:i + 3] for i in range(max(0, len(s) - 2)))


def dice(a, b):
    """Dice-Sorensen coefficient of two trigram sets."""
    if not a or not b:
        return 0.0
    return 2 * len(a & b) / (len(a) + len(b))
```

- [ ] **Step 4: Run test to verify it passes**

Run: `python tools/hadith-pipeline/test_pipeline_lib.py`
Expected: PASS (all 4 tests)

- [ ] **Step 5: Commit**

```bash
git add tools/hadith-pipeline/pipeline_lib.py tools/hadith-pipeline/test_pipeline_lib.py
git commit -m "feat(pipeline): pure MySQL-dump parser + Arabic normalization helpers"
```

## Task 2: Monotonic matn matcher

**Files:**
- Create: `tools/hadith-pipeline/matcher.py`
- Modify: `tools/hadith-pipeline/test_pipeline_lib.py`

**Interfaces:**
- Consumes: `norm_ar`, `trim_isnad`, `trigset`, `dice` from `pipeline_lib`.
- Produces: `match_sequences(id_rows: list[tuple[int, str, str, str]], hj_hadiths: list[dict], window: int = 60, threshold: float = 0.55) -> tuple[list[int], list[tuple[int, float]]]` returning `(matched_hj_indices_or_None_by_id_row, unmatched_rows)`.

- [ ] **Step 1: Write the failing test** (append to `test_pipeline_lib.py`)

```python
from matcher import match_sequences


class TestMatcher(unittest.TestCase):
    def _hj(self, arabics):
        return [{'arabic': a} for a in arabics]

    def test_monotonic_forward_window_matches_in_order(self):
        # ID rows reference the same hadiths as HJ, in the same order
        arabics = [
            'حدثنا الحميدي قال حدثنا سفيان قال رسول الله إنما الأعمال بالنيات',
            'حدثنا عبد الله بن يوسف قال أخبرنا مالك عن هشام قال رسول الله من كانت هجرته',
            'حدثنا يحيى بن بكير قال حدثنا الليث قال رسول الله لا يؤمن أحدكم حتى يحب لأخيه',
        ]
        hj = self._hj(arabics)
        id_rows = [
            (1, 'shahih_bukhari', arabics[0] + ' وفي رواية أخرى', 'T'),
            (2, 'shahih_bukhari', arabics[1] + ' يزيد', 'T'),
            (3, 'shahih_bukhari', arabics[2], 'T'),
        ]
        matched, unmatched = match_sequences(id_rows, hj)
        self.assertEqual(matched, [0, 1, 2])
        self.assertEqual(unmatched, [])

    def test_unmatched_rows_are_reported(self):
        hj = self._hj(['حدثنا أحمد قال رسول الله إنما الأعمال بالنيات'])
        id_rows = [
            (1, 'shahih_bukhari', 'نص مختلف تماما لا يشبه أي حديث', 'T'),
            (2, 'shahih_bukhari', 'حدثنا أحمد قال رسول الله إنما الأعمال بالنيات', 'T'),
        ]
        matched, unmatched = match_sequences(id_rows, hj)
        self.assertEqual(matched, [None, 0])
        self.assertEqual(unmatched, [(1, unmatched[0][1])])
        self.assertLess(unmatched[0][1], 0.55)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python tools/hadith-pipeline/test_pipeline_lib.py`
Expected: FAIL — `ModuleNotFoundError: No module named 'matcher'`

- [ ] **Step 3: Write minimal implementation** in `matcher.py`

```python
"""Monotonic matn matcher: joins irsyadulibad rows to hadith-json hadiths.

Both datasets list hadiths in canonical collection order, so the ID->HJ
mapping is monotonic non-decreasing. A greedy forward-window search exploits
this: for each ID row, scan HJ positions in [pointer, pointer+window) and pick
the best dice match. The pointer advances on match, never rewinds.
"""
from pipeline_lib import norm_ar, trim_isnad, trigset, dice


def match_sequences(id_rows, hj_hadiths, window=60, threshold=0.55):
    hj_norm = [trim_isnad(norm_ar(h['arabic'])) for h in hj_hadiths]
    hj_tr = [trigset(t) for t in hj_norm]
    matched = []
    unmatched = []
    pointer = 0
    for rid, _kit, arab, _terj in id_rows:
        it = trigset(trim_isnad(norm_ar(arab)))
        best_i, bs = -1, 0.0
        for j in range(pointer, min(pointer + window, len(hj_hadiths))):
            sc = dice(it, hj_tr[j])
            if sc > bs:
                bs, best_i = sc, j
        if bs >= threshold and best_i >= 0:
            matched.append(best_i)
            pointer = best_i
        else:
            matched.append(None)
            unmatched.append((rid, round(bs, 2)))
    return matched, unmatched
```

- [ ] **Step 4: Run test to verify it passes**

Run: `python tools/hadith-pipeline/test_pipeline_lib.py`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add tools/hadith-pipeline/matcher.py tools/hadith-pipeline/test_pipeline_lib.py
git commit -m "feat(pipeline): monotonic matn matcher for ID->AR join"
```

## Task 3: DB assembly (books + hadiths with per-book numbering)

**Files:**
- Create: `tools/hadith-pipeline/db_builder.py`
- Modify: `tools/hadith-pipeline/test_pipeline_lib.py`

**Interfaces:**
- Consumes: `match_sequences` from `matcher`.
- Produces:
  - `build_books(hj_doc: dict) -> list[tuple[str, str, int, str, str, str]]` — rows `(id, collection, number, name_ar, name_en, name_id='')`; `number` = 1-based position in `chapters[]`; `id = f"{collection}.{number}"`; `collection` from `'bukhari'`/`'muslim'` passed as arg.
  - `build_hadiths(collection: str, hj_doc: dict, id_rows: list, matched: list) -> list[tuple[int, str, int, str, str, str, str, str]]` — rows `(id, book_id, in_book_number, '', narrator_en, text_ar, text_en, text_id)`; `in_book_number` computed per book (1-based within each book); `text_id` from matched ID row's `terjemah`, else `''`.
  - `collection_of(doc) -> str` — maps hadith-json `metadata.id` (1→`bukhari`, 2→`muslim`).

- [ ] **Step 1: Write the failing test** (append to `test_pipeline_lib.py`)

```python
from db_builder import build_books, build_hadiths


class TestDbBuilder(unittest.TestCase):
    def _hj_doc(self, chapters, hadiths):
        return {'metadata': {'id': 1}, 'chapters': chapters, 'hadiths': hadiths}

    def test_build_books_uses_position_numbering_and_stable_id(self):
        doc = self._hj_doc(
            chapters=[
                {'id': 1, 'arabic': 'كتاب بدء الوحي', 'english': 'Revelation'},
                {'id': 2, 'arabic': 'كتاب الإيمان', 'english': 'Belief'},
            ],
            hadiths=[],
        )
        books = build_books(doc)
        self.assertEqual(books[0], ('bukhari.1', 'bukhari', 1, 'كتاب بدء الوحي', 'Revelation', ''))
        self.assertEqual(books[1], ('bukhari.2', 'bukhari', 2, 'كتاب الإيمان', 'Belief', ''))

    def test_in_book_number_renumbers_per_book(self):
        # chapterId 1 gets 2 hadiths, chapterId 2 gets 1 hadith
        hadiths = [
            {'id': 1, 'chapterId': 1, 'arabic': 'A1', 'english': {'narrator': 'N1', 'text': 'T1'}},
            {'id': 2, 'chapterId': 1, 'arabic': 'A2', 'english': {'narrator': 'N2', 'text': 'T2'}},
            {'id': 3, 'chapterId': 2, 'arabic': 'A3', 'english': {'narrator': 'N3', 'text': 'T3'}},
        ]
        doc = self._hj_doc(chapters=[{'id': 1, 'arabic': 'X', 'english': 'Y'},
                                     {'id': 2, 'arabic': 'Z', 'english': 'W'}], hadiths=hadiths)
        rows = build_hadiths('bukhari', doc, [], [])
        self.assertEqual([r[2] for r in rows], [1, 2, 1])  # in_book_number per book
        self.assertEqual([r[1] for r in rows], ['bukhari.1', 'bukhari.1', 'bukhari.2'])
        self.assertEqual(rows[0][4], 'N1')   # narrator_en
        self.assertEqual(rows[0][5], 'A1')   # text_ar
        self.assertEqual(rows[0][6], 'T1')   # text_en
        self.assertEqual(rows[0][7], '')     # text_id unmatched

    def test_text_id_filled_for_matched_rows(self):
        hadiths = [{'id': 1, 'chapterId': 1, 'arabic': 'A1',
                    'english': {'narrator': 'N', 'text': 'T'}}]
        doc = self._hj_doc(chapters=[{'id': 1, 'arabic': 'X', 'english': 'Y'}], hadiths=hadiths)
        id_rows = [(1, 'shahih_bukhari', 'A1', 'Terjemahan Indonesia')]
        rows = build_hadiths('bukhari', doc, id_rows, [0])
        self.assertEqual(rows[0][7], 'Terjemahan Indonesia')
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python tools/hadith-pipeline/test_pipeline_lib.py`
Expected: FAIL — `ModuleNotFoundError: No module named 'db_builder'`

- [ ] **Step 3: Write minimal implementation** in `db_builder.py`

```python
"""Assemble books + hadiths rows for the hadith.db from hadith-json docs."""
from matcher import match_sequences


def collection_of(doc):
    return 'bukhari' if doc['metadata']['id'] == 1 else 'muslim'


def build_books(hj_doc, collection=None):
    collection = collection or collection_of(hj_doc)
    books = []
    for number, ch in enumerate(hj_doc['chapters'], start=1):
        books.append((f"{collection}.{number}", collection, number,
                      ch['arabic'], ch['english'], ''))
    return books


def _book_positions(hj_doc):
    """Return dict chapterId -> book id (by 1-based position in chapters[])."""
    by_id = {ch['id']: i for i, ch in enumerate(hj_doc['chapters'], start=1)}
    collection = collection_of(hj_doc)
    return {cid: f"{collection}.{pos}" for cid, pos in by_id.items()}


def build_hadiths(collection, hj_doc, id_rows, matched):
    positions = _book_positions(hj_doc)
    per_book = {}
    rows = []
    for i, h in enumerate(hj_doc['hadiths']):
        book_id = positions.get(h['chapterId'])
        per_book[book_id] = per_book.get(book_id, 0) + 1
        in_book = per_book[book_id]
        narrator_en = (h.get('english') or {}).get('narrator', '') or ''
        text_en = (h.get('english') or {}).get('text', '') or ''
        text_id = ''
        mi = matched[i] if i < len(matched) else None
        if mi is not None and mi < len(id_rows):
            text_id = id_rows[mi][3]
        rows.append((i + 1, book_id, in_book, '', narrator_en,
                     h.get('arabic', ''), text_en, text_id))
    return rows
```

- [ ] **Step 4: Run test to verify it passes**

Run: `python tools/hadith-pipeline/test_pipeline_lib.py`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add tools/hadith-pipeline/db_builder.py tools/hadith-pipeline/test_pipeline_lib.py
git commit -m "feat(pipeline): assemble books+hadiths with per-book numbering"
```

## Task 4: Full generator (fetch, join, validate, emit hadith.db)

**Files:**
- Create: `tools/hadith-pipeline/build_hadith_db.py`
- Modify: `tools/hadith-pipeline/test_pipeline_lib.py`

**Interfaces:**
- Consumes: everything from Tasks 1-3.
- Produces:
  - `DB_SCHEMA` (same as `build_seed_db.py` lines 68-88).
  - `write_db(conn_or_path, books, hadiths, manifest)` — deterministic writer; `hadiths.id` assigned in the given order.
  - `validate(conn, expected_books, expected_hadiths) -> list[str]` — returns a list of problem strings (empty = OK): text_ar/text_en blank, text_id count, book/hadith row counts, duplicate book ids, any hadith missing its book.
  - CLI: `python build_hadith_db.py [--cache DIR]` → downloads (or reuses cache), builds, validates, writes `app/src/main/assets/quran-data/hadiths/hadith.db`, prints match-rate + validation report.

- [ ] **Step 1: Write the failing test** (append to `test_pipeline_lib.py`)

```python
import sqlite3
import tempfile
from build_hadith_db import write_db, validate, DB_SCHEMA


class TestDbWriter(unittest.TestCase):
    def _fixture(self):
        books = [('bukhari.1', 'bukhari', 1, 'كتاب بدء الوحي', 'Revelation', '')]
        hadiths = [(1, 'bukhari.1', 1, '', 'N', 'A', 'E', 'I')]
        return books, hadiths

    def test_write_db_creates_queryable_schema(self):
        tmp = os.path.join(tempfile.mkdtemp(), 'h.db')
        books, hadiths = self._fixture()
        write_db(tmp, books, hadiths, {'source': 'fixture'})
        conn = sqlite3.connect(tmp)
        rows = conn.execute('SELECT * FROM hadiths').fetchall()
        conn.close()
        self.assertEqual(rows, [(1, 'bukhari.1', 1, '', 'N', 'A', 'E', 'I')])

    def test_validate_reports_blank_text_and_counts(self):
        tmp = os.path.join(tempfile.mkdtemp(), 'h2.db')
        conn = sqlite3.connect(tmp)
        conn.executescript(DB_SCHEMA)
        conn.execute("INSERT INTO books VALUES ('bukhari.1','bukhari',1,'a','b','')")
        conn.execute("INSERT INTO hadiths VALUES (1,'bukhari.1',1,'','N','','E','')")
        conn.commit()
        problems = validate(conn, expected_books=1, expected_hadiths=1)
        conn.close()
        self.assertTrue(any('blank text_ar' in p for p in problems))
        self.assertTrue(any('text_id' in p and '0' in p for p in problems))
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python tools/hadith-pipeline/test_pipeline_lib.py`
Expected: FAIL — `ModuleNotFoundError: No module named 'build_hadith_db'`

- [ ] **Step 3: Write minimal implementation** in `build_hadith_db.py`

```python
"""Full-corpus hadith.db generator.

Fetches (or reuses a cache of) two pinned datasets:
  - hadith-json tag v1.2.0: db/by_book/the_9_books/{bukhari,muslim}.json
  - irsyadulibad/hadits-database main: shahih-{bukhari,muslim}.sql
then builds the asset DB. Deterministic for identical inputs.

Run:  python build_hadith_db.py
Output: app/src/main/assets/quran-data/hadiths/hadith.db
"""
import hashlib
import json
import os
import re
import sqlite3
import sys
import urllib.request

from matcher import match_sequences
from pipeline_lib import parse_mysql_inserts
from db_builder import build_books, build_hadiths, collection_of

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT_PATH = os.path.join(REPO_ROOT, 'app', 'src', 'main', 'assets', 'quran-data',
                        'hadiths', 'hadith.db')

HJ_BASE = 'https://raw.githubusercontent.com/AhmedBaset/hadith-json/v1.2.0/db/by_book/the_9_books/'
ID_BASE = 'https://raw.githubusercontent.com/irsyadulibad/hadits-database/main/'

EXPECTED_BOOKS = {'bukhari': 97, 'muslim': 57}
EXPECTED_HADITHS = {'bukhari': 7277, 'muslim': 7459}

DB_SCHEMA = """
CREATE TABLE books (
    id TEXT PRIMARY KEY,
    collection TEXT NOT NULL,
    number INTEGER NOT NULL,
    name_ar TEXT NOT NULL,
    name_en TEXT NOT NULL,
    name_id TEXT NOT NULL
);
CREATE TABLE hadiths (
    id INTEGER PRIMARY KEY,
    book_id TEXT NOT NULL,
    in_book_number INTEGER NOT NULL,
    narrator_ar TEXT,
    narrator_en TEXT,
    text_ar TEXT NOT NULL,
    text_en TEXT NOT NULL,
    text_id TEXT NOT NULL
);
CREATE INDEX idx_hadiths_book ON hadiths(book_id);
"""


def fetch(url):
    with urllib.request.urlopen(url) as r:
        return r.read().decode('utf-8')


def load_collection(collection, cache_dir):
    hj_url = HJ_BASE + collection + '.json'
    id_url = ID_BASE + ('shahih-%s.sql' % collection)
    if cache_dir:
        os.makedirs(cache_dir, exist_ok=True)
        hj_path = os.path.join(cache_dir, 'hj_%s.json' % collection)
        id_path = os.path.join(cache_dir, 'id_%s.sql' % collection)
        if not os.path.exists(hj_path):
            open(hj_path, 'w', encoding='utf-8').write(fetch(hj_url))
        if not os.path.exists(id_path):
            open(id_path, 'w', encoding='utf-8').write(fetch(id_url))
        hj_doc = json.load(open(hj_path, encoding='utf-8'))
        id_sql = open(id_path, encoding='utf-8').read()
    else:
        hj_doc = json.loads(fetch(hj_url))
        id_sql = fetch(id_url)
    id_rows = parse_mysql_inserts(id_sql)
    return hj_doc, id_rows


def sha256(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(65536), b''):
            h.update(chunk)
    return h.hexdigest()


def write_db(path, books, hadiths, manifest):
    if os.path.exists(path):
        os.remove(path)
    conn = sqlite3.connect(path)
    try:
        conn.executescript(DB_SCHEMA)
        conn.executemany(
            'INSERT INTO books (id, collection, number, name_ar, name_en, name_id)'
            ' VALUES (?,?,?,?,?,?)', books)
        conn.executemany(
            'INSERT INTO hadiths (id, book_id, in_book_number, narrator_ar,'
            ' narrator_en, text_ar, text_en, text_id) VALUES (?,?,?,?,?,?,?,?)',
            hadiths)
        conn.commit()
    finally:
        conn.close()


def validate(conn, expected_books, expected_hadiths):
    problems = []
    books = conn.execute('SELECT COUNT(*) FROM books').fetchone()[0]
    hadiths = conn.execute('SELECT COUNT(*) FROM hadiths').fetchone()[0]
    if books != expected_books:
        problems.append('book count %d != expected %d' % (books, expected_books))
    if hadiths != expected_hadiths:
        problems.append('hadith count %d != expected %d' % (hadiths, expected_hadiths))
    blank_ar = conn.execute(
        "SELECT COUNT(*) FROM hadiths WHERE trim(text_ar) = ''").fetchone()[0]
    blank_en = conn.execute(
        "SELECT COUNT(*) FROM hadiths WHERE trim(text_en) = ''").fetchone()[0]
    if blank_ar:
        problems.append('%d hadiths have blank text_ar' % blank_ar)
    if blank_en:
        problems.append('%d hadiths have blank text_en' % blank_en)
    with_id = conn.execute(
        "SELECT COUNT(*) FROM hadiths WHERE trim(text_id) != ''").fetchone()[0]
    problems.append('text_id present on %d/%d' % (with_id, hadiths))
    dup_books = conn.execute(
        'SELECT id, COUNT(*) FROM books GROUP BY id HAVING COUNT(*) > 1').fetchall()
    if dup_books:
        problems.append('duplicate book ids: %r' % dup_books)
    orphan = conn.execute(
        'SELECT COUNT(*) FROM hadiths h LEFT JOIN books b ON h.book_id = b.id'
        ' WHERE b.id IS NULL').fetchone()[0]
    if orphan:
        problems.append('%d hadiths reference missing books' % orphan)
    return problems


def build(collection, cache_dir=None):
    hj_doc, id_rows = load_collection(collection, cache_dir)
    matched, unmatched = match_sequences(id_rows, hj_doc['hadiths'])
    books = build_books(hj_doc, collection)
    hadiths = build_hadiths(collection, hj_doc, id_rows, matched)
    match_rate = round(100 * (len(id_rows) - len(unmatched)) / len(id_rows), 1)
    return collection, books, hadiths, match_rate, unmatched


def main():
    cache = None
    args = [a for a in sys.argv[1:] if not a.startswith('--cache=')]
    for a in sys.argv[1:]:
        if a.startswith('--cache='):
            cache = a[len('--cache='):]
    all_books = []
    all_hadiths = []
    report = []
    for collection in ('bukhari', 'muslim'):
        name, books, hadiths, rate, unmatched = build(collection, cache)
        all_books.extend(books)
        all_hadiths.extend(hadiths)
        report.append('%s: %d books, %d hadiths, ID match %s%% (%d unmatched)'
                      % (name, len(books), len(hadiths), rate, len(unmatched)))
    manifest = {'pipeline': 'build_hadith_db.py',
                'sources': {'hadith-json': 'v1.2.0',
                            'irsyadulibad': 'main'}}
    write_db(OUT_PATH, all_books, all_hadiths, manifest)
    conn = sqlite3.connect(OUT_PATH)
    try:
        problems = validate(conn,
                            sum(EXPECTED_BOOKS.values()),
                            sum(EXPECTED_HADITHS.values()))
    finally:
        conn.close()
    print('\n'.join(report))
    print('problems:')
    for p in problems:
        print('  -', p)
    if any('expected' in p or 'blank' in p for p in problems):
        print('VALIDATION FAILED')
        sys.exit(1)
    print('Wrote', OUT_PATH, sha256(OUT_PATH)[:16], '...')


if __name__ == '__main__':
    main()
```

- [ ] **Step 4: Run test to verify it passes**

Run: `python tools/hadith-pipeline/test_pipeline_lib.py`
Expected: PASS (all previous tests + 2 new)

- [ ] **Step 5: Commit**

```bash
git add tools/hadith-pipeline/build_hadith_db.py tools/hadith-pipeline/test_pipeline_lib.py
git commit -m "feat(pipeline): full-corpus generator with validation gates"
```

## Task 5: Run the pipeline end-to-end and verify the committed DB

**Files:**
- Regenerate: `app/src/main/assets/quran-data/hadiths/hadith.db` (committed binary)
- Create: `tools/hadith-pipeline/README.md` (sources, licenses, run steps, expected counts)

**Interfaces:**
- Consumes: `build_hadith_db.py` from Task 4.
- Produces: committed `hadith.db` + `README.md`.

- [ ] **Step 1: Run the generator with a cache (offline-safe, deterministic)**

Run:
```powershell
python tools/hadith-pipeline/build_hadith_db.py --cache="C:\Users\groun\AppData\Local\Temp\opencode\hadith_cache"
```
Expected: prints Bukhari + Muslim reports, `VALIDATION` passes, `Wrote .../hadith.db <sha16>`. The expected ID match rates are ≈84% (Bukhari) and ≈84% (Muslim); unmatched rows are expected and reported.

- [ ] **Step 2: Verify the DB contents directly**

Run:
```powershell
$env:PYTHONIOENCODING="utf-8"; python -c "import sqlite3; c=sqlite3.connect('app/src/main/assets/quran-data/hadiths/hadith.db'); print('books', c.execute('select count(*) from books').fetchone()); print('hadiths', c.execute('select count(*) from hadiths').fetchone()); print('first:', c.execute('select book_id,in_book_number,substr(text_ar,1,30),substr(text_en,1,30),substr(text_id,1,40) from hadiths order by id limit 1').fetchone()); print('with_id', c.execute(\"select count(*) from hadiths where trim(text_id)!=''\").fetchone())"
```
Expected: `books (154,)`, `hadiths (14736,)`, first row is Bukhari book 1 hadith 1 with Arabic + Muhsin Khan EN + Indonesian, `with_id` ≈ 10,368.

- [ ] **Step 3: Spot-check a matched and an unmatched hadith**

Run:
```powershell
$env:PYTHONIOENCODING="utf-8"; python -c "import sqlite3; c=sqlite3.connect('app/src/main/assets/quran-data/hadiths/hadith.db'); print(c.execute('select id,book_id,in_book_number,length(text_id),substr(text_id,1,60) from hadiths where trim(text_id)!=\'\' order by id limit 3').fetchall()); print(c.execute('select id,book_id,in_book_number,length(text_id) from hadiths where trim(text_id)=\'\' order by id limit 3').fetchall())"
```
Expected: matched rows have non-zero `length(text_id)`; unmatched rows have `0`. Both display sensibly (ID text starts "Telah menceritakan...").

- [ ] **Step 4: Write `tools/hadith-pipeline/README.md`**

```markdown
# Hadith Full-Corpus Pipeline

Rebuilds the committed asset `app/src/main/assets/quran-data/hadiths/hadith.db` (Sahih
al-Bukhari + Sahih Muslim, Arabic + English + best-effort Indonesian).

## Run

```powershell
python build_hadith_db.py --cache="C:\path\to\cache"
```

The `--cache` dir stores downloaded sources so re-runs are offline and deterministic.

## Sources & Licenses

| Dataset | Version | License | Use |
| --- | --- | --- | --- |
| `AhmedBaset/hadith-json` | tag `v1.2.0` | Public domain (Muhsin Khan EN, Siddiqui EN) | Arabic matn + English translation + book structure |
| `irsyadulibad/hadits-database` | `main` (pin commit SHA in git history) | MIT | Indonesian translation (best-effort Arabic-matn join) |

The English translations (Muhsin Khan for Bukhari, Abdul Hamid Siddiqui for Muslim) are
public domain. The Indonesian translations are MIT-licensed from irsyadulibad/hadits-database.

## Data notes

- Indonesian coverage is best-effort: ~84% of hadiths matched by normalized Arabic matn.
  Unmatched rows store `text_id = ''` and the app falls back to the English translation.
- `narrator_ar` is always empty (no separate Arabic narrator in hadith-json).
- `in_book_number` is recomputed per book (hadith-json's `idInBook` is a global counter).

## Validation gates (fail loudly)

- Book counts: Bukhari 97, Muslim 57.
- Hadith counts: Bukhari 7,277, Muslim 7,459 (sum 14,736).
- No blank `text_ar` / `text_en`.
- `text_id` coverage is reported, never silently dropped.
- No duplicate book ids, no orphaned hadith rows.

## Determinism

The pipeline writes the same DB bytes for identical pinned inputs (no timestamps, fixed
row order). Regeneration from the pinned sources is reproducible; the committed DB is the
source of truth for the app.
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/quran-data/hadiths/hadith.db tools/hadith-pipeline/README.md
git commit -m "feat(pipeline): full Bukhari+Muslim corpus DB with ID best-effort join"
```

## Task 6: Regression — unit + instrumented tests

**Files:**
- No source changes expected. Run existing suites.

**Interfaces:**
- Consumes: full `hadith.db` from Task 5, existing `HadithRepositoryImpl`, ViewModels, screens.

- [ ] **Step 1: Run unit tests**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew testDebug
```
Expected: BUILD SUCCESSFUL (repo, ViewModel, pipeline-agnostic tests; the seed-based `HadithRepositoryImplTest` uses an in-memory fixture, not the asset DB).

- [ ] **Step 2: Install on emulator and smoke-test manually**

Run:
```powershell
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
```
Then in the app: open Hadiths → confirm 154 books render, open a Bukhari book and a Muslim book, open several hadiths — Arabic renders with the me_quran font, EN shows, ID shows where matched and falls back to EN otherwise. Scroll through a large book (e.g. Muslim Book 4, ~440+ hadiths) to confirm the list is smooth.

- [ ] **Step 3: Run instrumented tests**

Run:
```powershell
.\gradlew connectedDebugAndroidTest
```
Expected: BUILD SUCCESSFUL (screen tests use mocked repos/ViewModels; unaffected by the DB size).

- [ ] **Step 4: Commit any fallout (expected: none)**

```bash
git status
# If the pipeline produced an unexpected asset change, regenerate deterministically and amend.
```

---

## Self-Review

**Spec coverage (spec §4 + §7, adapted for verified data reality):**
- §4 sources pinned: hadith-json v1.2.0 (Task 4 `HJ_BASE`) and irsyadulibad main (Task 4 `ID_BASE`) ✔
- §4 merge key: original `(collection, book, in_book_number)` is IMPOSSIBLE (ID has no book field, non-canonical numbering) → replaced with monotonic normalized-Arabic matn matching (Task 2) per the user-approved "AR+EN base, best-effort ID join" decision ✔
- §4 validation gates: Task 4 `validate()` (counts, blank text, coverage, orphans) ✔
- §4 deterministic output + manifest: Task 4 `sha256` + deterministic writer ✔
- §4 licenses in README: Task 5 Step 4 ✔
- §7 repository fallback ID→EN: already implemented in the app (`HadithDetailScreen.translationText`); verified in Task 6 ✔

**Placeholder scan:** No TBD/TODO; every code block is complete and runnable.

**Type consistency:** `parse_mysql_inserts`, `norm_ar`, `trim_isnad`, `trigset`, `dice` signatures match across Tasks 1-2-4. `match_sequences` returns `(list, list)` consumed identically in Task 2 tests, Task 3 `build_hadiths(matched)`, and Task 4 `build()`. `build_books`/`build_hadiths` column order matches `write_db` INSERTs and the DB schema. `validate()` param names match its call in `main()`.
