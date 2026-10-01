# Stability Surgical Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 3 stability hotspots (sticky media notification, main-thread startup block, prayer alarm drift) with minimal churn.

**Architecture:** No new modules. Changes stay in `:app` `service/`, `RafiqApp.kt`, `MainActivity.kt`, reusing `:data` `PreferencesManager` and `:core` `DispatcherProvider`/`retryIO`/`Result`. Public ViewModel and Worker APIs unchanged.

**Tech Stack:** Kotlin 2.1.20, AGP 8.9.2, Hilt 2.56.2 (KAPT), Room 2.8.4, Media3 1.5.1, WorkManager 2.10.0, JUnit4 + MockK + kotlinx-coroutines-test, PowerShell + Temurin JDK 17.

## Global Constraints

- Hilt via KAPT only, never KSP (warning "Kapt currently doesn't support language version 2.0+. Falling back to 1.9." is harmless).
- AGP 8.9.2 only, never 9.x (Hilt 2.56.2 requires AGP 8.x).
- Kotlin 2.1.20 (MapLibre Native 13.5.1 requires Kotlin 2.1).
- Room `room-runtime` MUST be `api` (not `implementation`) in `:data`.
- `material-icons-core` only, never `material-icons-extended`.
- Never use Java `Math.*` — use `kotlin.math.*`.
- Compose `textDirection` is a `TextStyle` property, not a direct `Text` param.
- Arabic font via `FontFamily(Font(R.font.me_quran))`, never `fontResource()`.
- Cross-module nullable `String?` — use `!!` or local `val`/`?:`, never rely on smart-cast.
- DataStore via `PreferencesManager` only, never SharedPreferences.
- Build with `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"` then `.\gradlew assembleDebug`.
- Tests with `.\gradlew testDebug` (JVM) — emulator not required for these tasks.

---

## File Structure

- Modify: `app/src/main/java/com/smiledev/rafiq_quran/service/AudioRecitationService.kt` — dismissable media notification wrapper + `ACTION_STOP_FROM_DISMISS` handling; owns `MediaNotificationPolicy` ongoing decision.
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/service/AudioPlayerController.kt` — remove eager `init { connect() }`, lazy connect on first `runOrQueue`; `stop()` clears + releases + stops service.
- Create: `app/src/main/java/com/smiledev/rafiq_quran/service/MediaNotificationPolicy.kt` — pure decision `shouldBeOngoing(isPlaying: Boolean): Boolean`, testable without Android.
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/RafiqApp.kt` — remove `runBlocking` from `attachBaseContext`, move channel+schedule to IO scope.
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/MainActivity.kt` — remove `runBlocking` from `attachBaseContext`, add async locale `recreate()` check in `onCreate`.
- Create: `app/src/main/java/com/smiledev/rafiq_quran/StartupLocalePolicy.kt` — pure `shouldRecreate(currentLang: String, storedLang: String): Boolean`, testable.
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/service/PrayerNotificationWorker.kt` — single `combine().first()`, reuse timeouts, `setInitialDelay` to midnight, Friday resolved at trigger only.
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/service/PrayerAlarmReceiver.kt` — resolve Friday from trigger-time `Calendar.getInstance()`, ignore stale `isFriday` extra except as fallback.
- Test: `app/src/test/java/com/smiledev/rafiq_quran/service/MediaNotificationPolicyTest.kt` (new)
- Test: `app/src/test/java/com/smiledev/rafiq_quran/StartupLocalePolicyTest.kt` (new)
- Test: extend `app/src/test/java/com/smiledev/rafiq_quran/service/PrayerNotificationWorkerTest.kt` (existing)

---

### Task 1: Dismissable media notification + lazy controller

**Files:**
- Create: `app/src/main/java/com/smiledev/rafiq_quran/service/MediaNotificationPolicy.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/service/AudioRecitationService.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/service/AudioPlayerController.kt`
- Test: `app/src/test/java/com/smiledev/rafiq_quran/service/MediaNotificationPolicyTest.kt`

**Interfaces:**
- Consumes: Media3 `Player.isPlaying`, `MediaSessionService.onStartCommand`, `AudioPlayerController.play/toggle/stop/seek` (unchanged signatures).
- Produces: `object MediaNotificationPolicy { fun shouldBeOngoing(isPlaying: Boolean): Boolean }`, `const val ACTION_STOP_FROM_DISMISS = "com.smiledev.rafiq_quran.action.STOP_FROM_DISMISS"` in `AudioRecitationService.Companion`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.smiledev.rafiq_quran.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaNotificationPolicyTest {
    @Test
    fun ongoingOnlyWhenPlaying() {
        assertTrue(MediaNotificationPolicy.shouldBeOngoing(true))
        assertFalse(MediaNotificationPolicy.shouldBeOngoing(false))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq_quran.service.MediaNotificationPolicyTest"`
