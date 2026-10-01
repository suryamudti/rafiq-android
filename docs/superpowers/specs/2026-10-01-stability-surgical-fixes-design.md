# Stability Surgical Fixes — Design

Date: 2026-10-01
Goal: Tech health / Stability (user-approved Option A)
Scope: 3 hotspots only, no new modules, no new features.

## 1. Context

Rafiq Android is multi-module (`:core/:domain/:data/:app`), Compose + Hilt (KAPT) + Room 2.8.4 + Navigation3, Kotlin 2.1.20, AGP 8.9.2. Recent work landed dashboard revamp, tasbih history, prayer-log streaks, CI pr-check/release, lint fixes.

Approved pain: ANRs, crashes, notification bugs, offline DB issues. Investigation found:

- `app/.../service/AudioRecitationService.kt` + `AudioPlayerController.kt` (singleton): `media_playback` notification uses `FLAG_NO_CLEAR` in all states (playing/paused/stopped), persists after `STATE_ENDED`, only force-stop clears. `AudioPlayerController.init { connect() }` eagerly binds on app start.
- `app/.../RafiqApp.kt`: `attachBaseContext()` does `runBlocking { PreferencesManager(base).translationLanguage.first() }` with 1000ms timeout on main thread.
- `app/.../service/PrayerNotificationWorker.kt`: `doWork()` does 3x `runBlocking first()` (lat/lon/method), builds inline `OkHttpClient` (10s timeout, no reuse), `PeriodicWork 1 DAY` with no midnight alignment, `isTodayFriday` baked at schedule time. `PrayerAlarmReceiver` already resolves Friday at trigger time.

## 2. Architecture

- No new modules. Changes confined to `:app` (`service/`, `RafiqApp.kt`) + reuse `:data` `PreferencesManager`, `:core` `DispatcherProvider`, `Result`, `retryIO`, `AppError`.
- No KAPT/KSP change, no `material-icons-extended`, no `Math.*` (use `kotlin.math`), no NavKey changes.
- Public APIs unchanged: `RecitationViewModel.play/toggle/stop/seek`, `PrayerNotificationWorker.WORK_NAME`, `CHANNEL_ID="prayer_times"`, `media_playback` channel ID. Existing installs migrate cleanly.
- Follows existing patterns: `@HiltViewModel` + `StateFlow<UiState>`, `Result.Success/Error`, `viewModelScope.launch(dispatcherProvider.io)`.

## 3. Components

### 3.1 Media session lifecycle (isolated unit)

Purpose: make media notification dismissable when not playing, without breaking background playback.
Uses: `AudioRecitationService`, `AudioPlayerController`, `PlaybackState`. Depends on: Media3 `MediaSessionService`, `DefaultMediaNotificationProvider`.

- Customize `DefaultMediaNotificationProvider` via wrapper `MediaNotification.Provider`: delegate to `DefaultMediaNotificationProvider.getNotification()`, then set `notification.flags = if (player.isPlaying) notification.flags or Notification.FLAG_ONGOING_EVENT else notification.flags and Notification.FLAG_ONGOING_EVENT.inv()` so `FLAG_NO_CLEAR` applies only while playing.
- Set `deleteIntent` on notification -> explicit `Intent(context, AudioRecitationService::class.java).setAction(ACTION_STOP_FROM_DISMISS)` (`"com.smiledev.rafiq_quran.action.STOP_FROM_DISMISS"`), handled in `onStartCommand` to call `player.stop()`, `player.clearMediaItems()`, `stopSelf()`.
- Keep existing `STOP_DELAY_MS=3000L` auto-`stopSelf()` on `STATE_ENDED`; ensure `handler.removeCallbacks` on `BUFFERING/READY/isPlaying/mediaItemTransition` (already present).
- `AudioPlayerController`: remove eager `init { connect() }`; connect lazily on first `play/toggle/seek`. `stop()` does `stop+clear+release+stopService`. `release()` increments `connectGeneration`, cancels poller, clears queue (already present). Keep `MAX_CONNECT_ATTEMPTS=3`, `CONNECT_RETRY_DELAY_MS=1500L`.
- `RecitationViewModel.onCleared()` keeps calling `audioPlayer.release()`; no API change.

### 3.2 Startup path (isolated unit)

Purpose: remove main-thread block, keep locale correct.
Uses: `RafiqApp`, `LocaleWrapper.wrapLocale`. Depends on: `PreferencesManager.translationLanguage`.

