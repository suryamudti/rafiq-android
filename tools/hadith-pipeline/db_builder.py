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


def build_hadiths(collection, hj_doc, id_rows, matched, id_offset=0):
    positions = _book_positions(hj_doc)
    # matched is aligned to id_rows, NOT to hadiths:
    #   matched[k] = HJ hadith index matched by id_row k (or None).
    # Invert it so we can route a terjemah to the hadith that won.
    hj_to_idrow = {}
    for k, m in enumerate(matched):
        if m is not None and m not in hj_to_idrow:
            hj_to_idrow[m] = k
    per_book = {}
    rows = []
    nid = id_offset
    for i, h in enumerate(hj_doc['hadiths']):
        text_en = (h.get('english') or {}).get('text', '') or ''
        if not text_en.strip():
            continue  # hadith-json v1.2.0 has 2 blank-EN hadiths: drop them
        book_id = positions.get(h['chapterId'])
        per_book[book_id] = per_book.get(book_id, 0) + 1
        in_book = per_book[book_id]
        narrator_en = (h.get('english') or {}).get('narrator', '') or ''
        text_id = ''
        k = hj_to_idrow.get(i)
        if k is not None and k < len(id_rows):
            text_id = id_rows[k][3]
        nid += 1
        rows.append((nid, book_id, in_book, '', narrator_en,
                     h.get('arabic', ''), text_en, text_id))
    return rows
