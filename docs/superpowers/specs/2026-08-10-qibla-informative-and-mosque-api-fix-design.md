# Qibla Informative Enhancements + Mosque API Fix — Design

Date: 2026-08-10
Status: Approved (user approved design on 2026-08-10)

## Problem

1. **Mosque API always fails.** The nearby-mosque screen (Overpass API) never returns data. Root cause confirmed by live testing: `https://overpass-api.de/api/interpreter` returns HTTP **406** for programmatic clients that lack a descriptive custom `User-Agent`. The app sends OkHttp's default UA (`okhttp/4.x`), which is rejected. Secondary mirrors are intermittently overloaded (`overpass.kumi.systems` returned 502, `overpass.private.coffee` timed out; `maps.mail.ru` responded 200). The app has no failover, so any single mirror outage breaks the screen.

2. **Qibla screen is thin.** It shows only a bearing in degrees, distance to Mecca, and a compass. No compass direction name, no live alignment feedback, no Kaaba context, no way to open the Kaaba location in Google Maps.

## Part 1 — Mosque API Fix

### 1.1 Custom User-Agent on shared OkHttp client

`AppModule.provideOkHttpClient()` (app/src/main/java/com/smiledev/rafiq/di/AppModule.kt) currently builds:

```
OkHttpClient.Builder()
    .connectTimeout(30, SECONDS)
    .readTimeout(30, SECONDS)
    .writeTimeout(30, SECONDS)
    .addInterceptor(HttpLoggingInterceptor...)
    .build()
```

Add a request interceptor that sets `User-Agent: RafiqApp/1.0 (Android Islamic App)`. Use OkHttp's `Interceptor`:

```kotlin
.addInterceptor { chain ->
    val request = chain.request().newBuilder()
        .header("User-Agent", "RafiqApp/1.0 (Android Islamic App)")
        .build()
    chain.proceed(request)
}
```

This applies to all Retrofit clients (Aladhan, Metals, IslamicApp, EQuran, Overpass) and is harmless to the others. Verified live: `overpass-api.de` returns 200 with this UA.

### 1.2 Overpass mirror failover

Refactor `OverpassApi` (data/src/main/kotlin/com/smiledev/rafiq/data/remote/OverpassApi.kt) to try a list of mirror services in order and fall through on failure.

**Implementation (final):**
1. DI (`AppModule.provideOverpassApiServices`): provide `List<OverpassApiService>`, one `Retrofit` built per mirror base URL (`https://overpass-api.de/api/`, `https://overpass.kumi.systems/api/`, `https://maps.mail.ru/osm/tools/overpass/api/`), sharing the same OkHttp client and Gson converter. Remove the old `provideOverpassRetrofit` / `provideOverpassApiService` / `provideOverpassApi` single-service providers (replaced by a single `provideOverpassApi(services: List<OverpassApiService>)`).
2. `OverpassApi` constructor takes `List<OverpassApiService>`; `fetchMosques` iterates services in order, returns the first successful result, and on per-service exception records it and tries the next. If all mirrors throw, rethrow the last exception so the repository still maps it to `AppError.Network`.
3. Tests: update `OverpassApiTest` (constructor now takes a list) and add a fallback test where the first service throws and the second returns data.

### 1.3 Query consideration

The current query uses `out center 50;`. Device verification revealed the real bug: Retrofit `@POST("interpreter")` + `@Query("data")` sends the query in the URL but leaves the POST body **empty**, which Overpass rejects with `400 Bad Request` on every mirror (custom User-Agent alone is not enough). Fixed by sending the query as the POST body: `@FormUrlEncoded @POST("interpreter") suspend fun query(@Field("data") query: String)` — verified live returning `200 OK` against `overpass-api.de` (mirror failover also observed: a 504 on one mirror triggered the next).

## Part 2 — Qibla Screen Enhancements

### 2.1 Direction name

Derive a 16-point compass direction name from the bearing in `QiblaViewModel` (or a small pure function in `QiblaScreen.kt`). Standard 16-point rose: N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW. Display below the bearing, e.g. `NW (295°)`.

