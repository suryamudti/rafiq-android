---
name: pr-with-screenshot
description: Flow to build, test on emulator, take screenshots, and submit a GitHub Pull Request with embedded screenshots in the description.
---

# PR With Screenshot Flow

Use this skill when implementing a feature or UI change on a separate branch and submitting a Pull Request with UI screenshots embedded directly into the PR description.

## Workflow Steps

### 1. Separate Branch
- Ensure work is on a dedicated feature/fix branch:
  ```powershell
  git checkout -b <branch-name>
  ```

### 2. Implementation & Local Verification
- Implement the changes adhering to repository guidelines (multi-module, Jetpack Compose, Room, Hilt, Material 3).
- Run unit tests to verify correctness:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
  .\gradlew testDebug
  ```
- Build the debug APK:
  ```powershell
  .\gradlew assembleDebug
  ```

### 3. Emulator Deployment & Screenshot Capture
- Ensure Android emulator is running (e.g. `Medium_Phone_API_35` via `adb devices`).
  If not running, launch:
  ```powershell
  & "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Medium_Phone_API_35
  ```
- Install the debug APK:
  ```powershell
  $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
  & $adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-x86_64-debug.apk
  ```
- Launch app and navigate to relevant screens:
  ```powershell
  & $adb -s emulator-5554 shell monkey -p com.smiledev.rafiq_quran -c android.intent.category.LAUNCHER 1
  ```
- Capture binary screenshots via adb pull:
  ```powershell
  & $adb -s emulator-5554 shell screencap -p /sdcard/screen.png
  & $adb -s emulator-5554 pull /sdcard/screen.png docs/screenshots/<screen_name>.png
  ```

### 4. Commit and Push
- Stage code changes and screenshots in `docs/screenshots/`:
  ```powershell
  git add <source_files> docs/screenshots/
  git commit -m "feat: description of changes"
  git push -u origin <branch-name>
  ```

### 5. Create Pull Request with Rendered Screenshots
- Embed images in the PR markdown description using raw GitHub URLs pointing to the branch so they render directly:
  ```markdown
  ## Screenshots

  | Screen | Screenshot |
  | --- | --- |
  | Screen Name | <img src="https://raw.githubusercontent.com/<owner>/<repo>/<branch>/docs/screenshots/<name>.png" width="300" /> |
  ```
- Create the PR with `gh pr create`:
  ```powershell
  gh pr create --title "<title>" --body "<body>"
  ```
