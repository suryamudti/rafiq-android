# Media Playback Progress & Notification — Design

**Date:** 2026-08-19
**Branch:** `fix/audio-recitation-loading` (current work branch)
**Status:** Approved

## 1. Overview

Add a **seekable progress bar** for the currently playing audio and a **media
notification** (with play/pause/seek/close controls, lock-screen support, and
background playback) covering **both** audio features:

1. **Recitation screen** — full-surah playback (download.quranicaudio.com MP3s).
2. **Quran screen** — verse-by-verse ayah playback with auto-advance
   (everyayah.com `Alafasy_128kbps` MP3s).

Approach: route all playback through the **existing but currently dead**
`AudioRecitationService` (`MediaSessionService`). Media3's `MediaSessionService`
provides the notification, foreground service lifecycle, lock-screen controls, and
background playback for free. `AudioPlayerController` becomes a facade that binds a
`MediaController` to that session.

## 2. Current State (as found)

- `AudioPlayerController` (`app/.../service/AudioPlayerController.kt`) — a `@Singleton`
  ExoPlayer wrapper used by both ViewModels. Exposes `play(url)`, `playAyah(url,
  onComplete)`, `toggle()`, `stop()`, `release()`, and getters `isPlaying`,
  `currentPosition`, `duration`. No MediaSession → no notification, no background
  playback.
