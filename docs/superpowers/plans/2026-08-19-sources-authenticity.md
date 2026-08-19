# Sources & Authenticity Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a static "Sources & Authenticity" screen reachable from Settings that documents the provenance of the Quran, Hadith, and Prayer Times sources, with authenticity labels and links to the original websites.

**Architecture:** A new `ui/sources/` package in `:app` with a small content catalog (`SourcesCatalog.kt` — pure data, unit-testable without an emulator) and a composable screen (`SourcesScreen.kt`) that renders it. Navigation follows the existing Navigation3 pattern: a new `@Serializable Sources` NavKey in `NavigationKeys.kt`, an `entry<Sources>` block in `Navigation.kt`, and a row in `SettingsScreen.kt` reusing the `MoreFeatureItem` composable. All strings are resource-backed in EN + ID.

**Tech Stack:** Kotlin, Jetpack Compose + Material3, Navigation3, Hilt (not involved here), JUnit4 + MockK for unit tests, Compose UI test for instrumented tests.

## Global Constraints

- **JAVA_HOME for all gradle commands:** `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`. Run `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"` first. Android Studio's bundled jbr is incomplete on this machine.
- **Build verification:** `.\gradlew assembleDebug`
- **Unit tests:** `.\gradlew testDebug`
- **Instrumented tests** (emulator `Medium_Phone_API_35` required): `.\gradlew connectedDebugAndroidTest`
- **Material Icons: core only** — never import `material-icons-extended`.
- **Compose `textDirection`** is a `TextStyle` property, not a `Text` param.
- **Cross-module smart casts from nullable don't work** — not relevant here (all code in `:app`), but do not rely on them anyway.
- **Strings must be added in BOTH `values/strings.xml` (EN) and `values-id/strings.xml` (ID)** — they are parallel files; every key must exist in both.
- **XML escaping:** `&` must be written as `&amp;` in Android string resources.

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `app/src/main/res/values/strings.xml` | EN strings for the new feature | Modify |
| `app/src/main/res/values-id/strings.xml` | ID strings for the new feature | Modify |
| `app/src/main/java/com/smiledev/rafiq_quran/ui/sources/SourcesCatalog.kt` | `SourceSection`/`SourceItem` data classes + `sourcesSections()` | Create |
| `app/src/main/java/com/smiledev/rafiq_quran/ui/sources/SourcesScreen.kt` | Composable screen rendering the catalog | Create |
| `app/src/main/java/com/smiledev/rafiq_quran/NavigationKeys.kt` | Add `Sources` NavKey | Modify |
| `app/src/main/java/com/smiledev/rafiq_quran/Navigation.kt` | Add `entry<Sources>` | Modify |
| `app/src/main/java/com/smiledev/rafiq_quran/ui/settings/SettingsScreen.kt` | Add "Sources & Authenticity" row | Modify |
| `app/src/test/java/com/smiledev/rafiq_quran/ui/sources/SourcesCatalogTest.kt` | JVM unit tests for the catalog | Create |
| `app/src/androidTest/java/com/smiledev/rafiq_quran/ui/sources/SourcesScreenTest.kt` | Instrumented test for the screen | Create |
| `app/src/androidTest/java/com/smiledev/rafiq_quran/ui/settings/SettingsScreenTest.kt` | Add test that the Settings row is displayed | Modify |

---

### Task 1: Add EN and ID string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-id/strings.xml`

