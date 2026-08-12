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