- `AudioRecitationService` (`app/.../service/AudioRecitationService.kt`) — a
  `MediaSessionService` declared in `AndroidManifest.xml` (FGS type `mediaPlayback`,
  permissions `POST_NOTIFICATIONS` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` already
  present). It owns its own ExoPlayer + `MediaSession` but **nothing ever starts or
  binds to it** — dead code.
- Both ViewModels depend on the concrete `AudioPlayerController` class, so existing
  `mockk`-based tests mock the concrete class.
- Media3 1.5.1 (`media3-exoplayer` + `media3-session` already in
  `gradle/libs.versions.toml:25,79,80`; wired in `app/build.gradle.kts:138-139`).
- `POST_NOTIFICATIONS` is declared in the manifest but never requested at runtime, so
  on API 33+ the notification would be suppressed unless we add a runtime request.

## 3. Architecture

```
RecitationViewModel ─┐
                     ├─> AudioPlayerController (facade) ── MediaController ──> AudioRecitationService (MediaSessionService)
AyahViewModel      ──┘                                                          └─ owns ExoPlayer + MediaSession
```

- **`AudioRecitationService`** is the single owner of the ExoPlayer + `MediaSession`.
  All playback commands arrive through the session; Media3 handles the notification
  and foreground state.
- **`AudioPlayerController`** is a client-side facade that binds a `MediaController`
  to the service's session and exposes the same surface the ViewModels already use,
  plus new `seekTo()` and a `playbackState: StateFlow`.
- **ViewModels** collect `playbackState` and push position/duration/isPlaying into
  their UiState; screens render a `Slider` + time label.

## 4. AudioRecitationService Changes

Keep it a `MediaSessionService` (manifest is already correct — no manifest change).

- In `onCreate`, keep building the ExoPlayer + `MediaSession`. Ensure `onGetSession`
  returns the session (Media3 shows no notification if it returns null).
- Create a dedicated notification channel (`media_playback`, low/medium importance)
  and install a custom provider in `onCreate`:
  `setMediaNotificationProvider(DefaultMediaNotificationProvider.Builder(this)
  .setChannelId("media_playback").setNotificationId(...).build())`.
  Set a small icon via `setSmallIcon(...)` on the provider — a missing icon is a
  common reason the notification fails to render (use the existing launcher icon or
  an `ic_stat_*` drawable; verify a drawable is available, otherwise add one).
- Remove the unused `play()/pause()/resume()/stopPlayback()` methods — playback is
  driven through the session only.

## 5. AudioPlayerController Changes

`@Singleton`, constructor gains `dispatcherProvider: DispatcherProvider =
DefaultDispatcherProvider` (keeps Hilt injection and existing test construction
working).

Public surface:

```kotlin
fun play(url: String, title: String, artist: String)          // sets MediaItem + MediaMetadata
fun playAyah(url: String, title: String, artist: String, onComplete: () -> Unit)
fun toggle()
fun stop()
fun seekTo(positionMs: Long)
fun release()                                                  // release controller, NOT service player
val playbackState: StateFlow<PlaybackState>                    // positionMs, durationMs, isPlaying
```

Internal mechanics:

- **Bind:** `MediaController.Builder(context, SessionToken(context, ComponentName(
  context, AudioRecitationService::class.java))).buildAsync()`. Listen for connection.
- **Async command queue:** commands issued before connection complete are queued and
  flushed on connect. Because connection is async, `play()/toggle()/stop()/seekTo()`
  may be called before the controller is ready.
- **Completion listener:** register a `Player.Listener` on the controller; on
  `STATE_ENDED` invoke the stored `onComplete` for ayah auto-advance (replaces the
  current ExoPlayer-level listener in `playAyah`).
- **Position polling:** a coroutine on `dispatcherProvider.main` polls
  `controller.currentPosition`/`duration` every ~500ms while `isPlaying` and emits
  into `MutableStateFlow<PlaybackState>`; also update on
  `onPlaybackStateChanged`/`onIsPlayingChanged`/`onMediaItemTransition`.
- **`release()`** releases the `MediaController` and cancels the poller — it must
  **not** release the service's player, so background playback survives the screen
  closing.

## 6. RecitationScreen + RecitationViewModel

- `RecitationViewModel.init` collects `audioPlayer.playbackState` → UiState gains
  `positionMs: Long`, `durationMs: Long` (replacing the manual `isPlaying` sync reads).
- `playSurah` passes metadata: `title = "${chapterNumber}. ${surah.nameSimple}"`,
  `artist = reciter.nameEn`.
- New `seekTo(positionMs)`.
- Now Playing card adds a `Slider` bound to `positionMs..durationMs`:
  - Local drag state so the thumb does not fight the poller while dragging.
  - Seek commits on `onValueChangeFinished` via `viewModel.seekTo(...)`.
  - `mm:ss` current / total time label.

## 7. AyahScreen + AyahViewModel

- `AyahViewModel` collects `playbackState` → UiState gains `positionMs`, `durationMs`.
- `playAyahAudio` passes `title = "Surah X · Ayah Y"`, `artist = "Alafasy"`;
  auto-advance logic (`toggleAyahAudio` / `playAyahAudio` chaining) is unchanged and
  still driven by the `onComplete` callback.
- New `seekTo(positionMs)`.
- A compact progress `Slider` + time label renders only under the currently-playing
  ayah card (`isPlayingAyah`).

## 8. Notification Permission (API 33+)

`POST_NOTIFICATIONS` is declared but never requested. Add a one-time runtime request
triggered on first play (Recitation or ayah), using the existing simple pattern from
`PrayerNotificationWorker.kt:117-124` (`ContextCompat.checkSelfPermission`). Where to
hook it: from the ViewModel/controller is awkward (no Activity context); simplest is a
request from `RecitationScreen`/`AyahScreen` before starting playback, or a small
helper invoked on first play. Design choice: request from the screens when the user
taps play and the permission is not yet granted (leverage
`rememberLauncherForActivityResult`). Playback still starts even if denied (Media3
suppresses the notification only).

## 9. Testing

- Existing `RecitationViewModelTest` / new `AyahViewModelTest` keep mocking the
  concrete `AudioPlayerController` — signature stays compatible.
- New unit tests:
  - `RecitationViewModelTest`: `play()` called with expected title/artist metadata;
    `seekTo()` forwards to controller; UiState reflects `playbackState` emission.
  - `AyahViewModelTest`: `playAyah` completion still chains to the next ayah.
- Controller ↔ service binding is exercised on the emulator (integration), not unit
  tests.
- Verification: `.\gradlew testDebug` and `.\gradlew assembleDebug` must pass
  (JAVA_HOME = `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`).
- Manual check: play a surah → notification appears with seek/close; background the
  app → audio continues; drag slider → position jumps; play an ayah on the Quran
  screen → auto-advance still works.

## 10. Files Touched

- `app/src/main/java/com/smiledev/rafiq_quran/service/AudioRecitationService.kt`
- `app/src/main/java/com/smiledev/rafiq_quran/service/AudioPlayerController.kt`
- `app/src/main/java/com/smiledev/rafiq_quran/ui/recitation/RecitationViewModel.kt`
- `app/src/main/java/com/smiledev/rafiq_quran/ui/recitation/RecitationScreen.kt`
- `app/src/main/java/com/smiledev/rafiq_quran/ui/quran/AyahViewModel.kt`
- `app/src/main/java/com/smiledev/rafiq_quran/ui/quran/AyahScreen.kt`
- `app/src/test/java/com/smiledev/rafiq_quran/ui/recitation/RecitationViewModelTest.kt`
- `app/src/test/java/com/smiledev/rafiq_quran/ui/quran/AyahViewModelTest.kt` (if exists)
- Possibly `MainActivity` or the screens for the POST_NOTIFICATIONS runtime request.
- No `AndroidManifest.xml` changes needed (service + permissions already present).