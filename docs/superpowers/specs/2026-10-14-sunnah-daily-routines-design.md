# Sunnah Daily Routines — Design (2026-10-14)

## Goal
Add 6 daily-routine entries to Sunnah Guidance (17 → 23 items) to fix "hard to find / thin content" for everyday moments. Bilingual EN/ID, authentic sources only.

## §1 Architecture / data flow
- Append to `app/src/main/assets/quran-data/sunnah_guidance.json` (118KB). No schema change.
- All 6 use `category: DAILY_LIFESTYLE` (4 → 10, balances 10 PRAYER skew). No `SunnahCategory` enum change.
- Per-entry fields (matches `SunnahGuidanceRaw.toDomain` in `data/.../SunnahGuidanceRepositoryImpl.kt:63`): `id, titleEn/Id/Arabic, summaryEn/Id, descriptionEn/Id, surahReference?, hadithReference, dalilArabic + dalilTranslationEn/Id, virtuesEn/Id[3-4], steps[3-4 with arabic/transliteration/translation + refs], tipsEn/Id[3]`.
- Flow unchanged: asset → Gson → `cachedItems` → `GetSunnahGuidanceUseCase()` → `SunnahGuidanceViewModel.filterItems`. Search auto-covers new titles.
- Error path unchanged: malformed JSON → `AppError.Database`; IDs unique snake_case.

## §2 The 6 entries
1. `sunnah_masuk_keluar_rumah` — home in/out duas (Abu Dawood 5096 entering, 5095/Tirmidhi 3426 leaving).
2. `sunnah_toilet` — toilet adab: enter left + `khubuth/khabaith` (Bukhari 142), exit `Ghufraanak` (Abu Dawood 30), no facing qibla (Bukhari 394), left-hand istinja (Muslim 262).
3. `sunnah_berpakaian_cermin` — dressing dua (Abu Dawood 4023), start right (Bukhari 168), white best (Abu Dawood 4061), mirror dua (Ahmad 24392).
4. `sunnah_masuk_keluar_masjid` — mosque duas in/out (Abu Dawood 466, Muslim 713); complements existing `tahiyyatul_masjid`.
5. `sunnah_safar` — travel dua + return (Muslim 1342, QS 43:13-14), 2 rak'ahs before/after (Bukhari 3088).
6. `sunnah_wudhu_adab` — wudu extras/virtues (siwak Bukhari 887, post-wudu shahada Muslim 234, Bilal Bukhari 1149). Fiqh steps stay in `prayer_guidance.json wudhu`.
- Bar: every dalil + step carries HR./QS. ref.

## §3 UI / testing / rollout
- UI v1: no layout change. Existing chips + search pick up new items. Follow-up (separate): moment sub-filter.
- Tests: `SunnahGuidanceRepositoryImplTest` (count 23, unique IDs, `getSunnahById` per new id); `SunnahGuidanceViewModelTest.filterItems` ("toilet", "safar", "masjid" hits). Manual emulator: list + one detail RTL check.
- Rollout: single JSON append + tests. No migration, nav, or strings.xml change. Risk negligible (+~6KB, parsed once + cached).

## Out of scope
- No category/taxonomy redesign, no timeline UI, no favorites/progress/reminders.
- No changes to `prayer_guidance.json` fardhu content.