Expected: FAIL with "unresolved reference: MediaNotificationPolicy"

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.smiledev.rafiq_quran.service

object MediaNotificationPolicy {
    fun shouldBeOngoing(isPlaying: Boolean): Boolean = isPlaying
}
```

In `AudioRecitationService.kt`, add to `companion object`:

```kotlin
const val ACTION_STOP_FROM_DISMISS = "com.smiledev.rafiq_quran.action.STOP_FROM_DISMISS"
```

In `onCreate()`, wrap provider (replace `setMediaNotificationProvider(notificationProvider)` with):

```kotlin
val baseProvider = notificationProvider
setMediaNotificationProvider(
    object : androidx.media3.session.MediaNotification.Provider {
        override fun createNotification(
            mediaSession: MediaSession,
            customLayout: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
            actionFactory: androidx.media3.session.MediaNotification.ActionFactory,
            onNotificationChangedCallback: androidx.media3.session.MediaNotification.Provider.Callback
        ): androidx.media3.session.MediaNotification {
            val base = baseProvider.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedCallback)
            val notif = base.notification
            val ongoing = MediaNotificationPolicy.shouldBeOngoing(player.isPlaying)
            if (!ongoing) {
                notif.flags = notif.flags and android.app.Notification.FLAG_ONGOING_EVENT.inv()
            }
            val deleteIntent = android.app.PendingIntent.getService(
                this@AudioRecitationService,
                0,
                android.content.Intent(this@AudioRecitationService, AudioRecitationService::class.java).setAction(ACTION_STOP_FROM_DISMISS),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            notif.deleteIntent = deleteIntent
            return base
        }
        override fun handleCustomCommand(
            session: MediaSession,
            action: String,
            extras: android.os.Bundle
        ): Boolean = baseProvider.handleCustomCommand(session, action, extras)
    }
)
```

Add `onStartCommand` override:

```kotlin
override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP_FROM_DISMISS) {
        player.stop()
        player.clearMediaItems()
        stopSelf()
        return START_NOT_STICKY
    }
    return super.onStartCommand(intent, flags, startId)
}
```

In `AudioPlayerController.kt`, delete the block:

```kotlin
init {
    connect()
}
```

No other signature changes. `runOrQueue` already calls `connect()` lazily.

- [ ] **Step 4: Run test to verify it passes**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq_quran.service.MediaNotificationPolicyTest" --tests "com.smiledev.rafiq_quran.ui.recitation.RecitationViewModelTest"`
Expected: PASS (existing `RecitationViewModelTest` still passes — public `play/toggle/stop/seek` unchanged).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq_quran/service/MediaNotificationPolicy.kt app/src/main/java/com/smiledev/rafiq_quran/service/AudioRecitationService.kt app/src/main/java/com/smiledev/rafiq_quran/service/AudioPlayerController.kt app/src/test/java/com/smiledev/rafiq_quran/service/MediaNotificationPolicyTest.kt
git commit -m "fix(media): dismissable notification when paused, lazy controller connect"
```

### Task 2: Non-blocking startup locale

**Files:**
- Create: `app/src/main/java/com/smiledev/rafiq_quran/StartupLocalePolicy.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/RafiqApp.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/MainActivity.kt`
- Test: `app/src/test/java/com/smiledev/rafiq_quran/StartupLocalePolicyTest.kt`

**Interfaces:**
- Consumes: `PreferencesManager.translationLanguage: Flow<String>`, `Context.wrapLocale(lang: String)`, `resolveLocale(lang: String)`.
- Produces: `object StartupLocalePolicy { fun shouldRecreate(currentLang: String, storedLang: String): Boolean }` — true when `storedLang != "system"` and `storedLang != currentLang`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.smiledev.rafiq_quran

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupLocalePolicyTest {
    @Test
    fun recreatesOnlyOnMismatch() {
        assertTrue(StartupLocalePolicy.shouldRecreate("system", "id"))
        assertTrue(StartupLocalePolicy.shouldRecreate("en", "id"))
        assertFalse(StartupLocalePolicy.shouldRecreate("id", "id"))
        assertFalse(StartupLocalePolicy.shouldRecreate("en", "system"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq_quran.StartupLocalePolicyTest"`