**Interfaces:**
- Produces: resource IDs used by Tasks 2-4 — `R.string.sources_and_authenticity`, `R.string.source_section_quran`, `R.string.source_section_hadith`, `R.string.source_section_prayer_times`, `R.string.source_quran_uthmani`, `R.string.source_quran_uthmani_desc`, `R.string.source_quran_en`, `R.string.source_quran_en_desc`, `R.string.source_quran_id`, `R.string.source_quran_id_desc`, `R.string.source_hadith_bukhari`, `R.string.source_hadith_bukhari_desc`, `R.string.source_hadith_muslim`, `R.string.source_hadith_muslim_desc`, `R.string.source_prayer_aladhan`, `R.string.source_prayer_aladhan_desc`, `R.string.source_prayer_location`, `R.string.source_prayer_location_desc`, `R.string.source_authentic_text`, `R.string.source_recognized_translation`, `R.string.source_authentic_collection`, `R.string.source_kemenag_method`, `R.string.source_default_jakarta`, `R.string.source_translator`, `R.string.source_translator_saheeh`, `R.string.source_translator_kemenag`, `R.string.source_translator_muhsin_khan`, `R.string.source_translator_siddiqui`, `R.string.open_source`, `R.string.link_error`.

- [ ] **Step 1: Add the strings to `app/src/main/res/values/strings.xml`**

Insert before the closing `</resources>` tag:

```xml
    <!-- Sources & Authenticity -->
    <string name="sources_and_authenticity">Sources &amp; Authenticity</string>
    <string name="source_section_quran">Quran</string>
    <string name="source_section_hadith">Hadith</string>
    <string name="source_section_prayer_times">Prayer Times</string>
    <string name="source_quran_uthmani">Quran Text (Uthmani)</string>
    <string name="source_quran_uthmani_desc">Standard Uthmani Arabic script, bundled with the app.</string>
    <string name="source_quran_en">English Translation</string>
    <string name="source_quran_en_desc">Saheeh International translation, bundled with the app.</string>
    <string name="source_quran_id">Indonesian Translation</string>
    <string name="source_quran_id_desc">Official translation by the Indonesian Ministry of Religious Affairs (Kemenag), bundled with the app.</string>
    <string name="source_hadith_bukhari">Sahih al-Bukhari</string>
    <string name="source_hadith_bukhari_desc">Authentic collection by Imam al-Bukhari. Arabic with Muhsin Khan English translation, bundled with the app.</string>
    <string name="source_hadith_muslim">Sahih Muslim</string>
    <string name="source_hadith_muslim_desc">Authentic collection by Imam Muslim. Arabic with Abdul Hamid Siddiqui English translation, bundled with the app.</string>
    <string name="source_prayer_aladhan">Prayer Times API</string>
    <string name="source_prayer_aladhan_desc">Calculated by the Aladhan API with KEMENAG calculation method 20.</string>
    <string name="source_prayer_location">Default Location</string>
    <string name="source_prayer_location_desc">Jakarta (-6.2088, 106.8456) is used when no location is saved.</string>
    <string name="source_authentic_text">Authentic Quranic text</string>
    <string name="source_recognized_translation">Recognized translation</string>
    <string name="source_authentic_collection">Authentic collection (Shahih)</string>
    <string name="source_kemenag_method">KEMENAG calculation method (20)</string>
    <string name="source_default_jakarta">Default: Jakarta</string>
    <string name="source_translator">Translator: %s</string>
    <string name="source_translator_saheeh">Saheeh International</string>
    <string name="source_translator_kemenag">Indonesian Ministry of Religious Affairs (Kemenag)</string>
    <string name="source_translator_muhsin_khan">Muhsin Khan</string>
    <string name="source_translator_siddiqui">Abdul Hamid Siddiqui</string>
    <string name="open_source">Open source</string>
    <string name="link_error">Could not open link</string>
```

- [ ] **Step 2: Add the matching Indonesian strings to `app/src/main/res/values-id/strings.xml`**

Insert before the closing `</resources>` tag:

