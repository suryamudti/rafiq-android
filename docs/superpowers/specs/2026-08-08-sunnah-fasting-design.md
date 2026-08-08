# Design: Sunnah Fasting on the Islamic Calendar

Date: 2026-08-08
Status: Approved
Branch: `feat/islamic-calendar-improvements`

## Problem

The monthly Hijri grid (shipped in this branch) marks Islamic events but has no concept of *recommended fasting*. The well-known sunnah fasts — Monday/Thursday, the White Days, Ashura, Tasu'a, the six days of Shawwal, the first nine days of Dhul-Hijjah, and the general fast-much-of-Muharram/Sha'ban — are either absent or visually indistinguishable from ordinary observances. Weekly fasts (Monday/Thursday) cannot even be represented by the current model, which is keyed strictly by fixed Hijri month/day.

## Goal

Add recommended-fasting awareness to the calendar screen:

- Mark **every Monday and Thursday** in the grid as recommended fasting (weekly, by Gregorian weekday).
- Add fixed-date sunnah fasts to the dataset: **Tasu'a** (9 Muharram), **six days of Shawwal** (2–7), **first nine days of Dhul-Hijjah** (1–9, days 8–9 already exist as Tarwiyah/Arafah).
- Add **month-wide banners** for the general recommendations: "fast much of Muharram" and "fast much of Sha'ban".
- Give recommended-fasting days a **distinct visual marker** (purple dot) so fasting stands out from holidays (gold) and observances (teal).
- Reclassify existing fast-recommendation events (Ashura, Tarwiyah, Arafah, all 36 Ayyam al-Bid) to the new `fasting` type so all sunnah fasts share one visual language.

## Design Decisions (from brainstorming)

| Question | Decision |
|---|---|
| Which weekly fasts | **Monday + Thursday** (the established sunnah) |
| Weekly representation | Add an optional `weekday` field to the `IslamicEvent` model |
| Additional fasts | Tasu'a, six days of Shawwal, first nine days of Dhul-Hijjah |
| Month-wide recommendations | Banner under the month navigator (not dots on 30 days) |
| Fasting visual marker | New `event_type: "fasting"` with a distinct dot color; reclassify existing fast days |
| Dataset | Extend both `islamic_events.json` and the test fixture (byte-identical) |

## Architecture

### 1. Event Model (`:domain`)

`IslamicEvent` gains one nullable field:

```kotlin
data class IslamicEvent(
    val hijriMonth: Int,
    val hijriDay: Int,
    val titleEn: String,
    val titleId: String,
    val descriptionEn: String,
    val descriptionId: String,
    val eventType: String,
    val weekday: Int? = null   // 0=Sunday..6=Saturday; null for Hijri-date events
)
```

`weekday` is appended last with a default so existing positional constructor calls in tests (`IslamicEvent(1, 10, "Day of Ashura", ...)`) keep compiling unchanged.

Three event shapes now exist:

| Shape | hijriMonth | hijriDay | weekday | eventType |
|---|---|---|---|---|
| Fixed-date (existing) | 1..12 | 1..30 | null | holiday/observance/fasting |
| Weekly (new) | 0 | 0 | 0..6 | fasting |
| Month-wide (new) | 1..12 | 0 | null | recommendation |

### 2. Parsing (`:data`)

`IslamicCalendarRepositoryImpl.getEvents()` changes from `getInt` to `optInt` so weekly/month-wide entries can omit or zero their day fields:

- `hijriMonth = obj.optInt("hijri_month", 0)`
- `hijriDay = obj.optInt("hijri_day", 0)`
- `weekday = if (obj.has("weekday")) obj.getInt("weekday") else null`

### 3. Matching (`:data` + `:app`)

**Grid cell** (`CalendarViewModel.buildGrid`): an event lands on a day if any of:

- Weekly: `event.weekday == HijriDateConverter.weekdayOf(year, month, day)`
- Fixed-date: `event.hijriMonth == hijri.month && event.hijriDay == hijri.day`
- Month-wide: **excluded from cells** (banner-only; never a dot)

**Today's events** (`getTodayEvents`): match if any of:

- Weekly: `event.weekday == weekdayOf(today)` (via `HijriDateConverter`)
- Fixed-date: `event.hijriMonth == todayHijri.month && event.hijriDay == todayHijri.day`
- Month-wide: `event.hijriMonth == todayHijri.month && event.hijriDay == 0`

### 4. Dataset additions (byte-identical in both files)

`app/src/main/assets/quran-data/islamic_events.json` and `data/src/test/resources/expanded_events.json`:

- **Weekly**: `Monday Fasting` (weekday 1), `Thursday Fasting` (weekday 4) — `event_type: "fasting"`
- **Tasu'a** (9 Muharram) — `"fasting"`
- **Six days of Shawwal**: (10,2)..(10,7) — `"fasting"`
- **Dhul-Hijjah days 1–7**: (12,1)..(12,7) — `"fasting"` (days 8–9 Tarwiyah/Arafah already present and reclassified)
- **Month-wide recommendations**: Muharram (1,0), Sha'ban (8,0) — `event_type: "recommendation"`
- **Reclassify** to `"fasting"`: Ashura (1,10), Tarwiyah (12,8), Arafah (12,9), and all 36 Ayyam al-Bid entries (including Dhul-Hijjah 14–16).

Resulting count: 57 → 75 events. EN/ID titles and descriptions for every new entry.

### 5. ViewModel (`:app`)

`CalendarUiState` gains `monthlyRecommendations: List<IslamicEvent>`.

- Computed in `load()`/`refreshGrid()`: collect the distinct Hijri months present in the displayed grid (a Gregorian month spans parts of two Hijri months), then take `hijriDay == 0` events whose `hijriMonth` is in that set.
- `buildGrid` adds the weekday branch to the event filter.

### 6. UI (`:app`)

`IslamicCalendarScreen`:

- `DayCell`: dot color switches on `eventType` — `holiday`→gold, `fasting`→purple, else teal.
- New small **recommendation banner** Card below the month navigator, shown when `monthlyRecommendations` is non-empty, rendering each recommendation's localized title (e.g., "Recommended: fast much of Sha'ban" / "Dianjurkan: perbanyak puasa di bulan Sya'ban").
- Today's Events card and the day bottom sheet already render titles/descriptions via existing localization — no changes needed there.

### 7. Tests

- `:data` — `IslamicCalendarRepositoryImplTest`: update parse-all test (75 events; allow `hijriMonth` 0..12, `hijriDay` 0..30; allow `event_type` values `holiday|observance|fasting|recommendation`; validate `weekday` in 0..6 when present). New test: `getTodayEvents` returns Monday Fasting when today is a Monday.
- `:app` — `CalendarViewModelTest`: Monday Fasting lands on the Mondays of a displayed month (e.g., July 2025); month-wide recommendation appears in `monthlyRecommendations` for a month spanning Sha'ban; recommendation events produce no cell dots.

## Out of Scope

- Changing the tabular Hijri conversion algorithm.
- Prayer-log integration from the fasting marker.
- Notifications/reminders for fasting days (future work).
- Any change to the `docs/readme-update` branch or main checkout.

## Open Risks

- `weekday` 0-based mapping (0=Sunday) must stay consistent with `HijriDateConverter.weekdayOf`; verified against known anchors in tests.
- The `docs/readme-update` checkout contains the pre-grid calendar code; all work happens in the `feat/islamic-calendar-improvements` worktree only.
- Reclassifying existing events changes their dot color on the emulator — verify visually after build.
