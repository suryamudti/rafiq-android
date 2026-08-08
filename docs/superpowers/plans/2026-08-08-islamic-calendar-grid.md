# Islamic Calendar Monthly Grid Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the flat month-chip + event-list Islamic Calendar screen with a real offline monthly grid that shows the Gregorian month, per-day Hijri dates from a tabular conversion algorithm, Islamic event dots, a Hijri header banner, day-tap bottom sheet, and a fixed `getTodayEvents()`.

**Architecture:** A pure Kotlin `HijriDateConverter` in `:domain` (Dershowitz & Reingold RD-based tabular algorithm, `ISL_EPOCH = 227015`, `RD_TO_JDN = 1721424`) drives conversion and weekday math. The repository rewrites `getTodayEvents()` to convert today's Gregorian date to Hijri and match on Hijri month/day (fallback removed). `CalendarViewModel` gains displayed-year/month + a 42-cell grid with per-day events; `IslamicCalendarScreen` renders grid + header + bottom sheet. Tests: JUnit anchors/round-trip in `:domain`, Robolectric+mockk in `:data`, mockk+coroutines-test in `:app`.

**Tech Stack:** Kotlin 2.0.0, AGP 8.9.2, Hilt (KAPT, NOT KSP), Compose Material3, Robolectric 4.13, mockk 1.13.11, JUnit 4, Gradle multi-module (`:core`, `:domain`, `:data`, `:app`).

## Global Constraints

- Worktree root (all commands run here): `C:\Flutter\rafiq-app-android\.worktree\worktrees_name` on branch `feat/islamic-calendar-improvements`.
- Build prefix for EVERY gradle command (Android Studio jbr FAILS; use Adoptium, no trailing backslash):
  `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; cmd /c "gradlew.bat <task>"`
- Hilt uses KAPT, not KSP. The "Kapt currently doesn't support language version 2.0+" warning is harmless.
- Never add `material-icons-extended`. Icons core only (`DateRange`, `Face`, `Favorite`, `List`, `LocationOn`, `Notifications`, `Person`, `Place`, `PlayArrow`, `Refresh`, `ShoppingCart`, `Star`). The grid screen uses text arrows `\u2039`/`\u203A`, no icon deps.
- Never use Java `Math.*` — use `kotlin.math.*` (`kotlin.math.min`).
- Cross-module smart casts from nullable don't work. Guard with `!!`, `?:`, or a local `val` — never rely on `if (x != null)` smart-casting a property from another module.
- Room `room-runtime` stays `api` in `:data` (do not downgrade).
- Android minSdk 23 → NO `java.time` (API 26+). Use `java.util.Calendar` + `SimpleDateFormat`.
- Islamic month name lists come from the repository (`islamicMonthNames`, `islamicMonthNamesId`) — do not duplicate.
- Locale via `currentLocaleCode()` (`:core`); `Result<T, E : AppError>` has `onSuccess`/`onError`/`getOrNull` (`:core`).
- Data asset lives at `app/src/main/assets/quran-data/islamic_events.json`. The worktree top-level `assets/` is NOT the real asset dir.
- **Conversion algorithm (VALIDATED — locked constants):**
  - `islamicToFixed(y,m,d) = ISLAMIC_EPOCH + (y-1)*354 + floor((3 + 11*y)/30) + ceil(29.5*(m-1)) + (d-1)` with `ISLAMIC_EPOCH = 227015L`; `JDN = fixed + RD_TO_JDN` (`RD_TO_JDN = 1721424L`).
  - `fixedToIslamic(f)`: `year = floor((30*(f - 227015) + 10646) / 10631)`; `month = min(12, floor((f - islamicToFixed(year,1,1)) / 29.5) + 1)`; `day = f - (islamicToFixed(year,month,1) - 1)`.
  - **DO NOT use the `min(12, ceil((f - (start-1))/29.5))` variant** — it yields `day 0` on the 30th of 30-day months. Use the `floor(...)+1` form above (verified: exact round-trips, zero `day 0`/`month 0` over 1900–2100).
  - Weekday for Sunday-start grid: `weekdayOf = ((jdn + 1) % 7)` → 0=Sunday…6=Saturday (2025-06-26 → 4 = Thursday).
  - Grid leading blanks = `weekdayOf(year, month, 1)`; `daysInGregorianMonth` standard Gregorian; `isIslamicLeapYear = (11*y + 14) % 30 < 11`; month lengths: 1,3,5,7,9,11 → 30; 12 → 30 if leap else 29; else 29.
- **Anchors (use verbatim in tests):**
  - 1 Muharram 1447 = 2025-06-26; 1 Muharram 1446 = 2024-07-07.
  - 2025-01-01 = **2 Rajab 1446** (NOT 1 Rajab — tabular ±1 day vs Umm al-Qura; spec documents this).
  - 10 Muharram 1447 (Ashura) = 2025-07-05; 1 Ramadan 1447 = 2026-02-17; 1 Shawwal 1447 = 2026-03-19.
  - July 2025 grid: `weekdayOf(2025,7,1)=2` → July 1 at grid index 2 = `(1447,1,6)`; July 5 at index 6 = `(1447,1,10)`; July 25 at index 26 = `(1447,1,30)`; July 31 at index 32 = `(1447,2,6)`. Header spans Muharram–Safar (first cell `(1447,1,6)`, last cell `(1447,2,6)`).

---

### Task 1: `HijriDateConverter`, models, and TodayProvider in `:domain` (TDD)

**Files:**
- Create: `domain/src/main/kotlin/com/smiledev/rafiq/domain/model/HijriDate.kt`
- Create: `domain/src/main/kotlin/com/smiledev/rafiq/domain/model/GregorianDate.kt`
- Create: `domain/src/main/kotlin/com/smiledev/rafiq/domain/util/TodayProvider.kt`
- Create: `domain/src/main/kotlin/com/smiledev/rafiq/domain/util/HijriDateConverter.kt`
- Test: `domain/src/test/kotlin/com/smiledev/rafiq/domain/util/HijriDateConverterTest.kt`

**Interfaces:**
- Produces (consumed by later tasks):
  - `data class HijriDate(val year: Int, val month: Int, val day: Int)` in `com.smiledev.rafiq.domain.model`
  - `data class GregorianDate(val year: Int, val month: Int, val day: Int)` in `com.smiledev.rafiq.domain.model`
  - `fun interface TodayProvider { fun today(): GregorianDate }` in `com.smiledev.rafiq.domain.util`
  - `object SystemTodayProvider : TodayProvider` (uses `java.util.Calendar`) in `com.smiledev.rafiq.domain.util`
  - `object HijriDateConverter` in `com.smiledev.rafiq.domain.util` with:
    - `fun gregorianToHijri(year: Int, month: Int, day: Int): HijriDate`
    - `fun hijriToGregorian(year: Int, month: Int, day: Int): GregorianDate`
    - `fun isIslamicLeapYear(year: Int): Boolean`
    - `fun daysInHijriMonth(year: Int, month: Int): Int`
    - `fun daysInGregorianMonth(year: Int, month: Int): Int`
    - `fun weekdayOf(year: Int, month: Int, day: Int): Int` (0=Sunday…6=Saturday)

- [ ] **Step 1: Write the failing test**

`domain/src/test/kotlin/com/smiledev/rafiq/domain/util/HijriDateConverterTest.kt`:

```kotlin
package com.smiledev.rafiq.domain.util

import com.smiledev.rafiq.domain.model.GregorianDate
import com.smiledev.rafiq.domain.model.HijriDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HijriDateConverterTest {

    @Test
    fun `gregorianToHijri matches known anchors`() {
        assertEquals(HijriDate(1447, 1, 1), HijriDateConverter.gregorianToHijri(2025, 6, 26))
        assertEquals(HijriDate(1447, 1, 10), HijriDateConverter.gregorianToHijri(2025, 7, 5))
        assertEquals(HijriDate(1446, 7, 2), HijriDateConverter.gregorianToHijri(2025, 1, 1))
        assertEquals(HijriDate(1446, 1, 1), HijriDateConverter.gregorianToHijri(2024, 7, 7))
    }

    @Test
    fun `hijriToGregorian matches known anchors`() {
        assertEquals(GregorianDate(2025, 6, 26), HijriDateConverter.hijriToGregorian(1447, 1, 1))
        assertEquals(GregorianDate(2025, 7, 5), HijriDateConverter.hijriToGregorian(1447, 1, 10))
        assertEquals(GregorianDate(2026, 2, 17), HijriDateConverter.hijriToGregorian(1447, 9, 1))
        assertEquals(GregorianDate(2026, 3, 19), HijriDateConverter.hijriToGregorian(1447, 10, 1))
    }

    @Test
    fun `day 30 of a 30-day month maps correctly (no day-zero bug)`() {
        assertEquals(HijriDate(1447, 1, 30), HijriDateConverter.gregorianToHijri(2025, 7, 25))
        assertEquals(GregorianDate(2025, 7, 25), HijriDateConverter.hijriToGregorian(1447, 1, 30))
    }

    @Test
    fun `weekdayOf is zero based on Sunday`() {
        assertEquals(0, HijriDateConverter.weekdayOf(2025, 6, 1))
        assertEquals(2, HijriDateConverter.weekdayOf(2025, 7, 1))
        assertEquals(4, HijriDateConverter.weekdayOf(2025, 6, 26))
        assertEquals(5, HijriDateConverter.weekdayOf(2025, 8, 1))
    }

    @Test
    fun `gregorian month lengths`() {
        assertEquals(29, HijriDateConverter.daysInGregorianMonth(2024, 2))
        assertEquals(28, HijriDateConverter.daysInGregorianMonth(2025, 2))
        assertEquals(31, HijriDateConverter.daysInGregorianMonth(2025, 7))
        assertEquals(30, HijriDateConverter.daysInGregorianMonth(2025, 6))
    }

    @Test
    fun `islamic leap years and month lengths`() {
        assertTrue(HijriDateConverter.isIslamicLeapYear(1447))
        assertFalse(HijriDateConverter.isIslamicLeapYear(1446))
        assertEquals(30, HijriDateConverter.daysInHijriMonth(1447, 1))
        assertEquals(29, HijriDateConverter.daysInHijriMonth(1447, 2))
        assertEquals(30, HijriDateConverter.daysInHijriMonth(1447, 12))
        assertEquals(29, HijriDateConverter.daysInHijriMonth(1446, 12))
    }

    @Test
    fun `round trip gregorian to hijri to gregorian over a full month`() {
        for (day in 1..30) {
            val hijri = HijriDateConverter.gregorianToHijri(2025, 6, day)
            val back = HijriDateConverter.hijriToGregorian(hijri.year, hijri.month, hijri.day)
            assertEquals("round trip failed for 2025-06-$day -> $hijri", GregorianDate(2025, 6, day), back)
        }
    }

    @Test
    fun `all days of Muharram 1447 round trip`() {
        for (day in 1..30) {
            val g = HijriDateConverter.hijriToGregorian(1447, 1, day)
            val h = HijriDateConverter.gregorianToHijri(g.year, g.month, g.day)
            assertEquals(HijriDate(1447, 1, day), h)
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; cmd /c "gradlew.bat :domain:test --tests com.smiledev.rafiq.domain.util.HijriDateConverterTest"`
Expected: FAIL to compile — `HijriDateConverter`, `HijriDate`, `GregorianDate` not defined.

- [ ] **Step 3: Write the models**

`domain/src/main/kotlin/com/smiledev/rafiq/domain/model/HijriDate.kt`:

```kotlin
package com.smiledev.rafiq.domain.model

data class HijriDate(val year: Int, val month: Int, val day: Int)
```

`domain/src/main/kotlin/com/smiledev/rafiq/domain/model/GregorianDate.kt`:

```kotlin
package com.smiledev.rafiq.domain.model

data class GregorianDate(val year: Int, val month: Int, val day: Int)
```

`domain/src/main/kotlin/com/smiledev/rafiq/domain/util/TodayProvider.kt`:

```kotlin
package com.smiledev.rafiq.domain.util

import com.smiledev.rafiq.domain.model.GregorianDate
import java.util.Calendar

fun interface TodayProvider {
    fun today(): GregorianDate
}

object SystemTodayProvider : TodayProvider {
    override fun today(): GregorianDate {
        val cal = Calendar.getInstance()
        return GregorianDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }
}
```

- [ ] **Step 4: Write the converter**

`domain/src/main/kotlin/com/smiledev/rafiq/domain/util/HijriDateConverter.kt`:

```kotlin
package com.smiledev.rafiq.domain.util

import com.smiledev.rafiq.domain.model.GregorianDate
import com.smiledev.rafiq.domain.model.HijriDate
import kotlin.math.min

object HijriDateConverter {

    const val ISLAMIC_EPOCH = 227015L

    fun gregorianToHijri(year: Int, month: Int, day: Int): HijriDate {
        val fixed = gregorianToFixed(year, month, day)
        val hijriYear = ((30 * (fixed - ISLAMIC_EPOCH) + 10646) / 10631).toInt()
        val yearStart = islamicToFixed(hijriYear, 1, 1)
        val hijriMonth = min(12, ((fixed - yearStart) * 2) / 59 + 1).toInt()
        val hijriDay = (fixed - (islamicToFixed(hijriYear, hijriMonth, 1) - 1)).toInt()
        return HijriDate(hijriYear, hijriMonth, hijriDay)
    }

    fun hijriToGregorian(year: Int, month: Int, day: Int): GregorianDate {
        return jdnToGregorian(islamicToFixed(year, month, day) + RD_TO_JDN)
    }

    fun isIslamicLeapYear(year: Int): Boolean = (11 * year + 14) % 30 < 11

    fun daysInHijriMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 9, 11 -> 30
        12 -> if (isIslamicLeapYear(year)) 30 else 29
        else -> 29
    }

    fun daysInGregorianMonth(year: Int, month: Int): Int = when (month) {
        2 -> if (isGregorianLeapYear(year)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    fun weekdayOf(year: Int, month: Int, day: Int): Int {
        return ((gregorianToJdn(year, month, day) + 1) % 7).toInt()
    }

    private const val RD_TO_JDN = 1721424L

    private fun gregorianToFixed(year: Int, month: Int, day: Int): Long {
        return gregorianToJdn(year, month, day) - RD_TO_JDN
    }

    private fun gregorianToJdn(year: Int, month: Int, day: Int): Long {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return (day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045).toLong()
    }

    private fun islamicToFixed(year: Int, month: Int, day: Int): Long {
        return ISLAMIC_EPOCH + (year - 1) * 354L + (3 + 11L * year) / 30 +
            (59L * (month - 1) + 1) / 2 + (day - 1)
    }

    private fun jdnToGregorian(jdn: Long): GregorianDate {
        val a = jdn + 32044
        val b = (4 * a + 3) / 146097
        val c = a - (146097 * b) / 4
        val d = (4 * c + 3) / 1461
        val e = c - (1461 * d) / 4
        val m = (5 * e + 2) / 153
        val day = (e - (153 * m + 2) / 5 + 1).toInt()
        val month = (m + 3 - 12 * (m / 10)).toInt()
        val year = (100 * b + d - 4800 + m / 10).toInt()
        return GregorianDate(year, month, day)
    }

    private fun isGregorianLeapYear(year: Int): Boolean =
        year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; cmd /c "gradlew.bat :domain:test --tests com.smiledev.rafiq.domain.util.HijriDateConverterTest"`
Expected: PASS, all 9 tests green.

- [ ] **Step 6: Commit**

```bash
git add domain/src/main/kotlin/com/smiledev/rafiq/domain/model/HijriDate.kt domain/src/main/kotlin/com/smiledev/rafiq/domain/model/GregorianDate.kt domain/src/main/kotlin/com/smiledev/rafiq/domain/util/TodayProvider.kt domain/src/main/kotlin/com/smiledev/rafiq/domain/util/HijriDateConverter.kt domain/src/test/kotlin/com/smiledev/rafiq/domain/util/HijriDateConverterTest.kt
git commit -m "feat: add tabular hijri date converter with today provider"
```

---

### Task 2: Fix `getTodayEvents()` and bind `TodayProvider` (TDD)

**Files:**
- Modify: `data/src/main/kotlin/com/smiledev/rafiq/data/repository/IslamicCalendarRepositoryImpl.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq/di/AppModule.kt`
- Test: `data/src/test/kotlin/com/smiledev/rafiq/data/repository/IslamicCalendarRepositoryImplTest.kt` (mirrors `QuranRepositoryImplTest` Robolectric+mockk AssetManager pattern)

