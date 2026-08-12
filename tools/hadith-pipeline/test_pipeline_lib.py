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
        self.assertEqual([r[0] for r in unmatched], [1])   # rid of the unmatched row
        self.assertEqual(len(unmatched), 1)
        self.assertLess(unmatched[0][1], 0.55)


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


if __name__ == '__main__':
    unittest.main()