Expected: FAIL with "unresolved reference: StartupLocalePolicy"

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.smiledev.rafiq_quran

object StartupLocalePolicy {
    fun shouldRecreate(currentLang: String, storedLang: String): Boolean {
        if (storedLang == "system") return false
        return currentLang != storedLang
    }
}
```

In `RafiqApp.kt`, replace `attachBaseContext` body with:

```kotlin
override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
}

override fun onCreate() {
    super.onCreate()
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            PrayerNotificationWorker.createNotificationChannel(this@RafiqApp)
        }
        PrayerNotificationWorker.schedule(this@RafiqApp)
    }
}
```

Remove `runBlocking`, `withTimeoutOrNull`, `PreferencesManager`, `flow.first` imports from `RafiqApp.kt`.

In `MainActivity.kt`, replace `attachBaseContext` body with:

```kotlin
override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(newBase)
}
```

Remove `runBlocking`, `withTimeoutOrNull`, `flow.first` imports used only there. No `recreate()` needed: `onCreate.setContent` already does `collectAsState(translationLanguage)` + `remember(translationLanguage) { context.wrapLocale(...) }` with `CompositionLocalProvider`, so locale switches apply via recomposition. Keep that block unchanged.

Note: `StartupLocalePolicy.shouldRecreate` remains as pure decision helper for future use and for the unit test; it is not wired to `recreate()` in this surgical fix to avoid activity-restart loops.

- [ ] **Step 4: Run test to verify it passes**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq_quran.StartupLocalePolicyTest" --tests "com.smiledev.rafiq_quran.LocaleWrapperTest"`
Expected: PASS. Then: `.\gradlew :app:assembleDebug` must succeed with no `runBlocking` left in `RafiqApp.kt` or `MainActivity.attachBaseContext` (verify with grep).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq_quran/StartupLocalePolicy.kt app/src/main/java/com/smiledev/rafiq_quran/RafiqApp.kt app/src/main/java/com/smiledev/rafiq_quran/MainActivity.kt app/src/test/java/com/smiledev/rafiq_quran/StartupLocalePolicyTest.kt
git commit -m "fix(startup): remove main-thread DataStore block, async locale apply"
```

### Task 3: Reliable prayer scheduling

**Files:**
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/service/PrayerNotificationWorker.kt`
- Modify: `app/src/main/java/com/smiledev/rafiq_quran/service/PrayerAlarmReceiver.kt`
- Test: `app/src/test/java/com/smiledev/rafiq_quran/service/PrayerNotificationWorkerTest.kt`

**Interfaces:**
- Consumes: `PreferencesManager.latitude/longitude/prayerCalculationMethod: Flow<String/Int>`, `com.smiledev.rafiq_quran.core.retryIO`, Aladhan `v1/timings/{date}?latitude=&longitude=&method=`.
- Produces: `PrayerNotificationWorker.schedule(context)` with midnight-aligned initial delay (same `WORK_NAME="prayer_notification_worker"`), `prayerTriggerMillis(time: String): Long` made `internal` for testing (was `private`).

- [ ] **Step 1: Write the failing test**

Append to existing `PrayerNotificationWorkerTest.kt`:

```kotlin
@Test
fun triggerParsesValidTime() {
    val worker = mockk<PrayerNotificationWorker>(relaxed = true)
    val millis = worker.prayerTriggerMillisPublic("05:30")
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    org.junit.Assert.assertEquals(5, cal.get(java.util.Calendar.HOUR_OF_DAY))
    org.junit.Assert.assertEquals(30, cal.get(java.util.Calendar.MINUTE))
}

@Test
fun triggerRejectsMalformedTime() {
    val worker = mockk<PrayerNotificationWorker>(relaxed = true)
    org.junit.Assert.assertEquals(0L, worker.prayerTriggerMillisPublic("bad"))
}
```