- `attachBaseContext()`: do NOT `runBlocking` and do NOT read DataStore. Call `super.attachBaseContext(base)` directly with no locale wrapping. Locale is applied in `MainActivity.onCreate` / `attachBaseContext` of activity via existing `wrapLocale()` after collecting prefs.
- Apply user language async: `MainActivity` collects `translationLanguage` on `dispatcherProvider.io`, calls `recreate()` only if `currentLocaleCode() != storedLang` and `storedLang != "system"`.
- `onCreate()`: `createNotificationChannel()` + `schedule()` off-loaded to `CoroutineScope(IO)`, not on main critical path.

### 3.3 Prayer scheduling (isolated unit)

Purpose: reliable daily alarms, no drift, no redundant IO.
Uses: `PrayerNotificationWorker`, `PrayerAlarmReceiver`. Depends on: Aladhan API, `AlarmManager.setAlarmClock`, `PreferencesManager`.

- Single DataStore read: `runBlocking { combine(prefs.latitude, prefs.longitude, prefs.prayerCalculationMethod) { lat, lon, method -> Triple(lat, lon, method) }.first() }` — one blocking call instead of three sequential `first()` calls.
- Reuse injected `OkHttpClient` from `AppModule` (30s timeouts, User-Agent) instead of inline builder; wrap fetch in `retryIO(times=3, initialDelay=200)` from `:core`.
- Align periodic work to next midnight: `val delayToMidnight = (nextMidnightMillis - System.currentTimeMillis())` computed via `Calendar`, passed as `setInitialDelay(delayToMidnight, TimeUnit.MILLISECONDS)` in `schedule()`, keep `Periodic 1 DAY`, `ExistingPeriodicWorkPolicy.UPDATE`, same `WORK_NAME`.
- Friday: do NOT bake `isFriday` at schedule time for future days; pass date string in alarm extras, resolve localized name at trigger in `PrayerAlarmReceiver`/`postPrayerNotification` via `Calendar.getInstance()` (receiver already does this — keep and remove schedule-time override).
- Skip past times (`trigger <= now`), use `name.hashCode()` requestCode (keep), `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE` (keep). Fallback `postPrayerNotification` only if `scheduled==0`.

## 4. Data flow

- Recitation UDF unchanged: UI -> `ViewModel.play/toggle` -> `AudioPlayerController.runOrQueue` -> `MediaController` -> `playbackState: StateFlow` -> UI collect on `dispatcherProvider.main`.
- Startup: `attachBaseContext (sync, no IO)` -> `onCreate (async channel+schedule)` -> `MainActivity collect language (IO)` -> `recreate if mismatch`.
- Prayer: `Worker (background)` -> `Aladhan timings/{date}?lat&lon&method` -> `parse JSONObject timings` -> `AlarmManager.setAlarmClock(trigger, showPi, pi)` per prayer -> `PrayerAlarmReceiver.onReceive` -> `postPrayerNotification`.

## 5. Error handling

- No raw `e.message` to UI (already uses `AppError`; keep).
- Worker: `IOException/JSONException` -> `Result.retry()` (WorkManager backoff); malformed time -> skip that prayer (`prayerTriggerMillis` returns 0). `AlarmManager` unavailable -> catch and skip, continue others.
- Media: connect failure with pending commands -> retry up to 3x1500ms (keep); `future.get()` generation mismatch -> release stale controller (keep).
- Startup: DataStore read failure -> fallback `"system"`, never crash `attachBaseContext` (keep try/catch).

## 6. Testing

- `testDebug` regression (JVM, no emulator):
  1. Controller `stop()` releases + stops service (mock `MediaController`, verify `release()` + `stopService` intent).
  2. Receiver resolves Friday at trigger (set `Calendar.FRIDAY`, assert `getLocalizedPrayerName("dhuhr") == prayer_friday`).
  3. `RafiqApp.attachBaseContext` contains no `runBlocking` (static check / startup timing <16ms).
- Manual: `assembleDebug`, play surah -> pause -> swipe dismiss succeeds; force-stop not required. Kill app, reboot, verify alarms fire at actual times. Cold start trace shows no 1s block.
- Out of scope (YAGNI): `CoroutineWorker` migration, crash reporting, full coverage gates, new features, icon changes, i18n extraction.

## 7. Constraints respected

- `material-icons-core` only, KAPT (not KSP), `kotlin.math`, `TextStyle.textDirection`, `room-runtime: api` in `:data`, cross-module nullable uses `!!`/local val, `FontFamily(Font(R.font.me_quran))`.
