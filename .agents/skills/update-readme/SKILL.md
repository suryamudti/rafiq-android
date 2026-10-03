---
name: update-readme
description: >-
  Update README.md based on the latest codebase architecture and synchronize screen screenshots in figure/.
  Trigger whenever there is a code structure change, architectural change, new module, dependency/tech stack update,
  new navigation route, or added/updated screens.
---

# Update README & Architecture Sync Skill

Use this skill whenever there is a **code structure change**, **architectural refactoring**, **new module or feature addition**, or **UI screen update** in the project. This ensures `README.md` and repository screenshots stay completely aligned with the codebase.

---

## 🎯 Trigger Conditions

Activate this workflow whenever:
1. **Module changes**: Modules are added, removed, or restructured in `settings.gradle.kts` (e.g. `:app`, `:domain`, `:data`, `:core`, or new feature modules).
2. **Architecture / Layer changes**: Architectural layers, data flow patterns (MVVM, UDF, Clean Architecture), repository patterns, Room databases, or DataStore preferences are altered.
3. **Dependency / Tech stack changes**: Key versions change in `gradle/libs.versions.toml` (e.g., Kotlin, Android Gradle Plugin, Compose BOM, Room, Hilt, Navigation3, MapLibre, Media3).
4. **Navigation & Screen additions**: New routes or screens are defined in `NavigationKeys.kt` and registered in `Navigation.kt` (e.g., Prayer Guidance, Sunnah Guidance, Tasbih History).
5. **Data source & API updates**: External APIs or bundled databases are added, modified, or removed (Aladhan, Metals.live, Overpass, EQuran, bundled SQLite databases).
6. **UI redesigns**: Screen layouts, themes, or core components are significantly updated, requiring updated screenshots in `figure/`.

---

## 📋 Step-by-Step Workflow

### Step 1: Run Architecture & README Audit

Run the bundled audit script to detect discrepancies between the codebase and `README.md`:

```powershell
powershell -ExecutionPolicy Bypass -File .agents/skills/update-readme/scripts/audit-readme-architecture.ps1
```

The script inspects:
- Multi-module definitions in `settings.gradle.kts` vs `README.md`.
- Library versions in `gradle/libs.versions.toml` vs `README.md` badges and tech stack.
- Registered navigation routes in `NavigationKeys.kt` vs the Key Features table.
- Screenshot files in `figure/` vs embedded images in `README.md`.

---

### Step 2: Inspect Codebase Changes

Examine the relevant source files to understand what changed:

1. **Modules**:
   - Check `settings.gradle.kts` for included project modules.
2. **Dependencies**:
   - Check `gradle/libs.versions.toml` for updated library versions (Kotlin, AGP, Hilt, Room, Compose, MapLibre, etc.).
3. **Navigation Routes & Screens**:
   - Check `app/src/main/java/com/smiledev/rafiq_quran/NavigationKeys.kt` and `Navigation.kt` for all available routes.
4. **Domain & Data Layers**:
   - Check `:domain` for new models, repository interfaces, and use cases.
   - Check `:data` for new Room databases, DAOs, Retrofit API services, or DataStore managers.
5. **Core Utilities**:
   - Check `:core` for new common utilities, error types, or helpers.

---

### Step 3: Update `README.md` Sections

Update `README.md` to reflect the latest codebase reality:

#### 1. Header Badges
Keep version badges up to date at the top of `README.md`:
- Kotlin badge (`https://img.shields.io/badge/Kotlin-<version>-blue.svg`)
- Min SDK badge (`https://img.shields.io/badge/Min%20SDK-<version>%2B-brightgreen.svg`)
- Jetpack Compose badge
- Hilt badge (`https://img.shields.io/badge/Hilt-<version>-orange.svg`)

#### 2. Key Features Table (`## 🌟 Key Features`)
Ensure all screens and major features (e.g. Prayer Times, Quran, Hadith, Prayer Guidance, Sunnah Guidance, etc.) are accurately listed with concise descriptions of their functionality.

#### 3. Tech Stack & Open-Source Libraries (`## 🛠️ Tech Stack & Open-Source Libraries`)
Synchronize library versions and descriptions with `gradle/libs.versions.toml`:
- Kotlin version
- Coroutines & Flow
- Hilt (KAPT)
- Navigation3 tokens & runtime
- Room Database version
- MapLibre Native version
- Media3 ExoPlayer version
- Retrofit & OkHttp

