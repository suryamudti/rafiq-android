# GitHub Actions CI/CD Workflows for Android

This repository is configured with two automated GitHub Actions workflows under `.github/workflows/`:
1. **Pull Request Check (`pr-check.yml`)**: Runs unit tests on every pull request to ensure stability before merging.
2. **Release Build & Publish (`release.yml`)**: Builds, signs, and automatically publishes release-ready APK/AAB artifacts on every merge to the main branches.

Below is the documentation on how to configure and adapt these workflows for another Android repository.

---

## 1. Workflows Setup

### A. Pull Request Test Check
Create a file at `.github/workflows/pr-check.yml` in your target repository with the following contents:

```yaml
name: Pull Request Check

on:
  pull_request:
    branches:
      - main
      - master

jobs:
  test:
    name: Run Unit Tests
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Source Code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          cache: 'gradle'

      - name: Grant Execute Permission to Gradlew
        run: chmod +x gradlew

      - name: Run Unit Tests
        run: ./gradlew testDebug
```

### B. Release Build, Sign, and Auto-Publish
Create a file at `.github/workflows/release.yml` in your target repository with the following contents:

```yaml
name: Draft Release

on:
  push:
    branches:
      - main
      - master

jobs:
  build:
    name: Build & Release
    runs-on: ubuntu-latest
    permissions:
      contents: write

    steps:
      - name: Checkout Source Code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'
          cache: 'gradle'

      - name: Grant Execute Permission to Gradlew
        run: chmod +x gradlew

      - name: Extract Version Name
        id: get_version
        run: |
          VERSION_NAME=$(sed -n 's/.*baseVersionName = "\(.*\)".*/\1/p' app/build.gradle.kts | tr -d '[:space:]')
          echo "version=$VERSION_NAME" >> $GITHUB_OUTPUT

      - name: Build Release APK & Bundle
        run: ./gradlew assembleRelease bundleRelease -PversionCode=${{ github.run_number }} -PversionName=${{ steps.get_version.outputs.version }}.${{ github.run_number }}

      - name: Gather Build Artifacts
        run: |
          mkdir -p app/build/outputs/release
          cp app/build/outputs/apk/release/*.apk app/build/outputs/release/
          cp app/build/outputs/bundle/release/*.aab app/build/outputs/release/

      - name: Sign Artifacts
        uses: r0adkll/sign-android-release@v1
        id: sign_app
        with:
          releaseDirectory: app/build/outputs/release
          signingKeyBase64: ${{ secrets.SIGNING_KEY }}
          alias: ${{ secrets.ALIAS }}
          keyStorePassword: ${{ secrets.KEY_STORE_PASSWORD }}
          keyPassword: ${{ secrets.KEY_PASSWORD }}
        env:
          BUILD_TOOLS_VERSION: "35.0.0"

      - name: Rename Signed Artifacts
        run: |
          mv app/build/outputs/release/app-release-unsigned-signed.apk app/build/outputs/release/rafiq-v${{ steps.get_version.outputs.version }}.${{ github.run_number }}.apk
          mv app/build/outputs/release/app-release.aab app/build/outputs/release/rafiq-v${{ steps.get_version.outputs.version }}.${{ github.run_number }}.aab

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          tag_name: v${{ steps.get_version.outputs.version }}.${{ github.run_number }}
          name: Release v${{ steps.get_version.outputs.version }}.${{ github.run_number }}
          files: |
            app/build/outputs/release/rafiq-*.apk
            app/build/outputs/release/rafiq-*.aab
          draft: false
          prerelease: false
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## 2. Gradle Configuration Requirements

To support automatic version code and name bumping, you must modify your `app/build.gradle.kts` file. Replace the static `versionCode` and `versionName` variables inside the `defaultConfig` block with the following conditional logic:

```kotlin
android {
    // ...
    defaultConfig {
        // ...
        
        // Define base version
        val baseVersionName = "1.0"
        
        // Check if version overrides are passed from the CI command line
        versionCode = if (project.hasProperty("versionCode")) project.property("versionCode").toString().toInt() else 1
        versionName = if (project.hasProperty("versionName")) project.property("versionName").toString() else baseVersionName
        
        // ...
    }
}
```

---

## 3. GitHub Secrets Configuration

For the **Release Workflow** to sign your app binaries, you must generate a release keystore (`.jks` file) and define these 4 secrets under **Repository Settings** ➔ **Secrets and variables** ➔ **Actions**:

| Secret Name | Description / Extraction Method |
|-------------|---------------------------------|
| `SIGNING_KEY` | The Base64-encoded string of your keystore file. Convert it using:<br>`[Convert]::ToBase64String([IO.File]::ReadAllBytes("your-key.jks"))` |
| `ALIAS` | The key alias name you defined when creating the keystore. |
| `KEY_STORE_PASSWORD` | The password for the keystore file. |
| `KEY_PASSWORD` | The private key password. |

---

## 4. Branch Protection Rules

To prevent code from being merged when tests fail:
1. Go to your GitHub repository -> **Settings** ➔ **Branches**.
2. Click **Add branch protection rule** (or Edit your existing rule).
3. Set the **Branch name pattern** to `main` (or `master`).
4. Check the box **Require status checks to pass before merging**.
5. Search for and select the status check: **`Run Unit Tests`**.
6. Save the changes.