```xml
    <!-- Sumber & Keaslian -->
    <string name="sources_and_authenticity">Sumber &amp; Keaslian</string>
    <string name="source_section_quran">Al-Quran</string>
    <string name="source_section_hadith">Hadis</string>
    <string name="source_section_prayer_times">Waktu Shalat</string>
    <string name="source_quran_uthmani">Teks Al-Quran (Utsmani)</string>
    <string name="source_quran_uthmani_desc">Teks Arab standar Utsmani, disertakan dalam aplikasi.</string>
    <string name="source_quran_en">Terjemahan Bahasa Inggris</string>
    <string name="source_quran_en_desc">Terjemahan Saheeh International, disertakan dalam aplikasi.</string>
    <string name="source_quran_id">Terjemahan Bahasa Indonesia</string>
    <string name="source_quran_id_desc">Terjemahan resmi Kementerian Agama RI (Kemenag), disertakan dalam aplikasi.</string>
    <string name="source_hadith_bukhari">Shahih al-Bukhari</string>
    <string name="source_hadith_bukhari_desc">Koleksi shahih oleh Imam al-Bukhari. Bahasa Arab dengan terjemahan Inggris Muhsin Khan, disertakan dalam aplikasi.</string>
    <string name="source_hadith_muslim">Shahih Muslim</string>
    <string name="source_hadith_muslim_desc">Koleksi shahih oleh Imam Muslim. Bahasa Arab dengan terjemahan Inggris Abdul Hamid Siddiqui, disertakan dalam aplikasi.</string>
    <string name="source_prayer_aladhan">API Waktu Shalat</string>
    <string name="source_prayer_aladhan_desc">Dihitung oleh API Aladhan dengan metode perhitungan KEMENAG 20.</string>
    <string name="source_prayer_location">Lokasi Default</string>
    <string name="source_prayer_location_desc">Jakarta (-6.2088, 106.8456) digunakan saat belum ada lokasi yang disimpan.</string>
    <string name="source_authentic_text">Teks Al-Quran yang autentik</string>
    <string name="source_recognized_translation">Terjemahan yang diakui</string>
    <string name="source_authentic_collection">Koleksi shahih</string>
    <string name="source_kemenag_method">Metode perhitungan KEMENAG (20)</string>
    <string name="source_default_jakarta">Default: Jakarta</string>
    <string name="source_translator">Penerjemah: %s</string>
    <string name="source_translator_saheeh">Saheeh International</string>
    <string name="source_translator_kemenag">Kementerian Agama RI (Kemenag)</string>
    <string name="source_translator_muhsin_khan">Muhsin Khan</string>
    <string name="source_translator_siddiqui">Abdul Hamid Siddiqui</string>
    <string name="open_source">Buka sumber</string>
    <string name="link_error">Tidak dapat membuka tautan</string>
```

- [ ] **Step 3: Verify the resources compile**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`. (R compilation fails fast on XML errors or a duplicate/missing key pair.)

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/res/values/strings.xml app/src/main/res/values-id/strings.xml
git commit -m "feat(sources): add EN and ID strings for sources screen"
```

---

### Task 2: SourcesCatalog — content model and data

**Files:**
- Create: `app/src/main/java/com/smiledev/rafiq_quran/ui/sources/SourcesCatalog.kt`
- Test: `app/src/test/java/com/smiledev/rafiq_quran/ui/sources/SourcesCatalogTest.kt`

