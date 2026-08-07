# Build, Run & Test

## JAVA_HOME (IMPORTANT)

The path in AGENTS.md (`C:\Program Files\Android\Android Studio\jbr`) is a
BROKEN/incomplete JBR — builds fail with `Error loading java.security file`.
The verified working JAVA_HOME on this machine is:

    C:\Program Files\Android\Android Studio1\jbr

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio1\jbr"
```

## Build & install

```powershell
.\gradlew assembleDebug
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
```

Emulator: `Medium_Phone_API_35`. No lint or typecheck tasks are set up.

## Tests

```powershell
.\gradlew testDebug                 # unit tests (JVM)
.\gradlew connectedDebugAndroidTest # instrumented tests (emulator required)
```

Unit tests live in `app/src/test/...` and `data/src/test/...`
(e.g. `QuranViewModelTest.kt`, `MetalPriceRepositoryImplTest.kt`).

## Known quirk

`settings.gradle.kts` includes `:feature:*` modules that don't exist on disk or
in git. Builds still succeed. Do NOT try to "fix" this.
