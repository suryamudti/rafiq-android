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