**Interfaces:**
- Consumes: `R.string.*` IDs defined in Task 1.
- Produces (consumed by Task 3's `SourcesScreen`):
  - `internal data class SourceItem(val titleRes: Int, val descriptionRes: Int, val authenticityRes: Int, val translatorRes: Int? = null, val linkUrl: String? = null)`
  - `internal data class SourceSection(val titleRes: Int, val items: List<SourceItem>)`
  - `internal fun sourcesSections(): List<SourceSection>` — 3 sections, 7 items: Quran (3), Hadith (2), Prayer Times (2).

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/smiledev/rafiq_quran/ui/sources/SourcesCatalogTest.kt`:

```kotlin
package com.smiledev.rafiq_quran.ui.sources

import com.smiledev.rafiq_quran.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcesCatalogTest {

    private val sections = sourcesSections()

    @Test
    fun `has exactly three sections in order`() {
        assertEquals(3, sections.size)
        assertEquals(R.string.source_section_quran, sections[0].titleRes)
        assertEquals(R.string.source_section_hadith, sections[1].titleRes)
        assertEquals(R.string.source_section_prayer_times, sections[2].titleRes)
    }

    @Test
    fun `has expected item counts per section`() {
        assertEquals(3, sections[0].items.size)
        assertEquals(2, sections[1].items.size)
        assertEquals(2, sections[2].items.size)
        assertEquals(7, sections.sumOf { it.items.size })
    }

    @Test
    fun `every item has non-zero resource ids`() {
        sections.forEach { section ->
            assertTrue("section titleRes must not be 0", section.titleRes != 0)
            section.items.forEach { item ->
                assertTrue("item titleRes must not be 0", item.titleRes != 0)
                assertTrue("item descriptionRes must not be 0", item.descriptionRes != 0)
                assertTrue("item authenticityRes must not be 0", item.authenticityRes != 0)
            }
        }
    }

    @Test
    fun `every link is https or absent`() {
        sections.forEach { section ->
            section.items.forEach { item ->
                item.linkUrl?.let { url ->
                    assertTrue("link should be https: $url", url.startsWith("https://"))
                }
            }
        }
    }

    @Test
    fun `hadith items carry authentic collection labels`() {
        val hadith = sections[1]
        assertTrue(hadith.items.all { it.authenticityRes == R.string.source_authentic_collection })
    }

    @Test
    fun `only the default location item has no link`() {
        val noLink = sections.flatMap { it.items }.filter { it.linkUrl == null }
        assertEquals(1, noLink.size)
        assertEquals(R.string.source_prayer_location, noLink[0].titleRes)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
.\gradlew testDebug
```

Expected: compile failure — `unresolved reference: sourcesSections` (SourcesCatalog.kt does not exist yet).

- [ ] **Step 3: Implement `SourcesCatalog.kt`**

Create `app/src/main/java/com/smiledev/rafiq_quran/ui/sources/SourcesCatalog.kt`:

```kotlin
package com.smiledev.rafiq_quran.ui.sources

import com.smiledev.rafiq_quran.R

internal data class SourceItem(
    val titleRes: Int,
    val descriptionRes: Int,
    val authenticityRes: Int,
    val translatorRes: Int? = null,
    val linkUrl: String? = null,
)

internal data class SourceSection(
    val titleRes: Int,
    val items: List<SourceItem>,
)

internal fun sourcesSections(): List<SourceSection> = listOf(
    SourceSection(
        titleRes = R.string.source_section_quran,
        items = listOf(
            SourceItem(
                titleRes = R.string.source_quran_uthmani,
                descriptionRes = R.string.source_quran_uthmani_desc,
                authenticityRes = R.string.source_authentic_text,
                linkUrl = "https://quran.com"
            ),
            SourceItem(
                titleRes = R.string.source_quran_en,
                descriptionRes = R.string.source_quran_en_desc,
                authenticityRes = R.string.source_recognized_translation,
                translatorRes = R.string.source_translator_saheeh,
                linkUrl = "https://quran.com"
            ),
            SourceItem(
                titleRes = R.string.source_quran_id,
                descriptionRes = R.string.source_quran_id_desc,
                authenticityRes = R.string.source_recognized_translation,
                translatorRes = R.string.source_translator_kemenag,
                linkUrl = "https://quran.kemenag.go.id"
            )
        )
    ),
    SourceSection(
        titleRes = R.string.source_section_hadith,
        items = listOf(
            SourceItem(
                titleRes = R.string.source_hadith_bukhari,
                descriptionRes = R.string.source_hadith_bukhari_desc,
                authenticityRes = R.string.source_authentic_collection,
                translatorRes = R.string.source_translator_muhsin_khan,
                linkUrl = "https://sunnah.com/bukhari"
            ),
            SourceItem(
                titleRes = R.string.source_hadith_muslim,
                descriptionRes = R.string.source_hadith_muslim_desc,
                authenticityRes = R.string.source_authentic_collection,
                translatorRes = R.string.source_translator_siddiqui,
                linkUrl = "https://sunnah.com/muslim"
            )
        )
    ),
    SourceSection(
        titleRes = R.string.source_section_prayer_times,
        items = listOf(
            SourceItem(
                titleRes = R.string.source_prayer_aladhan,
                descriptionRes = R.string.source_prayer_aladhan_desc,
                authenticityRes = R.string.source_kemenag_method,
                linkUrl = "https://aladhan.com"
            ),
            SourceItem(
                titleRes = R.string.source_prayer_location,
                descriptionRes = R.string.source_prayer_location_desc,
                authenticityRes = R.string.source_default_jakarta
            )
        )
    )
)
```

- [ ] **Step 4: Run the unit tests to verify they pass**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
.\gradlew testDebug
```

Expected: `SourcesCatalogTest` PASSES (6 tests). Run with `.\gradlew testDebug --tests "com.smiledev.rafiq_quran.ui.sources.SourcesCatalogTest"` to isolate if anything else is red.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/smiledev/rafiq_quran/ui/sources/SourcesCatalog.kt app/src/test/java/com/smiledev/rafiq_quran/ui/sources/SourcesCatalogTest.kt
git commit -m "feat(sources): add sources catalog with unit tests"
```

---

### Task 3: SourcesScreen composable

**Files:**
- Create: `app/src/main/java/com/smiledev/rafiq_quran/ui/sources/SourcesScreen.kt`

**Interfaces:**
- Consumes: `sourcesSections()`, `SourceItem`, `SourceSection` from Task 2; `R.string.*` from Task 1.
- Produces (consumed by Task 4): `@Composable fun SourcesScreen(onBack: () -> Unit, modifier: Modifier = Modifier)`.

- [ ] **Step 1: Create `SourcesScreen.kt`**

```kotlin
package com.smiledev.rafiq_quran.ui.sources

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smiledev.rafiq_quran.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sources_and_authenticity)) },
                navigationIcon = {
                    Text(
                        stringResource(R.string.back),
                        modifier = Modifier.clickable(onClick = onBack).padding(16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            sourcesSections().forEach { section ->
                SectionHeader(section.titleRes)
                section.items.forEach { item ->
                    SourceCard(
                        item = item,
                        onOpenLink = { url ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.link_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SourceCard(item: SourceItem, onOpenLink: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(item.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(item.authenticityRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = stringResource(item.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            item.translatorRes?.let { translatorRes ->
                Text(
                    text = stringResource(R.string.source_translator, stringResource(translatorRes)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            item.linkUrl?.let { url ->
                Text(
                    text = stringResource(R.string.open_source),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { onOpenLink(url) }
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`. (`SourcesScreen` is not yet reachable — that is Task 4 — so this only validates compilation.)

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/smiledev/rafiq_quran/ui/sources/SourcesScreen.kt
git commit -m "feat(sources): add sources screen composable"
```

---

### Task 4: Wire navigation — NavKey, route, and Settings entry

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/NavigationKeys.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/Navigation.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/ui/settings/SettingsScreen.kt`

**Interfaces:**
- Consumes: `SourcesScreen(onBack: () -> Unit, modifier: Modifier = Modifier)` from Task 3; `onNavigate: (NavKey) -> Unit` already on `SettingsScreen` (`SettingsScreen.kt:47`).
- Produces: `@Serializable data object Sources : NavKey`; `entry<Sources>` in `MainNavigation`; the Settings menu row.

- [ ] **Step 1: Add the NavKey**

In `app/src/main/java/com/smiledev/rafiq_quran/NavigationKeys.kt`, after the existing `@Serializable data object Settings : NavKey` (line 26), add:

```kotlin
@Serializable data object Sources : NavKey
```

- [ ] **Step 2: Register the route in `Navigation.kt`**

Add the import after `import com.smiledev.rafiq_quran.ui.settings.SettingsScreen` (line 31):

```kotlin
import com.smiledev.rafiq_quran.ui.sources.SourcesScreen
```

Add the entry after the `entry<Settings>` block (after line 192), before the closing `}` of `entryProvider`:

```kotlin
        entry<Sources> {
            SourcesScreen(
                onBack = { backStack.removeLastOrNull() },
                modifier = Modifier.safeDrawingPadding()
            )
        }
```

- [ ] **Step 3: Add the Settings menu row**

In `app/src/main/java/com/smiledev/rafiq_quran/ui/settings/SettingsScreen.kt`, add the import after `import com.smiledev.rafiq_quran.R` (line 41):

```kotlin
import com.smiledev.rafiq_quran.Sources
```

After the `AnimatedVisibility(visible = expanded) { ... }` block (which closes at line 126), add a visible, always-accessible row (it stays outside the "More Features" expandable so users can always reach it):

```kotlin
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            MoreFeatureItem(R.string.sources_and_authenticity) { onNavigate(Sources) }
```

- [ ] **Step 4: Verify the build**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
.\gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/smiledev/rafiq_quran/NavigationKeys.kt app/src/main/java/com/smiledev/rafiq_quran/Navigation.kt app/src/main/java/com/smiledev/rafiq_quran/ui/settings/SettingsScreen.kt
git commit -m "feat(sources): wire sources screen into navigation and settings"
```

---

### Task 5: Instrumented tests

**Files:**
- Create: `app/src/androidTest/java/com/smiledev/rafiq_quran/ui/sources/SourcesScreenTest.kt`
- Modify: `app/src/androidTest/java/com/smiledev/rafiq_quran/ui/settings/SettingsScreenTest.kt`

**Interfaces:**
- Consumes: `SourcesScreen` from Task 3; the Settings row added in Task 4.

- [ ] **Step 1: Write the failing screen test**

Create `app/src/androidTest/java/com/smiledev/rafiq_quran/ui/sources/SourcesScreenTest.kt`:

```kotlin
package com.smiledev.rafiq_quran.ui.sources

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class SourcesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sectionsAndItemsAreDisplayed() {
        composeTestRule.setContent { SourcesScreen(onBack = {}) }

        composeTestRule.onNodeWithText("Quran").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hadith").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prayer Times").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Sahih al-Bukhari").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Sahih Muslim").performScrollTo().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Open source")[0].performScrollTo().assertIsDisplayed()
    }

    @Test
    fun hadithItemsShowTranslatorCredit() {
        composeTestRule.setContent { SourcesScreen(onBack = {}) }

        composeTestRule.onNodeWithText("Translator: Muhsin Khan").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Translator: Abdul Hamid Siddiqui").performScrollTo().assertIsDisplayed()
    }
}
```

> Note: below-the-fold nodes (`Prayer Times`, `Sahih al-Bukhari`, `Sahih Muslim`, both translator credits) need `performScrollTo()` because the screen is a scrollable Column taller than the emulator viewport. `"Open source"` matches 6 nodes (one per card with a link), so the assertion targets `onAllNodesWithText(...)[0]`.

- [ ] **Step 2: Add the Settings screen test**

Append this test method to `app/src/androidTest/java/com/smiledev/rafiq_quran/ui/settings/SettingsScreenTest.kt` (inside the `SettingsScreenTest` class, after `radioOptionsAreDisplayed`):

```kotlin
    @Test
    fun sourcesItemIsDisplayed() {
        val prefs = mockk<PreferencesManager>(relaxed = true)
        every { prefs.themeMode } returns MutableStateFlow("system")
        every { prefs.translationLanguage } returns MutableStateFlow("system")
        every { prefs.ayahFontSize } returns MutableStateFlow(22)
        every { prefs.translationFontSize } returns MutableStateFlow(15)
        coEvery { prefs.setThemeMode(any()) } returns Unit
        coEvery { prefs.setTranslationLanguage(any()) } returns Unit
        coEvery { prefs.setAyahFontSize(any()) } returns Unit
        coEvery { prefs.setTranslationFontSize(any()) } returns Unit
        val viewModel = SettingsViewModel(prefs, createDispatcherProvider())

        composeTestRule.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Sources & Authenticity").assertIsDisplayed()
    }
```

- [ ] **Step 3: Run the instrumented tests on the emulator**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
.\gradlew connectedDebugAndroidTest
```

Expected: `SourcesScreenTest.sectionsAndItemsAreDisplayed`, `SourcesScreenTest.hadithItemsShowTranslatorCredit`, and `SettingsScreenTest.sourcesItemIsDisplayed` PASS.

> Note: the emulator runs with English locale by default, so assertions use the EN strings. If your emulator is set to Indonesian, the `onNodeWithText` calls in `sourcesItemIsDisplayed` must use `"Sumber & Keaslian"` instead. Pre-existing failures in `SettingsScreenTest` (the font-size assertions in `allSectionsAreDisplayed` reference rows that the current `SettingsScreen.kt` does not render) are NOT caused by this change and are out of scope.

- [ ] **Step 4: Commit**

```powershell
git add app/src/androidTest/java/com/smiledev/rafiq_quran/ui/sources/SourcesScreenTest.kt app/src/androidTest/java/com/smiledev/rafiq_quran/ui/settings/SettingsScreenTest.kt
git commit -m "test(sources): add instrumented tests for sources screen"
```

---

## Testing Strategy

- **JVM unit tests** (`testDebug`): `SourcesCatalogTest` covers the catalog contract — section count/order, per-section item counts, non-zero resource IDs, https-only links, hadith authenticity labels, and that exactly one item (the default location) has no link. This is the TDD anchor: the test is written before the catalog.
- **Instrumented tests** (`connectedDebugAndroidTest`, requires `Medium_Phone_API_35` emulator): `SourcesScreenTest` renders the screen and asserts sections, items, translator credits, and the link row; `SettingsScreenTest.sourcesItemIsDisplayed` asserts the new Settings row renders.
- **Build gate:** `assembleDebug` after each task.
- **Manual smoke test:** Settings → Sources & Authenticity; verify EN and ID (`Settings → ... `; the app strings follow the device locale) render; tap "Open source" on a few items and confirm the browser opens; confirm the Indonesian label set renders for the ID locale.

## Performance Considerations

- No network, no DB, no coroutines — the screen is static. `sourcesSections()` builds a list of 7 small objects on every composition; if it ever needs to avoid that, it can be hoisted to a `remember { sourcesSections() }` or a top-level `val`. Not necessary at this size.
- The screen scrolls lazily-free (single `verticalScroll` Column) — 7 cards is trivially small; no `LazyColumn` needed.
- Links only resolve to an activity when tapped; no permissions added.

## Self-Review Notes

- **Spec coverage:** Design's 4 requirements map to tasks — (1) screen reachable from Settings → Task 4; (2) content model with Quran/Hadith/Prayer sections → Task 2; (3) cards + link handling → Task 3; (4) EN/ID strings → Task 1. The spec's content table matches the catalog exactly (Quran 3, Hadith 2, Prayer 2, 7 items total).
- **Placeholder scan:** every code step contains full file contents; no TBD/TODO.
- **Type consistency:** `SourceItem`/`SourceSection`/`sourcesSections()` names and signatures are identical across Tasks 2 and 3; `SourcesScreen` signature matches across Tasks 3, 4, and 5; string keys match between Task 1 and Tasks 2-4.
- **Deviation from spec:** the spec said content would live inside `SourcesScreen.kt`; the plan splits the catalog into `SourcesCatalog.kt` so it is unit-testable without an emulator. The Settings row is placed outside the "More Features" expandable for discoverability (the design left placement open).
