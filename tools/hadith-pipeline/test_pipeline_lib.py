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


if __name__ == '__main__':
    unittest.main()
