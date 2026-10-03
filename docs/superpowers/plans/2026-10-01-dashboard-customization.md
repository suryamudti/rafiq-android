# Dashboard Customization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users hide/show any dashboard shortcut from Settings, persisted across restarts, all visible by default.

**Architecture:** One new `stringSet` DataStore key holds hidden shortcut keys. `DashboardViewModel` sanitizes the set against a known-keys allowlist and exposes it in UI state; `DashboardScreen` filters the existing `quickServices` list. `SettingsScreen` renders 14 `Switch` rows bound to the same key.

**Tech Stack:** Kotlin 2.1.20, Jetpack Compose (BOM 2026.03.01), DataStore Preferences 1.1.1, Hilt 2.56.2 (KAPT), JUnit4 + mockk + kotlinx-coroutines-test, Robolectric not needed here.

## Global Constraints

- Temurin JDK 17 for every Gradle invocation: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"`.
- Never add `material-icons-extended` dependency (core icons only).
- Never replace KAPT with KSP.
- All user prefs via `PreferencesManager` (DataStore), never SharedPreferences.
- Never use Java `Math.*` — use `kotlin.math.*`.
- Cross-module nullable access: use `!!` or local `val`, never rely on smart-cast from another module's property.
- Unit tests run with `.\gradlew testDebug`; emulator is `Medium_Phone_API_35`.
- Every screen keeps the signature shape `(onBack: () -> Unit, viewModel = hiltViewModel(), modifier: Modifier = Modifier)` and back nav `{ backStack.removeLastOrNull() }`.

---

## File Structure

- Modify `data/src/main/kotlin/com/smiledev/rafiq_quran/data/preferences/PreferencesManager.kt` — new `HIDDEN_DASHBOARD_KEYS` key, `hiddenDashboardKeys` Flow, `setDashboardHidden()`, `toggleDashboardKey()`.
- Modify `app/src/main/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardViewModel.kt` — `KNOWN_DASHBOARD_KEYS` allowlist (15 entries, includes future `hifz`), `sanitizeHiddenKeys()`, `hiddenKeys` in `DashboardUiState`, collect the new Flow in `init`.
- Modify `app/src/main/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardScreen.kt` — `QuickServiceItem` becomes `internal` with a `key` field, all 14 entries gain keys, screen filters by `state.hiddenKeys`, empty-state fallback when all are hidden.
- Modify `app/src/main/java/com/smiledev/rafiq_quran/ui/settings/SettingsViewModel.kt` — `hiddenKeys` in state, `toggleDashboardKey()`, `resetDashboard()`.
- Modify `app/src/main/java/com/smiledev/rafiq_quran/ui/settings/SettingsScreen.kt` — "Dashboard customization" section with 14 `Switch` rows + reset button.
- Create `app/src/test/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardVisibilityTest.kt` — pure allowlist/filter tests.
- Modify `app/src/test/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardViewModelTest.kt` — stub the new Flow, assert sanitized keys land in state.
- Create `app/src/test/java/com/smiledev/rafiq_quran/ui/settings/SettingsViewModelTest.kt` — state + delegation tests.
- Modify `app/src/main/res/values/strings.xml`, `values-in/strings.xml`, `values-id/strings.xml` — 3 new strings (section title, reset, empty-state). Verify each file contains `quick_access` before editing.

**Interface contract this plan produces (consumed by the Hifz plan):**
- `PreferencesManager.hiddenDashboardKeys: Flow<Set<String>>`, `suspend fun setDashboardHidden(keys: Set<String>)`, `suspend fun toggleDashboardKey(key: String)`.
- `internal val KNOWN_DASHBOARD_KEYS: Set<String>` (15 keys, includes `"hifz"`), `internal fun sanitizeHiddenKeys(raw: Set<String>): Set<String>`.

---

### Task 1: PreferencesManager hidden-keys storage

**Files:**
- Modify: `data/src/main/kotlin/com/smiledev/rafiq_quran/data/preferences/PreferencesManager.kt`

**Interfaces:**
- Consumes: existing `context.dataStore` delegate and `stringSetPreferencesKey` import (already imported, line 8).
- Produces: `HIDDEN_DASHBOARD_KEYS`, `val hiddenDashboardKeys: Flow<Set<String>>`, `suspend fun setDashboardHidden(keys: Set<String>)`, `suspend fun toggleDashboardKey(key: String)` — used by Tasks 3 and 5.

- [ ] **Step 1: Add the key to the companion object**

After the `TASBIH_SOUND` key (lines 101-103), insert:

