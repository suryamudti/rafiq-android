# Assets & Data Loading

## Bundled SQLite databases

Assets live under `assets/` (e.g. `quran-data/`, `translations/`).
Translation DB names contain `/` (e.g. `translations/en.sahih.db`).

- `DatabaseCopier.kt` (in `:data`, package `com.smiledev.rafiq.core`) flattens
  `/` to `_` before calling `getDatabasePath()` because Android rejects path
  separators in DB names.
- `copyDatabaseIfNeeded()` copies to `filesDir/databases/<flatName>`.
- `copyAndVerifyTranslationDb()` verifies the file exists and is non-empty.

## Quran DB (`quran-uthmani.db`)

- All columns are TEXT.
- Bismillah is a nullable `String?` stored as actual Arabic text.

## Arabic font

Load via `FontFamily(Font(R.font.me_quran))`. NEVER use `fontResource()`.

## Translation query gotcha (historical bug, FTS3)

In `QuranRepositoryImpl.getTranslationForSura()`, the verses table stores `sura`
as TEXT. A query `SELECT ayah, text FROM verses WHERE sura = ?` with a string
arg returns 0 rows on Android SQLite due to type affinity. The fix was to cast:
`WHERE CAST(sura AS INTEGER) = ?`.
