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