```kotlin
/** Key for the set of hidden dashboard shortcut keys. Empty means all visible. */
val HIDDEN_DASHBOARD_KEYS = stringSetPreferencesKey("hidden_dashboard_keys")
```

- [ ] **Step 2: Add the Flow accessor after `tasbihSoundEnabled` (ends line 280)**

```kotlin
/**
 * Returns the set of hidden dashboard shortcut keys as a [Flow].
 * Defaults to empty (all shortcuts visible) if not set.
 */
val hiddenDashboardKeys: Flow<Set<String>> = context.dataStore.data.map { prefs ->
    prefs[HIDDEN_DASHBOARD_KEYS].orEmpty()
}
```

- [ ] **Step 3: Add the two setters after `resetTasbih` (ends line 552)**

```kotlin
/**
 * Replaces the full set of hidden dashboard shortcut keys.
 *
 * @param keys the keys to hide; empty means show all
 */
suspend fun setDashboardHidden(keys: Set<String>) {
    context.dataStore.edit { prefs -> prefs[HIDDEN_DASHBOARD_KEYS] = keys }
}

/**
 * Toggles one dashboard shortcut key in the hidden set.
 * If the key is hidden it becomes visible, otherwise it becomes hidden.
 *
 * @param key the dashboard shortcut key to toggle
 */
suspend fun toggleDashboardKey(key: String) {
    context.dataStore.edit { prefs ->
        val current = prefs[HIDDEN_DASHBOARD_KEYS].orEmpty()
        prefs[HIDDEN_DASHBOARD_KEYS] = if (key in current) current - key else current + key
    }
}
```

- [ ] **Step 4: Compile the data module and run its existing tests**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew :data:testDebug`
Expected: BUILD SUCCESSFUL (no new unit test here — DataStore needs a device/context; behavior is covered by the ViewModel tests in Tasks 3 and 5 plus the Task 6 device smoke test).

- [ ] **Step 5: Commit**

```bash
git add data/src/main/kotlin/com/smiledev/rafiq_quran/data/preferences/PreferencesManager.kt
git commit -m "feat: add hidden dashboard keys to PreferencesManager"
```

---

### Task 2: Known-keys allowlist + sanitizer with tests (TDD)

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardViewModel.kt`
- Create: `app/src/test/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardVisibilityTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `internal val KNOWN_DASHBOARD_KEYS: Set<String>`, `internal fun sanitizeHiddenKeys(raw: Set<String>): Set<String>` — used by Tasks 3 and 4.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardVisibilityTest.kt`:

```kotlin
package com.smiledev.rafiq_quran.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardVisibilityTest {

    @Test
    fun `empty set stays empty`() {
        assertEquals(emptySet<String>(), sanitizeHiddenKeys(emptySet()))
    }

    @Test
    fun `known keys pass through`() {
        val input = setOf("quran", "zakat", "prayer_log")
        assertEquals(input, sanitizeHiddenKeys(input))
    }

    @Test
    fun `unknown keys are dropped`() {
        assertEquals(setOf("quran"), sanitizeHiddenKeys(setOf("quran", "bogus-key")))
    }

    @Test
    fun `known keys cover all 15 shortcuts including future hifz`() {
        assertEquals(15, KNOWN_DASHBOARD_KEYS.size)
        assertTrue("hifz" in KNOWN_DASHBOARD_KEYS)
        assertTrue("prayer_guidance" in KNOWN_DASHBOARD_KEYS)
        assertTrue("sunnah_guidance" in KNOWN_DASHBOARD_KEYS)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq_quran.ui.dashboard.DashboardVisibilityTest"`
Expected: FAIL with unresolved reference to `sanitizeHiddenKeys` / `KNOWN_DASHBOARD_KEYS`.

- [ ] **Step 3: Add the allowlist and sanitizer to DashboardViewModel.kt**

Insert before `data class DashboardUiState` (line 35):

```kotlin
internal val KNOWN_DASHBOARD_KEYS: Set<String> = setOf(
    "quran",
    "prayer_times",
    "qibla",
    "tasbih",
    "hadith",
    "prophets",
    "asmaul_husna",
    "recitation",
    "mosques",
    "calendar",
    "zakat",
    "prayer_log",
    "prayer_guidance",
    "sunnah_guidance",
    // "hifz" card lands with the Hifz plan; listed now so stored prefs stay forward-compatible.
    "hifz"
)

internal fun sanitizeHiddenKeys(raw: Set<String>): Set<String> = raw.intersect(KNOWN_DASHBOARD_KEYS)
```

