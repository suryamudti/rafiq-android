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

- hadith-json v1.2.0 contains 2 hadiths with blank English text (Bukhari id 6857,
  Muslim id 13569); the pipeline drops them, so counts are Bukhari 7,276 / Muslim 7,458.
- Indonesian coverage is best-effort: ~84% of Indonesian-source rows matched a hadith by
  normalized Arabic matn (~10,322 of 14,734 hadiths carry Indonesian text). Unmatched rows
  store `text_id = ''` and the app falls back to the English translation.
- `narrator_ar` is always empty (no separate Arabic narrator in hadith-json).
- `in_book_number` is recomputed per book (hadith-json's `idInBook` is a global counter).

## Validation gates (fail loudly)

- Book counts: Bukhari 97, Muslim 57.
- Hadith counts: Bukhari 7,276, Muslim 7,458 (sum 14,734).
- No blank `text_ar` / `text_en`.
- `text_id` coverage is reported, never silently dropped.
- No duplicate book ids, no orphaned hadith rows.

## Determinism

The pipeline writes the same DB bytes for identical pinned inputs (no timestamps, fixed
row order). Regeneration from the pinned sources is reproducible; the committed DB is the
source of truth for the app.