This fails because `prayerTriggerMillisPublic` does not exist yet.

- [ ] **Step 2: Run test to verify it fails**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq_quran.service.PrayerNotificationWorkerTest"`
Expected: FAIL with "unresolved reference: prayerTriggerMillisPublic"

- [ ] **Step 3: Write minimal implementation**

In `PrayerNotificationWorker.kt`:

1. Change `private fun prayerTriggerMillis` to `internal fun prayerTriggerMillis`, and add:

```kotlin
// Test-only alias kept stable for JVM tests without Worker params.
internal fun prayerTriggerMillisPublic(time: String): Long = prayerTriggerMillis(time)
```

2. Replace the three `runBlocking { prefs.X.first() }` lines with one:

```kotlin
val (latStr, lonStr, method) = runBlocking {
    kotlinx.coroutines.flow.combine(
        prefs.latitude,
        prefs.longitude,
        prefs.prayerCalculationMethod
    ) { lat, lon, m -> Triple(lat, lon, m) }.first()
}
val lat = latStr.toDoubleOrNull() ?: -6.2088
val lon = lonStr.toDoubleOrNull() ?: 106.8456
```

3. In `schedule()`, align to midnight:

```kotlin
fun schedule(context: Context) {
    val now = java.util.Calendar.getInstance()
    val midnight = (now.clone() as java.util.Calendar).apply {
        add(java.util.Calendar.DAY_OF_YEAR, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 5)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val delayMs = (midnight.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    val request = PeriodicWorkRequestBuilder<PrayerNotificationWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}
```

4. Remove schedule-time Friday baking — delete `val isTodayFriday = ...` and remove `putExtra("isFriday", isTodayFriday)` from the alarm intent. Keep `putExtra("name", name)` and `putExtra("time", time)`.

5. Harden network fetch (same 30s timeouts as `AppModule.provideOkHttpClient`, no new DI): replace inline client block with:

```kotlin
val timings = try {
    com.smiledev.rafiq_quran.core.retryIO(times = 3, initialDelay = 200) {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val url = "https://api.aladhan.com/v1/timings/$today?latitude=$lat&longitude=$lon&method=$method"
        val request = okhttp3.Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw java.io.IOException("HTTP ${response.code}")
        val json = org.json.JSONObject(response.body?.string() ?: "")
        com.smiledev.rafiq_quran.core.Result.Success(json.getJSONObject("data").getJSONObject("timings"))
    }.let { result ->
        when (result) {
            is com.smiledev.rafiq_quran.core.Result.Success -> result.data
            is com.smiledev.rafiq_quran.core.Result.Error -> return androidx.work.ListenableWorker.Result.retry()
        }
    }
} catch (e: Exception) {
    return androidx.work.ListenableWorker.Result.retry()
}
```

Keep `Result.retry()` on `IOException`, `Result.success()` otherwise. `retryIO` signature is `suspend fun <T> retryIO(times, initialDelay, maxDelay, factor, block: suspend () -> Result<T, AppError>)` — wrap `IOException` as `AppError.Network` inside block if needed; simplest is try/catch above returning `Result.retry()`.

In `PrayerAlarmReceiver.kt`, replace `onReceive` with:

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    val name = intent.getStringExtra("name") ?: "Prayer"
    val time = intent.getStringExtra("time") ?: ""
    val cal = java.util.Calendar.getInstance()
    PrayerNotificationWorker.postPrayerNotification(context, name, time, cal)
}
```

Friday is now resolved at trigger time inside `getLocalizedPrayerName` via `Calendar.getInstance()` day-of-week — no stale extra.

- [ ] **Step 4: Run test to verify it passes**

Run: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; .\gradlew :app:testDebugUnitTest --tests "com.smiledev.rafiq_quran.service.PrayerNotificationWorkerTest"`
Expected: PASS (all 5 existing + 2 new). Then `.\gradlew :app:assembleDebug` must succeed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/smiledev/rafiq_quran/service/PrayerNotificationWorker.kt app/src/main/java/com/smiledev/rafiq_quran/service/PrayerAlarmReceiver.kt app/src/test/java/com/smiledev/rafiq_quran/service/PrayerNotificationWorkerTest.kt
git commit -m "fix(prayer): single prefs read, midnight-aligned schedule, trigger-time Friday"
```