- [ ] **Step 4: Run the test to verify it passes**

Run: same `--tests` command as Step 2.
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardViewModel.kt app/src/test/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardVisibilityTest.kt
git commit -m "feat: add dashboard known-keys allowlist and sanitizer"
```

---

### Task 3: DashboardViewModel exposes hidden keys

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardViewModel.kt`
- Modify: `app/src/test/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardViewModelTest.kt`

**Interfaces:**
- Consumes: `PreferencesManager.hiddenDashboardKeys` (Task 1), `sanitizeHiddenKeys` (Task 2).
- Produces: `DashboardUiState.hiddenKeys: Set<String>` — used by Task 4.

- [ ] **Step 1: Add the state field**

In `DashboardUiState`, after `todayCompletedPrayersCount: Int = 0` (line 64), add:

```kotlin
val hiddenKeys: Set<String> = emptySet()
```

- [ ] **Step 2: Collect the new Flow in `init`**

Read the `init` block (starts line 177) to find the existing `combine(...)` over preference Flows. Add `preferencesManager.hiddenDashboardKeys` as one more combined Flow, name its lambda parameter `hidden`, and extend the existing state `copy(...)` with:

```kotlin
hiddenKeys = sanitizeHiddenKeys(hidden)
```

- [ ] **Step 3: Extend the existing test**

In `DashboardViewModelTest.kt`, add next to the other Flow stubs (after line 56):

```kotlin
private val hiddenKeysFlow = MutableStateFlow<Set<String>>(emptySet())
```

In `setUp()` (after line 66) add:

```kotlin
every { preferencesManager.hiddenDashboardKeys } returns hiddenKeysFlow
```