#### 4. Multi-Module Structure (`### Multi-Module Structure`)
Ensure the ASCII module tree lists all active modules and their responsibilities:
```
rafiq-android/
├── :app          # Application module (UI screens, ViewModels, Hilt DI, Services, Navigation3)
├── :data         # Data layer (Room DAOs, Databases, Retrofit APIs, DataStore, Repository Impls)
├── :domain       # Domain layer (Repository interfaces, Use cases, Domain models)
└── :core         # Core utilities (Result pattern, AppError, DispatcherProvider, DatabaseCopier)
```

#### 5. Architecture Overview Diagram (`### Layer Architecture Overview`)
Update the Mermaid diagram (`graph TD`) if module connections, use cases, or data flow paths have changed.

#### 6. Data Sources & APIs (`## 🌐 Data Sources & APIs`)
Ensure all external endpoints, bundled SQLite databases, and static assets are listed.

---

### Step 4: Capture & Update Screen Screenshots

When new screens are introduced or UI layouts are updated, capture high-resolution screenshots from the Android emulator and place them into `figure/`.

1. **Verify Emulator Status**:
   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb devices
   ```
   If no emulator is running, start `Medium_Phone_API_35`:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Medium_Phone_API_35
   ```

2. **Build and Install Debug APK**:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
   .\gradlew assembleDebug
   & $adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
   ```

3. **Launch the App & Navigate to Target Screen**:
   ```powershell
   & $adb -s emulator-5554 shell monkey -p com.smiledev.rafiq_quran -c android.intent.category.LAUNCHER 1
   ```
   Navigate to the screen on the emulator using UI interaction or adb input taps.

4. **Capture Screenshot**:
   Use the bundled capture script:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/skills/update-readme/scripts/capture-screenshot.ps1 -ScreenName "<screen_name>"
   ```
   Or run manually:
   ```powershell
   & $adb -s emulator-5554 shell screencap -p /sdcard/<screen_name>.png
   & $adb -s emulator-5554 pull /sdcard/<screen_name>.png figure/<screen_name>.png
   & $adb -s emulator-5554 shell rm -f /sdcard/<screen_name>.png
   ```

   > **Note**: Screenshot resolution on `Medium_Phone_API_35` is `1080 x 2400`. Ensure files are saved in `figure/` with lowercase alphanumeric names (e.g. `figure/prayerguidance.png`).

---

### Step 5: Update the Screenshots Gallery in `README.md`

Maintain the 4-column balanced table in `README.md` under `## 📸 Screenshots`:

```markdown
## 📸 Screenshots

| Dashboard | Prayer Times | Quran Reader | Ayah Reader |
| :---: | :---: | :---: | :---: |
| <img src="figure/dashboard.png" width="220"/> | <img src="figure/prayertimes.png" width="220"/> | <img src="figure/quran.png" width="220"/> | <img src="figure/ayah.png" width="220"/> |

| Qibla Compass | Zakat Calculator | 99 Names of Allah | Tasbih Counter |
| :---: | :---: | :---: | :---: |
| <img src="figure/qibla.png" width="220"/> | <img src="figure/zakat.png" width="220"/> | <img src="figure/asmaulhusna.png" width="220"/> | <img src="figure/tasbih.png" width="220"/> |

| Nearby Mosques | Hadith Library | Prophet Stories | Islamic Calendar |
| :---: | :---: | :---: | :---: |
| <img src="figure/mosques.png" width="220"/> | <img src="figure/hadith.png" width="220"/> | <img src="figure/prophets.png" width="220"/> | <img src="figure/calendar.png" width="220"/> |
```

If adding new screens, create a new 4-column row or group logically. Always set `width="220"` for consistent alignment across devices and browsers.

---

### Step 6: Verify & Complete

1. Re-run the audit script to verify no discrepancies remain:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/skills/update-readme/scripts/audit-readme-architecture.ps1
   ```
2. Review the diff in `README.md`:
   ```powershell
   git diff README.md
   ```
3. Ensure all image files referenced in `README.md` exist under `figure/`.
