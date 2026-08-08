# Design: Meaningful Islamic Calendar — Monthly Hijri Grid

Date: 2026-08-08
Status: Approved
Branch: `feat/islamic-calendar-improvements`

## Problem

The current Islamic Calendar screen is a flat list: 12 Hijri-month chips + a list of 18 events + a "Today's Events" card. It is not really a calendar, and `getTodayEvents()` compares *Gregorian* month/day against *Hijri* month/day — so "Today's Events" effectively never matches. There is no Hijri↔Gregorian conversion anywhere in the app; the Aladhan API is used only for prayer timings.

## Goal

Replace the list with a real **monthly calendar grid**:

- Gregorian month grid where each day cell shows the Gregorian day + the corresponding Hijri day.
- A prominent **Hijri header** giving month context (e.g., "We are in Ramadan 1447").
- Islamic events marked as dots on their correct grid days.
- Tap a day to open a bottom sheet with that day's Hijri date and any events.
- Keep the "Today's Events" card at the top.

Fully offline and deterministic via an in-app **tabular Islamic calendar** conversion algorithm.

## Design Decisions (from brainstorming)

| Question | Decision |
|---|---|
| Direction | Real monthly calendar grid |
| Date mapping source | Offline arithmetic conversion algorithm (tabular Islamic calendar) |
| Grid navigation | Gregorian months navigated with arrows + Today button; Hijri header for context |
| Day tap | Modal bottom sheet with Hijri date + events on that day |
| Events on grid | Expand dataset beyond the current 18 events |
| Existing UI | Grid replaces the month-chip row and event list; keep Today's Events card |
| Algorithm placement | Pure Kotlin in `:domain` (no Android deps, unit-testable) |

## Architecture

### 1. Hijri Conversion (`:domain`)

New pure Kotlin, no Android dependencies:

- `domain/src/main/kotlin/com/smiledev/rafiq/domain/model/HijriDate.kt`
  - `data class HijriDate(val year: Int, val month: Int, val day: Int)`
- `domain/src/main/kotlin/com/smiledev/rafiq/domain/util/HijriDateConverter.kt`
  - `fun gregorianToHijri(year: Int, month: Int, day: Int): HijriDate`
  - `fun hijriToGregorian(year: Int, month: Int, day: Int): GregorianDate`
  - Tabular Islamic calendar algorithm (Kuwaiti/tabular 30-year cycle): months alternate 30/29 days; the 11 leap years in the 30-year cycle give the 12th month 30 days.

Known test anchors:
- 1 Muharram 1447 AH = 26 June 2025
- 1 Jan 2025 = 1 Rajab 1446
- Round-trip property: `hijriToGregorian(gregorianToHijri(d))` returns a date within ±1 day.

### 1b. Today's Events fix (`:data`)

`getTodayEvents()` currently compares Gregorian month/day against Hijri month/day. With the converter available, rewrite it to:
1. Get today's Gregorian date.
2. Convert to Hijri via `gregorianToHijri`.
3. Filter events where `hijriMonth == todayHijri.month && hijriDay == todayHijri.day`.
4. Keep the Muharram 1 fallback removed — an empty result just shows an empty card state (no misleading fallback).

### 2. Event Data (`:data`)

Extend `app/src/main/assets/quran-data/islamic_events.json` beyond the current 18 events:

- Add Ayyam al-Bid (13, 14, 15 of each Hijri month — recommended fasting days).
- Add Arafah, more observances across all months.
- Keep existing fields: `hijri_month`, `hijri_day`, `title_en`, `title_id`, `description_en`, `description_id`, `event_type`.
- `IslamicCalendarRepositoryImpl` already parses this file and caches events; no structural change needed, only more entries.

### 3. ViewModel (`:app`)

`CalendarViewModel` gains:
- Displayed Gregorian month/year state (defaults to today).
- `selectedDate` state for the day bottom sheet.
- A computed grid: `List<CalendarDay>` for the displayed month, where each `CalendarDay` has Gregorian day, Hijri date, and `List<IslamicEvent>`.
- Month navigation: `nextMonth()`, `previousMonth()`, `goToToday()`.
- Event→day mapping: convert each event's Hijri date to Gregorian via `hijriToGregorian`, place a marker if it lands within the displayed Gregorian month.
- Keep existing `todayEvents`, `localeCode`, `getMonthName`, `selectMonth`-equivalent functionality where relevant (month chip row is removed).

### 4. UI (`:app`)

`IslamicCalendarScreen` layout (top to bottom):

1. **Today's Events card** — unchanged.
2. **Hijri header** — banner showing the Hijri month/year of the displayed month's first day. If the Gregorian month spans two Hijri months, show both as a range (e.g., "Ramadan – Shawwal 1447 AH").
3. **Gregorian month navigator** — `‹` `Month Year` `›` + a "Today" button.
4. **Weekday header row** — Sun through Sat (localized).
5. **Grid** — up to 6 rows × 7 columns. Each cell:
   - Gregorian day number (primary, bold).
   - Hijri day number (small, secondary).
   - Event dot(s) if any events fall on that day.
   - Today highlighted; selected day outlined.
6. **Day bottom sheet** — full Gregorian date, Hijri date (e.g., "1 Muharram 1447 AH"), and events on that day with EN/ID descriptions.

Empty leading/trailing cells in the grid are blank (no content).

### 5. Tests

- `:domain` — `HijriDateConverterTest`: anchors (1 Muharram 1447 = 26 June 2025; 1 Jan 2025 = 1 Rajab 1446), month-length/leap-year cases, round-trip sanity.
- `:app` — `CalendarViewModelTest`: grid generation for a known month, month navigation, event-dot placement for a known event date, goToToday.
- `:data` — repository test for the expanded JSON (parse all entries, no crashes, events parse with valid fields).

### 6. Localization

Add strings to `app/src/main/res/values/strings.xml` and `app/src/main/res/values-id/strings.xml`:

- `hijri_header_prefix` — e.g., "We are in %1$s" / "Kita berada di %1$s"
- Month names already exist in the repository (`islamicMonthNames`, `islamicMonthNamesId`).
- "Today" button label.

## Out of Scope

- Network-based Umm al-Qura dates (offline tabular algorithm chosen).
- Prayer-log integration from the day sheet (could be a follow-up).
- Dark-mode-specific grid styling beyond the existing theme.
- Extending event model with Quran verse references (future work).

## Open Risks

- Tabular algorithm may differ ±1 day from Saudi Umm al-Qura/moon-sighting in some months. Accepted; deterministic and offline.
- Grid rendering must handle months spanning two Hijri months: the header shows a range (e.g., "Ramadan – Shawwal"), and each day cell independently shows its own Hijri date.