Append this test (reuse the file's existing `DashboardViewModel(...)` construction argument list verbatim):

```kotlin
@Test
fun `hidden dashboard keys flow into ui state sanitized`() = runTest(testDispatcher) {
    hiddenKeysFlow.value = setOf("zakat", "bogus-key")

    val vm = DashboardViewModel(
        prayerTimesRepository = prayerTimesRepository,
        preferencesManager = preferencesManager,
        context = context,
        dispatcherProvider = testDispatcherProvider,
        quranRepository = quranRepository,
        prayerLogRepository = prayerLogRepository,
        hadithRepository = hadithRepository
    )
    advanceUntilIdle()

    assertEquals(setOf("zakat"), vm.uiState.value.hiddenKeys)
}
```

If the existing construction in this file passes different arguments, use that exact argument list instead and keep the two added lines (`hiddenKeysFlow.value = ...`, `assertEquals(...)`) unchanged.

- [ ] **Step 4: Run the dashboard tests**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq_quran.ui.dashboard.*"`
Expected: BUILD SUCCESSFUL, all dashboard tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardViewModel.kt app/src/test/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardViewModelTest.kt
git commit -m "feat: expose sanitized hidden dashboard keys in DashboardUiState"
```

---

### Task 4: DashboardScreen filters shortcuts + empty state

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-in/strings.xml`, `app/src/main/res/values-id/strings.xml`

**Interfaces:**
- Consumes: `DashboardUiState.hiddenKeys` (Task 3). The `Settings` NavKey import already exists (line 73).
- Produces: visible filtered grid; nothing downstream.

- [ ] **Step 1: Confirm the three strings files**

Run: `Select-String -Path "C:\Flutter\rafiq-app-android\app\src\main\res\values\strings.xml", "C:\Flutter\rafiq-app-android\app\src\main\res\values-in\strings.xml", "C:\Flutter\rafiq-app-android\app\src\main\res\values-id\strings.xml" -Pattern "quick_access"`
Expected: one match per file. If any file is missing the key, add the new strings only to the files that exist and note the gap in the commit message.

- [ ] **Step 2: Make `QuickServiceItem` keyed**

Change line 80 from `private data class QuickServiceItem(` to:

```kotlin
internal data class QuickServiceItem(
    val key: String,
    val labelResId: Int,
    val navKey: NavKey,
    val iconResId: Int,
    val tintColor: Color,
    val containerColor: Color
)
```

Add `key = "..."` as the first named argument of each of the 14 entries, in order: `"quran"`, `"prayer_times"`, `"qibla"`, `"tasbih"`, `"hadith"`, `"prophets"`, `"asmaul_husna"`, `"recitation"`, `"mosques"`, `"calendar"`, `"zakat"`, `"prayer_log"`, `"prayer_guidance"`, `"sunnah_guidance"`.

- [ ] **Step 3: Filter in `DashboardScreen` and handle hide-all**

Replace `items = quickServices,` in the `QuickServicesGrid` call (line 254) with a filtered list computed just above the call:

```kotlin
val visibleServices = quickServices.filter { it.key !in state.hiddenKeys }
```

and pass `items = visibleServices`. Wrap the grid call:

```kotlin
if (visibleServices.isEmpty()) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.dashboard_empty_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { onNavigate(Settings) }) {
                Text(
                    text = stringResource(R.string.dashboard_empty_action),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
} else {
    QuickServicesGrid(
        items = visibleServices,
        onServiceClick = { onNavigate(it) }
    )
}
```

All symbols used (`Card`, `CardDefaults`, `RoundedCornerShape`, `TextButton`, `FontWeight`, `Spacer`, `height`, `padding`, `Alignment`) are already imported in this file.

- [ ] **Step 4: Add the two strings**

In each confirmed strings file, add:

```xml
<string name="dashboard_empty_title">No shortcuts shown</string>
<string name="dashboard_empty_action">Customize dashboard</string>
```

with Indonesian translations in `values-in`/`values-id` (`"Tidak ada pintasan yang ditampilkan"`, `"Sesuaikan dasbor"`).

- [ ] **Step 5: Build the app**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq_quran/ui/dashboard/DashboardScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-in/strings.xml app/src/main/res/values-id/strings.xml
git commit -m "feat: filter dashboard shortcuts by hidden keys with empty state"
```

---

### Task 5: Settings toggles (ViewModel test-first, then UI)

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/ui/settings/SettingsViewModel.kt`
- Create: `app/src/test/java/com/smiledev/rafiq_quran/ui/settings/SettingsViewModelTest.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/ui/settings/SettingsScreen.kt`
- Modify: the confirmed strings files from Task 4.

**Interfaces:**
- Consumes: `hiddenDashboardKeys`, `toggleDashboardKey`, `setDashboardHidden` (Task 1); existing `R.string.*` label IDs (same IDs used by `quickServices`: `quran`, `prayer_times`, `qibla`, `tasbih`, `hadiths`, `prophets`, `asmaul_husna`, `recitations`, `mosques`, `calendar`, `zakat`, `prayer_log`, `prayer_guidance`, `sunnah_guidance`).
- Produces: user-facing toggles; nothing downstream.

- [ ] **Step 1: Write the failing ViewModel test**

Create `app/src/test/java/com/smiledev/rafiq_quran/ui/settings/SettingsViewModelTest.kt`:

```kotlin
package com.smiledev.rafiq_quran.ui.settings

import com.smiledev.rafiq_quran.TestDispatcherProvider
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testDispatcherProvider = TestDispatcherProvider(testDispatcher)
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)

    private val themeFlow = MutableStateFlow("system")
    private val langFlow = MutableStateFlow("system")
    private val hiddenFlow = MutableStateFlow(setOf("zakat"))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { preferencesManager.themeMode } returns themeFlow
        every { preferencesManager.translationLanguage } returns langFlow
        every { preferencesManager.hiddenDashboardKeys } returns hiddenFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `hidden keys appear in ui state`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(preferencesManager, testDispatcherProvider)
        advanceUntilIdle()

        assertEquals(setOf("zakat"), vm.uiState.value.hiddenKeys)
    }

    @Test
    fun `toggle delegates to preferences`() = runTest(testDispatcher) {
        coEvery { preferencesManager.toggleDashboardKey(any()) } returns Unit

        val vm = SettingsViewModel(preferencesManager, testDispatcherProvider)
        advanceUntilIdle()

        vm.toggleDashboardKey("qibla")
        advanceUntilIdle()

        coVerify { preferencesManager.toggleDashboardKey("qibla") }
    }

    @Test
    fun `reset clears hidden set`() = runTest(testDispatcher) {
        coEvery { preferencesManager.setDashboardHidden(any()) } returns Unit

        val vm = SettingsViewModel(preferencesManager, testDispatcherProvider)
        advanceUntilIdle()

        vm.resetDashboard()
        advanceUntilIdle()

        coVerify { preferencesManager.setDashboardHidden(emptySet()) }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq_quran.ui.settings.SettingsViewModelTest"`
Expected: FAIL with unresolved reference to `hiddenKeys` / `toggleDashboardKey` / `resetDashboard`.

- [ ] **Step 3: Extend `SettingsViewModel`**

Add to `SettingsUiState`:

```kotlin
val hiddenKeys: Set<String> = emptySet()
```

Extend the `combine(...)` in `init` with `preferencesManager.hiddenDashboardKeys` (third Flow, lambda parameter `hidden`) and copy `hiddenKeys = hidden` into state. Add:

```kotlin
fun toggleDashboardKey(key: String) {
    viewModelScope.launch(dispatcherProvider.io) {
        preferencesManager.toggleDashboardKey(key)
    }
}

fun resetDashboard() {
    viewModelScope.launch(dispatcherProvider.io) {
        preferencesManager.setDashboardHidden(emptySet())
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: same `--tests` command as Step 2.
Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 5: Add the Settings UI section**

In `SettingsScreen.kt`, after the translation-language block and before the first `HorizontalDivider` (lines 99-101), insert:

```kotlin
Spacer(modifier = Modifier.height(24.dp))

Text(
    text = stringResource(R.string.dashboard_customize_title),
    style = MaterialTheme.typography.titleMedium,
    modifier = Modifier.padding(bottom = 4.dp)
)

dashboardToggleItems.forEach { (key, labelResId) ->
    DashboardToggleRow(
        labelResId = labelResId,
        checked = key !in state.hiddenKeys,
        onCheckedChange = { viewModel.toggleDashboardKey(key) }
    )
}

Text(
    text = stringResource(R.string.reset_defaults),
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.Bold,
    modifier = Modifier.clickable { viewModel.resetDashboard() }.padding(vertical = 12.dp, horizontal = 8.dp)
)
```

Add at file bottom:

```kotlin
private val dashboardToggleItems: List<Pair<String, Int>> = listOf(
    "quran" to R.string.quran,
    "prayer_times" to R.string.prayer_times,
    "qibla" to R.string.qibla,
    "tasbih" to R.string.tasbih,
    "hadith" to R.string.hadiths,
    "prophets" to R.string.prophets,
    "asmaul_husna" to R.string.asmaul_husna,
    "recitation" to R.string.recitations,
    "mosques" to R.string.mosques,
    "calendar" to R.string.calendar,
    "zakat" to R.string.zakat,
    "prayer_log" to R.string.prayer_log,
    "prayer_guidance" to R.string.prayer_guidance,
    "sunnah_guidance" to R.string.sunnah_guidance
)

@Composable
private fun DashboardToggleRow(
    labelResId: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(labelResId),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
```

Add the `androidx.compose.material3.Switch` import. All other symbols (`Row`, `fillMaxWidth`, `padding`, `Alignment`, `weight`, `stringResource`, `FontWeight`, `clickable`) are already imported or available.

- [ ] **Step 6: Add the two strings to each confirmed strings file**

```xml
<string name="dashboard_customize_title">Dashboard shortcuts</string>
<string name="reset_defaults">Reset to defaults</string>
```

Indonesian: `"Pintasan dasbor"`, `"Kembalikan ke bawaan"`.

- [ ] **Step 7: Build and run the settings tests**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew :app:assembleDebug :app:testDebugUnitTest --tests "com.smiledev.rafiq_quran.ui.settings.*"`
Expected: BUILD SUCCESSFUL. (Gradle runs both tasks; the `--tests` filter applies to the test task.)

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq_quran/ui/settings/ app/src/test/java/com/smiledev/rafiq_quran/ui/settings/ app/src/main/res/values/strings.xml app/src/main/res/values-in/strings.xml app/src/main/res/values-id/strings.xml
git commit -m "feat: add dashboard shortcut toggles to Settings"
```

---

### Task 6: Full verification + device smoke test

**Files:** none (verification only; fix commits if issues surface).

- [ ] **Step 1: Run the whole JVM unit suite**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew testDebug`
Expected: BUILD SUCCESSFUL, zero failures.

- [ ] **Step 2: Install on the emulator and smoke-test**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew :app:assembleDebug; adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk`
Then on the `Medium_Phone_API_35` emulator, in both English and Indonesian locales:
1. Dashboard shows all 14 shortcuts on fresh install.
2. Settings → toggle Zakat off → back → Zakat card gone, other cards keep order.
3. Kill and restart the app → Zakat still hidden (persistence).
4. Hide all 14 → dashboard shows the "No shortcuts shown" card → tap "Customize dashboard" → lands in Settings.
5. "Reset to defaults" → all 14 return.
6. Toggle dark mode → switches still legible (no new colors were introduced, so this is a no-change check).

- [ ] **Step 3: Commit any fixes as separate commits**

Each fix: `git add <exact files>; git commit -m "fix: <what>"`. If no fixes were needed, no commit for this task.
