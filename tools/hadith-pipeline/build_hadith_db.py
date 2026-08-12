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