Pure function `compassDirection(bearing: Int): String` — unit-testable (add tests to `QiblaCalculatorTest.kt`).

### 2.2 Live heading + offset indicator

`QiblaScreen` already reads the device azimuth from `TYPE_ROTATION_VECTOR` into `deviceAzimuth`. Add:

- **Offset**: `offset = normalize180(bearing - deviceAzimuth)` (range −180..180). Semantics: positive offset means the user must turn **right** by `offset` degrees to face Qibla; negative means turn **left** by `|offset|`. (Physical justification: a positive bearing difference while holding the device means the target is clockwise/right from current heading.)
- **Live readout**: "Heading: X° / Qibla: Y°".
- **Offset indicator**: "Turn 12° right to face Qibla". When `|offset| <= 5°`, show "Facing Qibla" and color the indicator **green** (the app's accent teal `Color(0xFF009688)` or a success green); otherwise neutral gray/amber.

Pure function `normalizeAngle180(deg: Double): Double` — unit-testable.

### 2.3 Kaaba info card

A `Card` below the compass showing:
- Kaaba / Mecca coords: `21.4225°N, 39.8262°E`
- User's current coords (from `QiblaUiState.userLat/userLon`) formatted to 4 decimal places
- Distance to Mecca: reuse existing `distanceKm` (keep `%d km` formatting, add comma formatting for readability e.g. via `String.format("%,d", ...)`)

### 2.4 Google Maps link

A button/card action using `Icons.Filled.Place` (available in material-icons-core, verified used by DashboardScreen). Launches:

```kotlin
val uri = "geo:21.4225,39.8262?q=21.4225,39.8262(Kaaba)"
context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
```

Wrap in a `runCatching` to avoid crashing if no maps app (fall back to web URL `https://maps.google.com/?q=Kaaba` or just log).

### 2.5 Delegate math to use case

`QiblaViewModel` duplicates `calculateBearing`/`calculateDistance` that already exist in `CalculateQiblaUseCase` (domain). Inject `CalculateQiblaUseCase` into the ViewModel (already provided in DI via `provideCalculateQiblaUseCase`, verified in AppModule) and call it instead of the local computation.

**Concrete change:** make `CalculateQiblaUseCase.calculateBearing`/`calculateDistance` public (they currently are private) and delete the duplicate top-level `calculateBearing`/`calculateDistance` from `QiblaViewModel.kt`. Update `QiblaCalculatorTest.kt` imports to reference the domain use case functions (`com.smiledev.rafiq.domain.usecase.CalculateQiblaUseCase`). Add the new pure helpers (`compassDirection`, `normalizeAngle180`) to `QiblaViewModel.kt` as top-level functions tested by `QiblaCalculatorTest.kt`.

## Strings

Add to `values/strings.xml` and `values-id/strings.xml` (Indonesian):
- `qibla_heading`: "Heading" / "Arah"
- `qibla_bearing`: "Qibla" / "Kiblat"
- `turn_left`: "Turn %d° left" / "Putar %d° ke kiri"
- `turn_right`: "Turn %d° right" / "Putar %d° ke kanan"
- `facing_qibla`: "Facing Qibla" / "Menghadap Kiblat"
- `kaaba_info`: "Kaaba (Mecca)" / "Ka'bah (Mekah)"
- `your_coords`: "Your location" / "Lokasi Anda"
- `kaaba_coords`: "Kaaba coords" / "Koordinat Ka'bah"
- `open_in_maps`: "Open in Google Maps" / "Buka di Google Maps"

## Testing

- Unit tests: `compassDirection`, `normalizeAngle180`, `OverpassApi` mirror fallback (new), existing `MosqueRepositoryImplTest` and `OverpassApiTest` unchanged and passing.
- Run `.\gradlew testDebug` for JVM tests.
- Manual: install to emulator, open Nearby Mosques (expect markers), open Qibla (expect direction name, offset indicator, Kaaba card, maps button).

## Out of Scope

- Not changing the compass drawing itself (rotation/needle logic stays).
- Not adding location permission to Qibla screen (uses DataStore coords as today).
- Not adding new dependencies.