**Interfaces:**
- Consumes: `HijriDateConverter`, `TodayProvider`, `SystemTodayProvider`, `GregorianDate` (all from Task 1); `IslamicEvent` (`domain.model`, unchanged).
- Produces: `IslamicCalendarRepositoryImpl` gains a second constructor param `todayProvider: TodayProvider = SystemTodayProvider`; `getTodayEvents()` now converts today to Hijri and filters on `hijriMonth`/`hijriDay` with NO fallback.
- Produces: `AppModule.provideTodayProvider(): TodayProvider = SystemTodayProvider` (required for Hilt to satisfy the new constructor param on both repo and ViewModel).

- [ ] **Step 1: Write the failing test**

`data/src/test/kotlin/com/smiledev/rafiq/data/repository/IslamicCalendarRepositoryImplTest.kt`:

```kotlin
package com.smiledev.rafiq.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.GregorianDate
import com.smiledev.rafiq.domain.util.TodayProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class IslamicCalendarRepositoryImplTest {

    private val assetManager: AssetManager = mockk()
    private lateinit var repo: IslamicCalendarRepositoryImpl

    @Before
    fun setUp() {
        val context: Context = mockk(relaxed = true)
        every { context.assets } returns assetManager
        repo = IslamicCalendarRepositoryImpl(
            context,
            todayProvider = TodayProvider { GregorianDate(2025, 7, 5) }
        )
    }

    @Test
    fun `getTodayEvents matches today via hijri conversion`() {
        val json = """
            [
              {"hijri_month": 1, "hijri_day": 10, "title_en": "Day of Ashura", "title_id": "Hari Asyura", "description_en": "D", "description_id": "D", "event_type": "observance"},
              {"hijri_month": 1, "hijri_day": 1, "title_en": "Islamic New Year", "title_id": "Tahun Baru Islam", "description_en": "D", "description_id": "D", "event_type": "holiday"}
            ]
        """.trimIndent()
        every { assetManager.open("quran-data/islamic_events.json") } returns ByteArrayInputStream(json.toByteArray())

        val result = repo.getTodayEvents()

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val events = (result as Result.Success).data
        assertEquals(1, events.size)
        assertEquals("Day of Ashura", events.single().titleEn)
    }

    @Test
    fun `getTodayEvents returns empty when nothing matches and no fallback`() {
        val json = """
            [
              {"hijri_month": 1, "hijri_day": 1, "title_en": "Islamic New Year", "title_id": "Tahun Baru Islam", "description_en": "D", "description_id": "D", "event_type": "holiday"}
            ]
        """.trimIndent()
        every { assetManager.open("quran-data/islamic_events.json") } returns ByteArrayInputStream(json.toByteArray())

        val result = repo.getTodayEvents()

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }

    @Test
    fun `getTodayEvents returns error on missing asset`() {
        every { assetManager.open("quran-data/islamic_events.json") } throws RuntimeException("File not found")

        val result = repo.getTodayEvents()

        assertTrue("Expected Error but got ${result}", result is Result.Error)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; cmd /c "gradlew.bat :data:testDebugUnitTest --tests com.smiledev.rafiq.data.repository.IslamicCalendarRepositoryImplTest"`
Expected: FAIL to compile — `IslamicCalendarRepositoryImpl` has no 2-arg constructor.

- [ ] **Step 3: Update the repository implementation**

Modify `data/src/main/kotlin/com/smiledev/rafiq/data/repository/IslamicCalendarRepositoryImpl.kt`:

- Replace the imports block. Remove `import java.util.Calendar`. Add:
  ```kotlin
  import com.smiledev.rafiq.domain.util.HijriDateConverter
  import com.smiledev.rafiq.domain.util.SystemTodayProvider
  import com.smiledev.rafiq.domain.util.TodayProvider
  ```
- Replace the class declaration:
  ```kotlin
  @Singleton
  class IslamicCalendarRepositoryImpl @Inject constructor(
      @ApplicationContext private val context: Context,
      private val todayProvider: TodayProvider = SystemTodayProvider
  ) : IslamicCalendarRepository {
  ```
- Replace `getTodayEvents()`:
  ```kotlin
  override fun getTodayEvents(): Result<List<IslamicEvent>, AppError> {
      return try {
          when (val result = getEvents()) {
              is Result.Success -> {
                  val today = todayProvider.today()
                  val todayHijri = HijriDateConverter.gregorianToHijri(today.year, today.month, today.day)
                  Result.Success(
                      result.data.filter {
                          it.hijriMonth == todayHijri.month && it.hijriDay == todayHijri.day
                      }
                  )
              }
              is Result.Error -> result
          }
      } catch (e: Exception) {
          Result.Error(AppError.Database("Failed to get today events", e))
      }
  }
  ```
- Leave `getEvents()`, `getEventsForMonth()`, `islamicMonthNames`, `islamicMonthNamesId`, `readAssetJsonArray()` unchanged.

- [ ] **Step 4: Bind `TodayProvider` in Hilt**

Modify `app/src/main/java/com/smiledev/rafiq/di/AppModule.kt`:

- Add imports:
  ```kotlin
  import com.smiledev.rafiq.domain.util.SystemTodayProvider
  import com.smiledev.rafiq.domain.util.TodayProvider
  ```
- Add a provider in `object AppModule` (next to `provideDispatcherProvider`):
  ```kotlin
  @Provides
  @Singleton
  fun provideTodayProvider(): TodayProvider = SystemTodayProvider
  ```

