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