- [ ] **Step 5: Run test to verify it passes**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; cmd /c "gradlew.bat :data:testDebugUnitTest --tests com.smiledev.rafiq.data.repository.IslamicCalendarRepositoryImplTest"`
Expected: PASS, all 3 tests green.

- [ ] **Step 6: Commit**

```bash
git add data/src/main/kotlin/com/smiledev/rafiq/data/repository/IslamicCalendarRepositoryImpl.kt data/src/test/kotlin/com/smiledev/rafiq/data/repository/IslamicCalendarRepositoryImplTest.kt app/src/main/java/com/smiledev/rafiq/di/AppModule.kt
git commit -m "fix: match today's events by hijri date with no fallback"
```

---

### Task 3: Expand the Islamic events dataset (57 events)

**Files:**
- Modify: `app/src/main/assets/quran-data/islamic_events.json` (18 → 57 events)
- Create: `data/src/test/resources/expanded_events.json` (identical content — fixture for the parse-all test)
- Test: add `parses all 57 expanded events` to `data/src/test/kotlin/com/smiledev/rafiq/data/repository/IslamicCalendarRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `IslamicCalendarRepositoryImpl.getEvents()` JSON contract (fields `hijri_month`, `hijri_day`, `title_en`, `title_id`, `description_en`, `description_id`, `event_type`). No parser change needed.
- Produces: 57-event dataset = the existing 18 + Ayyam al-Bid (days 13/14/15 of every month, 36 entries) + Laylat al-Qadr odd nights 9/23, 9/25, 9/29 (3 entries). Duplicate (month,day) pairs are intentional only for 8/15 (Nisfu Sha'aban + Ayyam al-Bid 15th) and 12/13 (Eid al-Adha Day 4 + Ayyam al-Bid 13th).

- [ ] **Step 1: Replace the app asset with the full expanded JSON**

Write the ENTIRE content below to `app/src/main/assets/quran-data/islamic_events.json` (overwriting the 18-event file):

```json
[
  {
    "hijri_month": 1,
    "hijri_day": 1,
    "title_en": "Islamic New Year",
    "title_id": "Tahun Baru Islam",
    "description_en": "First day of Muharram, marking the beginning of the Islamic lunar calendar year.",
    "description_id": "Hari pertama Muharram, menandai awal tahun kalender Islam.",
    "event_type": "holiday"
  },
  {
    "hijri_month": 1,
    "hijri_day": 10,
    "title_en": "Day of Ashura",
    "title_id": "Hari Asyura",
    "description_en": "Recommended fasting day. Commemorates the salvation of Musa (AS) from Pharaoh and the martyrdom of Husayn ibn Ali.",
    "description_id": "Hari puasa yang dianjurkan. Memperingati penyelamatan Musa AS dari Firaun dan syahidnya Husain bin Ali.",
    "event_type": "observance"
  },
  {
    "hijri_month": 1,
    "hijri_day": 13,
    "title_en": "Ayyam al-Bid (13th)",
    "title_id": "Ayyamul Bidh (13)",
    "description_en": "The white days. Recommended to fast on the 13th of Muharram, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 13 Muharram, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 1,
    "hijri_day": 14,
    "title_en": "Ayyam al-Bid (14th)",
    "title_id": "Ayyamul Bidh (14)",
    "description_en": "The white days. Recommended to fast on the 14th of Muharram, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 14 Muharram, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 1,
    "hijri_day": 15,
    "title_en": "Ayyam al-Bid (15th)",
    "title_id": "Ayyamul Bidh (15)",
    "description_en": "The white days. Recommended to fast on the 15th of Muharram, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 15 Muharram, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 2,
    "hijri_day": 13,
    "title_en": "Ayyam al-Bid (13th)",
    "title_id": "Ayyamul Bidh (13)",
    "description_en": "The white days. Recommended to fast on the 13th of Safar, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 13 Safar, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 2,
    "hijri_day": 14,
    "title_en": "Ayyam al-Bid (14th)",
    "title_id": "Ayyamul Bidh (14)",
    "description_en": "The white days. Recommended to fast on the 14th of Safar, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 14 Safar, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 2,
    "hijri_day": 15,
    "title_en": "Ayyam al-Bid (15th)",
    "title_id": "Ayyamul Bidh (15)",
    "description_en": "The white days. Recommended to fast on the 15th of Safar, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 15 Safar, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 3,
    "hijri_day": 12,
    "title_en": "Mawlid al-Nabi",
    "title_id": "Maulid Nabi",
    "description_en": "Birth of the Prophet Muhammad (PBUH), celebrated by Muslims worldwide.",
    "description_id": "Kelahiran Nabi Muhammad SAW, dirayakan oleh umat Islam di seluruh dunia.",
    "event_type": "holiday"
  },
  {
    "hijri_month": 3,
    "hijri_day": 13,
    "title_en": "Ayyam al-Bid (13th)",
    "title_id": "Ayyamul Bidh (13)",
    "description_en": "The white days. Recommended to fast on the 13th of Rabi' al-Awwal, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 13 Rabi'ul Awwal, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 3,
    "hijri_day": 14,
    "title_en": "Ayyam al-Bid (14th)",
    "title_id": "Ayyamul Bidh (14)",
    "description_en": "The white days. Recommended to fast on the 14th of Rabi' al-Awwal, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 14 Rabi'ul Awwal, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 3,
    "hijri_day": 15,
    "title_en": "Ayyam al-Bid (15th)",
    "title_id": "Ayyamul Bidh (15)",
    "description_en": "The white days. Recommended to fast on the 15th of Rabi' al-Awwal, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 15 Rabi'ul Awwal, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 4,
    "hijri_day": 13,
    "title_en": "Ayyam al-Bid (13th)",
    "title_id": "Ayyamul Bidh (13)",
    "description_en": "The white days. Recommended to fast on the 13th of Rabi' al-Thani, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 13 Rabi'ul Tsani, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 4,
    "hijri_day": 14,
    "title_en": "Ayyam al-Bid (14th)",
    "title_id": "Ayyamul Bidh (14)",
    "description_en": "The white days. Recommended to fast on the 14th of Rabi' al-Thani, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 14 Rabi'ul Tsani, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 4,
    "hijri_day": 15,
    "title_en": "Ayyam al-Bid (15th)",
    "title_id": "Ayyamul Bidh (15)",
    "description_en": "The white days. Recommended to fast on the 15th of Rabi' al-Thani, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 15 Rabi'ul Tsani, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 5,
    "hijri_day": 13,
    "title_en": "Ayyam al-Bid (13th)",
    "title_id": "Ayyamul Bidh (13)",
    "description_en": "The white days. Recommended to fast on the 13th of Jumada al-Awwal, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 13 Jumadil Awwal, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 5,
    "hijri_day": 14,
    "title_en": "Ayyam al-Bid (14th)",
    "title_id": "Ayyamul Bidh (14)",
    "description_en": "The white days. Recommended to fast on the 14th of Jumada al-Awwal, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 14 Jumadil Awwal, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 5,
    "hijri_day": 15,
    "title_en": "Ayyam al-Bid (15th)",
    "title_id": "Ayyamul Bidh (15)",
    "description_en": "The white days. Recommended to fast on the 15th of Jumada al-Awwal, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 15 Jumadil Awwal, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 6,
    "hijri_day": 13,
    "title_en": "Ayyam al-Bid (13th)",
    "title_id": "Ayyamul Bidh (13)",
    "description_en": "The white days. Recommended to fast on the 13th of Jumada al-Thani, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 13 Jumadil Tsani, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 6,
    "hijri_day": 14,
    "title_en": "Ayyam al-Bid (14th)",
    "title_id": "Ayyamul Bidh (14)",
    "description_en": "The white days. Recommended to fast on the 14th of Jumada al-Thani, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 14 Jumadil Tsani, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 6,
    "hijri_day": 15,
    "title_en": "Ayyam al-Bid (15th)",
    "title_id": "Ayyamul Bidh (15)",
    "description_en": "The white days. Recommended to fast on the 15th of Jumada al-Thani, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 15 Jumadil Tsani, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 7,
    "hijri_day": 13,
    "title_en": "Ayyam al-Bid (13th)",
    "title_id": "Ayyamul Bidh (13)",
    "description_en": "The white days. Recommended to fast on the 13th of Rajab, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 13 Rajab, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 7,
    "hijri_day": 14,
    "title_en": "Ayyam al-Bid (14th)",
    "title_id": "Ayyamul Bidh (14)",
    "description_en": "The white days. Recommended to fast on the 14th of Rajab, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 14 Rajab, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 7,
    "hijri_day": 15,
    "title_en": "Ayyam al-Bid (15th)",
    "title_id": "Ayyamul Bidh (15)",
    "description_en": "The white days. Recommended to fast on the 15th of Rajab, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 15 Rajab, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 7,
    "hijri_day": 27,
    "title_en": "Isra' Mi'raj",
    "title_id": "Isra' Mi'raj",
    "description_en": "The Night Journey and Ascension of the Prophet Muhammad (PBUH) to the heavens.",
    "description_id": "Perjalanan malam dan kenaikan Nabi Muhammad SAW ke langit.",
    "event_type": "observance"
  },
  {
    "hijri_month": 8,
    "hijri_day": 13,
    "title_en": "Ayyam al-Bid (13th)",
    "title_id": "Ayyamul Bidh (13)",
    "description_en": "The white days. Recommended to fast on the 13th of Sha'aban, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 13 Sya'ban, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 8,
    "hijri_day": 14,
    "title_en": "Ayyam al-Bid (14th)",
    "title_id": "Ayyamul Bidh (14)",
    "description_en": "The white days. Recommended to fast on the 14th of Sha'aban, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 14 Sya'ban, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 8,
    "hijri_day": 15,
    "title_en": "Nisfu Sha'aban",
    "title_id": "Nisfu Sya'ban",
    "description_en": "The night of forgiveness. Recommended to increase worship and supplication.",
    "description_id": "Malam pengampunan. Dianjurkan untuk meningkatkan ibadah dan doa.",
    "event_type": "observance"
  },
  {
    "hijri_month": 8,
    "hijri_day": 15,
    "title_en": "Ayyam al-Bid (15th)",
    "title_id": "Ayyamul Bidh (15)",
    "description_en": "The white days. Recommended to fast on the 15th of Sha'aban, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 15 Sya'ban, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 9,
    "hijri_day": 1,
    "title_en": "First Day of Ramadan",
    "title_id": "Hari Pertama Ramadan",
    "description_en": "Beginning of the holy month of fasting from dawn until sunset.",
    "description_id": "Awal bulan suci puasa dari fajar hingga terbenam matahari.",
    "event_type": "holiday"
  },
  {
    "hijri_month": 9,
    "hijri_day": 10,
    "title_en": "Battle of Badr Anniversary",
    "title_id": "Peringatan Perang Badar",
    "description_en": "Commemorates the victory of the Muslims in the Battle of Badr.",
    "description_id": "Memperingati kemenangan umat Islam dalam Perang Badar.",
    "event_type": "observance"
  },
  {
    "hijri_month": 9,
    "hijri_day": 13,
    "title_en": "Ayyam al-Bid (13th)",
    "title_id": "Ayyamul Bidh (13)",
    "description_en": "The white days. Recommended to fast on the 13th of Ramadan, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 13 Ramadan, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 9,
    "hijri_day": 14,
    "title_en": "Ayyam al-Bid (14th)",
    "title_id": "Ayyamul Bidh (14)",
    "description_en": "The white days. Recommended to fast on the 14th of Ramadan, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 14 Ramadan, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 9,
    "hijri_day": 15,
    "title_en": "Ayyam al-Bid (15th)",
    "title_id": "Ayyamul Bidh (15)",
    "description_en": "The white days. Recommended to fast on the 15th of Ramadan, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 15 Ramadan, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 9,
    "hijri_day": 17,
    "title_en": "Nuzul al-Qur'an",
    "title_id": "Nuzulul Qur'an",
    "description_en": "The revelation of the first verses of the Qur'an to Prophet Muhammad (PBUH).",
    "description_id": "Turunnya ayat-ayat pertama Al-Qur'an kepada Nabi Muhammad SAW.",
    "event_type": "observance"
  },
  {
    "hijri_month": 9,
    "hijri_day": 21,
    "title_en": "Laylat al-Qadr (Odd Night)",
    "title_id": "Lailatul Qadar (Malam Ganjil)",
    "description_en": "The Night of Power, better than a thousand months. One of the last ten odd nights of Ramadan.",
    "description_id": "Malam kemuliaan, lebih baik dari seribu bulan. Salah satu malam ganjil sepuluh terakhir Ramadan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 9,
    "hijri_day": 23,
    "title_en": "Laylat al-Qadr (23rd Night)",
    "title_id": "Lailatul Qadar (Malam ke-23)",
    "description_en": "A likely Night of Power among the last ten nights of Ramadan.",
    "description_id": "Malam yang berpeluang sebagai Lailatul Qadar di sepuluh malam terakhir Ramadan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 9,
    "hijri_day": 25,
    "title_en": "Laylat al-Qadr (25th Night)",
    "title_id": "Lailatul Qadar (Malam ke-25)",
    "description_en": "A likely Night of Power among the last ten nights of Ramadan.",
    "description_id": "Malam yang berpeluang sebagai Lailatul Qadar di sepuluh malam terakhir Ramadan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 9,
    "hijri_day": 27,
    "title_en": "Laylat al-Qadr (27th Night)",
    "title_id": "Lailatul Qadar (Malam ke-27)",
    "description_en": "The most probable night for Laylat al-Qadr, when the Qur'an was revealed.",
    "description_id": "Malam yang paling mungkin untuk Lailatul Qadar, saat Al-Qur'an diturunkan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 9,
    "hijri_day": 29,
    "title_en": "Laylat al-Qadr (29th Night)",
    "title_id": "Lailatul Qadar (Malam ke-29)",
    "description_en": "A likely Night of Power among the last ten nights of Ramadan.",
    "description_id": "Malam yang berpeluang sebagai Lailatul Qadar di sepuluh malam terakhir Ramadan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 10,
    "hijri_day": 1,
    "title_en": "Eid al-Fitr",
    "title_id": "Idul Fitri",
    "description_en": "Festival of Breaking the Fast, marking the end of Ramadan. A day of celebration, prayers, and charity.",
    "description_id": "Hari Raya Idul Fitri, menandai berakhirnya Ramadan. Hari perayaan, shalat, dan sedekah.",
    "event_type": "holiday"
  },
  {
    "hijri_month": 10,
    "hijri_day": 2,
    "title_en": "Eid al-Fitr (Day 2)",
    "title_id": "Idul Fitri (Hari ke-2)",
    "description_en": "Second day of Eid al-Fitr celebrations.",
    "description_id": "Hari kedua perayaan Idul Fitri.",
    "event_type": "holiday"
  },
  {
    "hijri_month": 10,
    "hijri_day": 13,
    "title_en": "Ayyam al-Bid (13th)",
    "title_id": "Ayyamul Bidh (13)",
    "description_en": "The white days. Recommended to fast on the 13th of Shawwal, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 13 Syawwal, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 10,
    "hijri_day": 14,
    "title_en": "Ayyam al-Bid (14th)",
    "title_id": "Ayyamul Bidh (14)",
    "description_en": "The white days. Recommended to fast on the 14th of Shawwal, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 14 Syawwal, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 10,
    "hijri_day": 15,
    "title_en": "Ayyam al-Bid (15th)",
    "title_id": "Ayyamul Bidh (15)",
    "description_en": "The white days. Recommended to fast on the 15th of Shawwal, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 15 Syawwal, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 11,
    "hijri_day": 13,
    "title_en": "Ayyam al-Bid (13th)",
    "title_id": "Ayyamul Bidh (13)",
    "description_en": "The white days. Recommended to fast on the 13th of Dhul-Qi'dah, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 13 Dzulqa'dah, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 11,
    "hijri_day": 14,
    "title_en": "Ayyam al-Bid (14th)",
    "title_id": "Ayyamul Bidh (14)",
    "description_en": "The white days. Recommended to fast on the 14th of Dhul-Qi'dah, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 14 Dzulqa'dah, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 11,
    "hijri_day": 15,
    "title_en": "Ayyam al-Bid (15th)",
    "title_id": "Ayyamul Bidh (15)",
    "description_en": "The white days. Recommended to fast on the 15th of Dhul-Qi'dah, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 15 Dzulqa'dah, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 12,
    "hijri_day": 8,
    "title_en": "Yawm al-Tarwiyah",
    "title_id": "Hari Tarwiyah",
    "description_en": "Eighth day of Dhul Hijjah. Pilgrims begin their Hajj rituals.",
    "description_id": "Hari kedelapan Dzulhijjah. Jemaah haji memulai ritual haji.",
    "event_type": "observance"
  },
  {
    "hijri_month": 12,
    "hijri_day": 9,
    "title_en": "Day of Arafah",
    "title_id": "Hari Arafah",
    "description_en": "The most important day of Hajj. Recommended fasting for non-pilgrims.",
    "description_id": "Hari terpenting dalam haji. Puasa dianjurkan bagi yang tidak berhaji.",
    "event_type": "observance"
  },
  {
    "hijri_month": 12,
    "hijri_day": 10,
    "title_en": "Eid al-Adha",
    "title_id": "Idul Adha",
    "description_en": "Festival of Sacrifice, commemorating Ibrahim's (AS) willingness to sacrifice his son.",
    "description_id": "Hari Raya Kurban, memperingati ketaatan Ibrahim AS.",
    "event_type": "holiday"
  },
  {
    "hijri_month": 12,
    "hijri_day": 11,
    "title_en": "Eid al-Adha (Day 2)",
    "title_id": "Idul Adha (Hari ke-2)",
    "description_en": "Second day of Tashriq, continuing Eid celebrations.",
    "description_id": "Hari kedua Tasyrik, melanjutkan perayaan Idul Adha.",
    "event_type": "holiday"
  },
  {
    "hijri_month": 12,
    "hijri_day": 12,
    "title_en": "Eid al-Adha (Day 3)",
    "title_id": "Idul Adha (Hari ke-3)",
    "description_en": "Third day of Tashriq, continuing Eid celebrations.",
    "description_id": "Hari ketiga Tasyrik, melanjutkan perayaan Idul Adha.",
    "event_type": "holiday"
  },
  {
    "hijri_month": 12,
    "hijri_day": 13,
    "title_en": "Eid al-Adha (Day 4)",
    "title_id": "Idul Adha (Hari ke-4)",
    "description_en": "Final day of Tashriq, last day of Eid al-Adha.",
    "description_id": "Hari terakhir Tasyrik, hari terakhir Idul Adha.",
    "event_type": "holiday"
  },
  {
    "hijri_month": 12,
    "hijri_day": 13,
    "title_en": "Ayyam al-Bid (13th)",
    "title_id": "Ayyamul Bidh (13)",
    "description_en": "The white days. Recommended to fast on the 13th of Dhul-Hijjah, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 13 Dzulhijjah, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 12,
    "hijri_day": 14,
    "title_en": "Ayyam al-Bid (14th)",
    "title_id": "Ayyamul Bidh (14)",
    "description_en": "The white days. Recommended to fast on the 14th of Dhul-Hijjah, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 14 Dzulhijjah, hari pertengahan bulan.",
    "event_type": "observance"
  },
  {
    "hijri_month": 12,
    "hijri_day": 15,
    "title_en": "Ayyam al-Bid (15th)",
    "title_id": "Ayyamul Bidh (15)",
    "description_en": "The white days. Recommended to fast on the 15th of Dhul-Hijjah, the middle days of the month.",
    "description_id": "Hari-hari putih. Dianjurkan berpuasa pada tanggal 15 Dzulhijjah, hari pertengahan bulan.",
    "event_type": "observance"
  }
]
```

- [ ] **Step 2: Create the test fixture copy**

Copy the identical content (from Step 1) to `data/src/test/resources/expanded_events.json`. The executing engineer copies the file byte-for-byte from Step 1.

- [ ] **Step 3: Write the parse-all test**

Add to `data/src/test/kotlin/com/smiledev/rafiq/data/repository/IslamicCalendarRepositoryImplTest.kt` (same file as Task 2). Add imports `org.junit.Assert.assertNotNull` and update the class with this test:

```kotlin
    @Test
    fun `repository parses all 57 expanded events with valid fields`() {
        val resource = javaClass.classLoader.getResourceAsStream("expanded_events.json")
        assertNotNull("expanded_events.json test resource missing", resource)
        every { assetManager.open("quran-data/islamic_events.json") } returns ByteArrayInputStream(resource!!.readBytes())

        val result = repo.getEvents()

        assertTrue("Expected Success but got ${result}", result is Result.Success)
        val events = (result as Result.Success).data
        assertEquals(57, events.size)
        events.forEach { event ->
            assertTrue("month out of range: ${event.hijriMonth}", event.hijriMonth in 1..12)
            assertTrue("day out of range: ${event.hijriDay}", event.hijriDay in 1..30)
            assertTrue(event.titleEn.isNotBlank())
            assertTrue(event.titleId.isNotBlank())
            assertTrue(event.descriptionEn.isNotBlank())
            assertTrue(event.descriptionId.isNotBlank())
            assertTrue(event.eventType == "holiday" || event.eventType == "observance")
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; cmd /c "gradlew.bat :data:testDebugUnitTest --tests com.smiledev.rafiq.data.repository.IslamicCalendarRepositoryImplTest"`
Expected: PASS, all 4 tests green (count assertion 57 holds).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/quran-data/islamic_events.json data/src/test/resources/expanded_events.json data/src/test/kotlin/com/smiledev/rafiq/data/repository/IslamicCalendarRepositoryImplTest.kt
git commit -m "feat: expand islamic events to 57 observances"
```

---

### Task 4: `CalendarViewModel` grid state and month navigation (TDD)

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/calendar/CalendarViewModel.kt`
- Test: `app/src/test/java/com/smiledev/rafiq/ui/calendar/CalendarViewModelTest.kt` (rewrite the file — the old `selectMonth`/`getEventsForMonth` behavior is gone)

**Interfaces:**
- Consumes: `HijriDateConverter`, `TodayProvider`, `SystemTodayProvider`, `GregorianDate`, `HijriDate` (Task 1); `IslamicCalendarRepository` (`getEvents()`, `getTodayEvents()`, `islamicMonthNames`, `islamicMonthNamesId`); `TestDispatcherProvider` (`app/src/test`).
- Produces (consumed by Task 5 screen):
  - `@Immutable data class CalendarDay(val gregorianDay: Int, val hijriDate: HijriDate, val events: List<IslamicEvent>, val isToday: Boolean)`
  - `@Immutable data class CalendarUiState(todayEvents, displayedYear, displayedMonth, grid: List<CalendarDay?>, selectedIndex: Int?, isLoading, error)` — `grid` is exactly 42 cells, `null` = blank (leading/trailing blanks).
  - `CalendarViewModel(repository, todayProvider: TodayProvider = SystemTodayProvider, dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider)`
  - Methods: `nextMonth()`, `previousMonth()`, `goToToday()`, `onDayClick(index: Int)`, `dismissDaySheet()`, `getMonthName(month: Int, indonesian: Boolean)`, `val localeCode`, `val monthNames`, `val uiState`.

Grid rules (must match the screen in Task 5):
- Leading blanks = `HijriDateConverter.weekdayOf(year, month, 1)` (0=Sunday).
- Cell index = `leadingBlanks + gregorianDay - 1`.
- Per-day events = events where `event.hijriMonth == hijri.month && event.hijriDay == hijri.day` (matching by the day's own converted Hijri date — this is what fixes "today" and correct-dot placement; no event-year assumption).
- `isToday` = the cell's Gregorian date equals `todayProvider.today()`.

- [ ] **Step 1: Write the failing test**

Rewrite `app/src/test/java/com/smiledev/rafiq/ui/calendar/CalendarViewModelTest.kt`:

```kotlin
package com.smiledev.rafiq.ui.calendar

import com.smiledev.rafiq.TestDispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.domain.model.GregorianDate
import com.smiledev.rafiq.domain.model.HijriDate
import com.smiledev.rafiq.domain.model.IslamicEvent
import com.smiledev.rafiq.domain.repository.IslamicCalendarRepository
import com.smiledev.rafiq.domain.util.TodayProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val repository: IslamicCalendarRepository = mockk()
    private val todayProvider = TodayProvider { GregorianDate(2025, 7, 5) }

    private val events = listOf(
        IslamicEvent(1, 10, "Day of Ashura", "Hari Asyura", "D", "D", "observance"),
        IslamicEvent(10, 1, "Eid al-Fitr", "Idul Fitri", "D", "D", "holiday")
    )

    private val monthNames = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhul-Qi'dah", "Dhul-Hijjah"
    )

    private fun stubRepository(todayEvents: List<IslamicEvent> = emptyList()) {
        every { repository.getEvents() } returns Result.Success(events)
        every { repository.getTodayEvents() } returns Result.Success(todayEvents)
        every { repository.islamicMonthNames } returns monthNames
        every { repository.islamicMonthNamesId } returns monthNames
    }

    @Test
    fun `grid for July 2025 has 42 cells and places Ashura on July 5`() = runTest(testDispatcher) {
        stubRepository()
        val vm = CalendarViewModel(repository, todayProvider, testDispatcherProvider)
        advanceUntilIdle()

        val grid = vm.uiState.value.grid
        assertEquals(42, grid.size)
        assertNull(grid[0])
        assertNull(grid[1])
        // July 1 2025 is Tuesday -> index 2 -> 1 Muharram 1447 day 6
        assertEquals(1, grid[2]?.gregorianDay)
        assertEquals(HijriDate(1447, 1, 6), grid[2]?.hijriDate)
        // July 5 2025 = 10 Muharram 1447 (Ashura) -> index 6
        assertEquals(5, grid[6]?.gregorianDay)
        assertEquals(HijriDate(1447, 1, 10), grid[6]?.hijriDate)
        assertEquals(1, grid[6]?.events?.size)
        assertEquals("Day of Ashura", grid[6]?.events?.single()?.titleEn)
        assertTrue(grid[6]?.isToday == true)
    }

    @Test
    fun `todayEvents loaded into state`() = runTest(testDispatcher) {
        stubRepository(todayEvents = events)
        val vm = CalendarViewModel(repository, todayProvider, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.todayEvents.size)
        assertEquals(2025, vm.uiState.value.displayedYear)
        assertEquals(7, vm.uiState.value.displayedMonth)
    }

    @Test
    fun `nextMonth and previousMonth navigate with year rollover`() = runTest(testDispatcher) {
        stubRepository()
        val vm = CalendarViewModel(repository, todayProvider, testDispatcherProvider)
        advanceUntilIdle()

        vm.nextMonth()
        advanceUntilIdle()
        assertEquals(8, vm.uiState.value.displayedMonth)
        assertEquals(2025, vm.uiState.value.displayedYear)

        vm.previousMonth()
        advanceUntilIdle()
        assertEquals(7, vm.uiState.value.displayedMonth)

        // roll to Dec 2025 then Jan 2026
        repeat(5) { vm.nextMonth(); advanceUntilIdle() }
        assertEquals(12, vm.uiState.value.displayedMonth)
        vm.nextMonth()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.displayedMonth)
        assertEquals(2026, vm.uiState.value.displayedYear)
    }

    @Test
    fun `goToToday returns to today month`() = runTest(testDispatcher) {
        stubRepository()
        val vm = CalendarViewModel(repository, todayProvider, testDispatcherProvider)
        advanceUntilIdle()

        vm.nextMonth()
        advanceUntilIdle()
        assertEquals(8, vm.uiState.value.displayedMonth)

        vm.goToToday()
        advanceUntilIdle()
        assertEquals(2025, vm.uiState.value.displayedYear)
        assertEquals(7, vm.uiState.value.displayedMonth)
    }

    @Test
    fun `onDayClick selects and dismissDaySheet clears`() = runTest(testDispatcher) {
        stubRepository()
        val vm = CalendarViewModel(repository, todayProvider, testDispatcherProvider)
        advanceUntilIdle()

        vm.onDayClick(6)
        assertEquals(6, vm.uiState.value.selectedIndex)

        vm.dismissDaySheet()
        assertNull(vm.uiState.value.selectedIndex)
    }

    @Test
    fun `events appear on the correct day across months`() = runTest(testDispatcher) {
        stubRepository()
        val vm = CalendarViewModel(repository, todayProvider, testDispatcherProvider)
        advanceUntilIdle()

        // Eid al-Fitr (10,1) = 1 Shawwal 1447 = 19 Mar 2026; navigate to Mar 2026
        repeat(8) { vm.nextMonth(); advanceUntilIdle() }
        assertEquals(3, vm.uiState.value.displayedMonth)
        assertEquals(2026, vm.uiState.value.displayedYear)

        val grid = vm.uiState.value.grid
        val cell = grid.firstOrNull { it?.gregorianDay == 19 }
        assertEquals("Eid al-Fitr", cell?.events?.single()?.titleEn)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; cmd /c "gradlew.bat :app:testDebugUnitTest --tests com.smiledev.rafiq.ui.calendar.CalendarViewModelTest"`
Expected: FAIL to compile — `CalendarDay`/`grid`/`selectedIndex` don't exist on the current state, and the constructor arity changed.

- [ ] **Step 3: Rewrite the ViewModel**

Replace the entire contents of `app/src/main/java/com/smiledev/rafiq/ui/calendar/CalendarViewModel.kt`:

```kotlin
package com.smiledev.rafiq.ui.calendar

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smiledev.rafiq.core.AppError
import com.smiledev.rafiq.core.DefaultDispatcherProvider
import com.smiledev.rafiq.core.DispatcherProvider
import com.smiledev.rafiq.core.Result
import com.smiledev.rafiq.core.currentLocaleCode
import com.smiledev.rafiq.domain.model.GregorianDate
import com.smiledev.rafiq.domain.model.HijriDate
import com.smiledev.rafiq.domain.model.IslamicEvent
import com.smiledev.rafiq.domain.repository.IslamicCalendarRepository
import com.smiledev.rafiq.domain.util.HijriDateConverter
import com.smiledev.rafiq.domain.util.SystemTodayProvider
import com.smiledev.rafiq.domain.util.TodayProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@Immutable
data class CalendarDay(
    val gregorianDay: Int,
    val hijriDate: HijriDate,
    val events: List<IslamicEvent>,
    val isToday: Boolean
)

@Immutable
data class CalendarUiState(
    val todayEvents: List<IslamicEvent> = emptyList(),
    val displayedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val displayedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val grid: List<CalendarDay?> = emptyList(),
    val selectedIndex: Int? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: IslamicCalendarRepository,
    private val todayProvider: TodayProvider = SystemTodayProvider,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState

    val localeCode = currentLocaleCode()

    val monthNames: List<String>
        get() = if (localeCode == "id") repository.islamicMonthNamesId else repository.islamicMonthNames

    init { load() }

    private fun load() {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val today = todayProvider.today()
            val todayResult = repository.getTodayEvents()
            val eventsResult = repository.getEvents()
            val todayEvents = if (todayResult is Result.Success) todayResult.data else emptyList()
            val events = if (eventsResult is Result.Success) eventsResult.data else emptyList()
            _uiState.value = _uiState.value.copy(
                todayEvents = todayEvents,
                displayedYear = today.year,
                displayedMonth = today.month,
                grid = buildGrid(today.year, today.month, events, today),
                isLoading = false,
                error = when {
                    todayResult is Result.Error -> todayResult.error
                    eventsResult is Result.Error -> eventsResult.error
                    else -> null
                }
            )
        }
    }

    fun nextMonth() {
        val s = _uiState.value
        val newMonth = if (s.displayedMonth == 12) 1 else s.displayedMonth + 1
        val newYear = if (s.displayedMonth == 12) s.displayedYear + 1 else s.displayedYear
        _uiState.value = s.copy(displayedYear = newYear, displayedMonth = newMonth, selectedIndex = null)
        refreshGrid()
    }

    fun previousMonth() {
        val s = _uiState.value
        val newMonth = if (s.displayedMonth == 1) 12 else s.displayedMonth - 1
        val newYear = if (s.displayedMonth == 1) s.displayedYear - 1 else s.displayedYear
        _uiState.value = s.copy(displayedYear = newYear, displayedMonth = newMonth, selectedIndex = null)
        refreshGrid()
    }

    fun goToToday() {
        val today = todayProvider.today()
        _uiState.value = _uiState.value.copy(
            displayedYear = today.year,
            displayedMonth = today.month,
            selectedIndex = null
        )
        refreshGrid()
    }

    fun onDayClick(index: Int) {
        _uiState.value = _uiState.value.copy(selectedIndex = index)
    }

    fun dismissDaySheet() {
        _uiState.value = _uiState.value.copy(selectedIndex = null)
    }

    fun getMonthName(month: Int, indonesian: Boolean = localeCode == "id"): String =
        if (indonesian) repository.islamicMonthNamesId.getOrElse(month - 1) { "" }
        else repository.islamicMonthNames.getOrElse(month - 1) { "" }

    private fun refreshGrid() {
        viewModelScope.launch(dispatcherProvider.io) {
            val s = _uiState.value
            val events = repository.getEvents().getOrNull() ?: emptyList()
            val today = todayProvider.today()
            _uiState.value = s.copy(grid = buildGrid(s.displayedYear, s.displayedMonth, events, today))
        }
    }

    private fun buildGrid(
        year: Int,
        month: Int,
        events: List<IslamicEvent>,
        today: GregorianDate
    ): List<CalendarDay?> {
        val daysInMonth = HijriDateConverter.daysInGregorianMonth(year, month)
        val leadingBlanks = HijriDateConverter.weekdayOf(year, month, 1)
        val grid = MutableList<CalendarDay?>(42) { null }
        for (day in 1..daysInMonth) {
            val hijri = HijriDateConverter.gregorianToHijri(year, month, day)
            grid[leadingBlanks + day - 1] = CalendarDay(
                gregorianDay = day,
                hijriDate = hijri,
                events = events.filter { it.hijriMonth == hijri.month && it.hijriDay == hijri.day },
                isToday = today.year == year && today.month == month && today.day == day
            )
        }
        return grid
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; cmd /c "gradlew.bat :app:testDebugUnitTest --tests com.smiledev.rafiq.ui.calendar.CalendarViewModelTest"`
Expected: PASS, all 6 tests green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/calendar/CalendarViewModel.kt app/src/test/java/com/smiledev/rafiq/ui/calendar/CalendarViewModelTest.kt
git commit -m "feat: add monthly hijri grid state with day navigation to calendar view model"
```

---

### Task 5: Rewrite `IslamicCalendarScreen` with the grid UI + localization

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq/ui/calendar/IslamicCalendarScreen.kt` (rewrite)
- Modify: `app/src/main/res/values/strings.xml` (add strings)
- Modify: `app/src/main/res/values-id/strings.xml` (add strings)

**Interfaces:**
- Consumes: `CalendarUiState`, `CalendarDay`, `CalendarViewModel` methods (`onDayClick`, `dismissDaySheet`, `goToToday`, `nextMonth`, `previousMonth`, `getMonthName`, `localeCode`) from Task 4; `HijriDate` for the sheet header.
- Produces: the composed screen. No repository/domain changes. New string resources (both locales):
  - `today_button` = "Today" / "Hari Ini"
  - `hijri_header_prefix` = "We are in %1$s" / "Kita berada di %1$s"
  - `hijri_year_suffix` = "AH" / "H"
  - `no_events_on_day` = "No events on this day" / "Tidak ada acara pada hari ini"
  - string-array `weekdays_short` = Sun..Sat / Min..Sab (Sunday-first)
- Existing strings reused: `islamic_calendar`, `back`, `todays_events`. `no_events_this_month` becomes unused — leave it in place (harmless) to avoid unrelated churn.

- [ ] **Step 1: Add string resources (values)**

In `app/src/main/res/values/strings.xml`, inside the `<!-- Calendar -->` section (after `<string name="no_events_this_month">`), add:

```xml
    <string name="today_button">Today</string>
    <string name="hijri_header_prefix">We are in %1$s</string>
    <string name="hijri_year_suffix">AH</string>
    <string name="no_events_on_day">No events on this day</string>
    <string-array name="weekdays_short">
        <item>Sun</item>
        <item>Mon</item>
        <item>Tue</item>
        <item>Wed</item>
        <item>Thu</item>
        <item>Fri</item>
        <item>Sat</item>
    </string-array>
```

- [ ] **Step 2: Add string resources (values-id)**

In `app/src/main/res/values-id/strings.xml`, inside the `<!-- Calendar -->` section, add:

```xml
    <string name="today_button">Hari Ini</string>
    <string name="hijri_header_prefix">Kita berada di %1$s</string>
    <string name="hijri_year_suffix">H</string>
    <string name="no_events_on_day">Tidak ada acara pada hari ini</string>
    <string-array name="weekdays_short">
        <item>Min</item>
        <item>Sen</item>
        <item>Sel</item>
        <item>Rab</item>
        <item>Kam</item>
        <item>Jum</item>
        <item>Sab</item>
    </string-array>
```

- [ ] **Step 3: Rewrite the screen**

Replace the entire contents of `app/src/main/java/com/smiledev/rafiq/ui/calendar/IslamicCalendarScreen.kt`:

```kotlin
package com.smiledev.rafiq.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smiledev.rafiq.R
import com.smiledev.rafiq.core.displayMessage
import com.smiledev.rafiq.domain.model.IslamicEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicCalendarScreen(
    onBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val weekdays = remember { stringArrayResource(R.array.weekdays_short) }
    val hijriSuffix = stringResource(R.string.hijri_year_suffix)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.islamic_calendar)) },
                navigationIcon = {
                    Text(stringResource(R.string.back), modifier = Modifier.clickable(onClick = onBack).padding(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize().padding(padding).semantics { contentDescription = "Loading" }
                )
            }
            state.error != null -> {
                Text(
                    text = state.error?.displayMessage ?: "",
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (state.todayEvents.isNotEmpty()) {
                        TodayEventsCard(events = state.todayEvents, localeCode = viewModel.localeCode)
                    }
                    HijriHeaderBanner(grid = state.grid, viewModel = viewModel, hijriSuffix = hijriSuffix)
                    MonthNavigator(state = state, viewModel = viewModel)
                    WeekdayHeader(weekdays = weekdays)
                    CalendarGrid(
                        grid = state.grid,
                        selectedIndex = state.selectedIndex,
                        onDayClick = viewModel::onDayClick
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    val selectedDay = state.selectedIndex?.let { state.grid.getOrNull(it) }
    if (selectedDay != null) {
        DayDetailSheet(
            day = selectedDay,
            displayedYear = state.displayedYear,
            displayedMonth = state.displayedMonth,
            viewModel = viewModel,
            hijriSuffix = hijriSuffix,
            onDismiss = viewModel::dismissDaySheet
        )
    }
}

@Composable
private fun TodayEventsCard(events: List<IslamicEvent>, localeCode: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "\uD83D\uDCC5", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.todays_events),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00695C)
                )
            }
            Spacer(Modifier.height(8.dp))
            events.forEach { event ->
                Text(
                    text = if (localeCode == "id") event.titleId else event.titleEn,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun HijriHeaderBanner(
    grid: List<CalendarDay?>,
    viewModel: CalendarViewModel,
    hijriSuffix: String
) {
    val first = grid.firstOrNull { it != null }?.hijriDate
    val last = grid.lastOrNull { it != null }?.hijriDate
    if (first == null || last == null) return
    val monthName = { month: Int -> viewModel.getMonthName(month) }
    val headerText = if (first.month == last.month && first.year == last.year) {
        stringResource(R.string.hijri_header_prefix, "${monthName(first.month)} ${first.year} $hijriSuffix")
    } else {
        stringResource(
            R.string.hijri_header_prefix,
            "${monthName(first.month)} \u2013 ${monthName(last.month)} ${last.year} $hijriSuffix"
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF009688))
    ) {
        Text(
            text = headerText,
            modifier = Modifier.padding(16.dp),
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MonthNavigator(state: CalendarUiState, viewModel: CalendarViewModel) {
    val label = remember(state.displayedYear, state.displayedMonth) {
        val cal = Calendar.getInstance().apply {
            clear()
            set(state.displayedYear, state.displayedMonth - 1, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "\u2039",
            fontSize = 28.sp,
            modifier = Modifier
                .clickable(onClick = viewModel::previousMonth)
                .padding(8.dp)
                .semantics { contentDescription = "Previous month" }
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                text = stringResource(R.string.today_button),
                color = Color(0xFF009688),
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable(onClick = viewModel::goToToday)
                    .padding(vertical = 4.dp)
            )
        }
        Text(
            text = "\u203A",
            fontSize = 28.sp,
            modifier = Modifier
                .clickable(onClick = viewModel::nextMonth)
                .padding(8.dp)
                .semantics { contentDescription = "Next month" }
        )
    }
}

@Composable
private fun WeekdayHeader(weekdays: Array<String>) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        weekdays.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    grid: List<CalendarDay?>,
    selectedIndex: Int?,
    onDayClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    DayCell(
                        day = grid.getOrNull(index),
                        selected = index == selectedIndex,
                        onClick = { onDayClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay?,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (day == null) {
        Box(modifier = Modifier.weight(1f).height(58.dp))
        return
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .height(58.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    day.isToday -> Color(0xFFE0F2F1)
                    selected -> Color(0xFFB2DFDB)
                    else -> Color.Transparent
                }
            )
            .border(if (selected) 1.dp else 0.dp, Color(0xFF009688), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = day.gregorianDay.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = day.hijriDate.day.toString(),
            fontSize = 10.sp,
            color = Color.Gray
        )
        if (day.events.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.Center) {
                day.events.take(3).forEach { event ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (event.eventType == "holiday") Color(0xFFB8860B) else Color(0xFF009688)
                            )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(
    day: CalendarDay,
    displayedYear: Int,
    displayedMonth: Int,
    viewModel: CalendarViewModel,
    hijriSuffix: String,
    onDismiss: () -> Unit
) {
    val hijri = day.hijriDate
    val gregorianLabel = remember(displayedYear, displayedMonth, day.gregorianDay) {
        val cal = Calendar.getInstance().apply {
            clear()
            set(displayedYear, displayedMonth - 1, day.gregorianDay)
        }
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(cal.time)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = gregorianLabel, fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${viewModel.getMonthName(hijri.month)} ${hijri.day}, ${hijri.year} $hijriSuffix",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF009688)
            )
            Spacer(Modifier.height(16.dp))
            if (day.events.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_events_on_day),
                    color = Color.Gray
                )
            } else {
                day.events.forEach { event ->
                    Text(
                        text = if (viewModel.localeCode == "id") event.titleId else event.titleEn,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (viewModel.localeCode == "id") event.descriptionId else event.descriptionEn,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
```

Note: the code above never references `HijriDate` by name in a type position — it only reads `day.hijriDate` properties. Omit `import com.smiledev.rafiq.domain.model.HijriDate` from the file. `IslamicEvent` IS referenced (the `TodayEventsCard(events: List<IslamicEvent>, ...)` signature), so keep that import. The compiler will flag any unused import as an error in this project.

- [ ] **Step 4: Verify the screen compiles (build app)**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; cmd /c "gradlew.bat :app:compileDebugKotlin"`
Expected: BUILD SUCCESSFUL (compiles the rewritten screen + ViewModel + strings).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq/ui/calendar/IslamicCalendarScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-id/strings.xml
git commit -m "feat: render monthly hijri grid with event dots and day sheet"
```

---

### Task 6: Full verification

**Files:**
- No source changes. Run the full unit-test suite + build as the final gate.

- [ ] **Step 1: Run the complete unit test suite**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; cmd /c "gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug"`
Expected: BUILD SUCCESSFUL — all of: `:domain` converter tests, `:data` repository tests (4 tests incl. 57-event parse), `:app` `CalendarViewModelTest` (6 tests), plus the APK assembles.

- [ ] **Step 2: Confirm git state is clean of unintended changes**

Run: `git status --short`
Expected: only the intended files from Tasks 1–5 are present/committed; the main repo's unrelated mosque files and README commits were never touched (we are isolated in the worktree).

- [ ] **Step 3: Report success criteria**

Confirm each of the following, citing the passing test names:
1. `HijriDateConverterTest` — anchors, 30-day-month (day-30) mapping, weekday offsets, month lengths/leap years, round trips.
2. `IslamicCalendarRepositoryImplTest` — today's events match by Hijri date, empty-without-fallback, error on missing asset, 57-event full parse.
3. `CalendarViewModelTest` — 42-cell grid, Ashura dot on 5 July 2025, today marker, month navigation + year rollover, goToToday, day selection/dismiss, event-dot placement in March 2026.
4. `:app:assembleDebug` builds the APK with the new screen.

---
