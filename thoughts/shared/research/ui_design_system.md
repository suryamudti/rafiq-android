# Rafiq App - UI Design System Spec

This document defines the Design System and UI layout components for the native Android version of the **Rafiq App**, translating the design tokens and visual style of the original Flutter implementation into Material 3 Compose specifications.

---

## 1. Color Palette (Material 3)

The primary theme color is a deep Teal (`#00897B`). Below are the light and dark color schemes mapped to Android Compose Color definitions.

### Light Theme
* **Primary:** `Color(0xFF00897B)` (Teal 600)
* **OnPrimary:** `Color(0xFFFFFFFF)`
* **PrimaryContainer:** `Color(0xFFB2DFDB)` (Teal 100)
* **OnPrimaryContainer:** `Color(0xFF004D40)`
* **Secondary:** `Color(0xFF00796B)`
* **Surface:** `Color(0xFFF5F5F5)` (Grey 100)
* **Background:** `Color(0xFFFFFFFF)`
* **OnSurface:** `Color(0xFF212121)` (Grey 900)
* **OnBackground:** `Color(0xFF212121)`

### Dark Theme
* **Primary:** `Color(0xFF80CBC4)` (Teal 200)
* **OnPrimary:** `Color(0xFF00332D)`
* **PrimaryContainer:** `Color(0xFF004D40)` (Teal 900)
* **OnPrimaryContainer:** `Color(0xFFB2DFDB)`
* **Secondary:** `Color(0xFF4DB6AC)`
* **Surface:** `Color(0xFF121212)`
* **Background:** `Color(0xFF000000)`
* **OnSurface:** `Color(0xFFE0E0E0)` (Grey 300)
* **OnBackground:** `Color(0xFFE0E0E0)`

---

## 2. Typography

The app uses three font families, configured in `assets/fonts/`:

| Font Family | Usage | Font Style |
|-------------|-------|------------|
| **Roboto** (Default) | System titles, body text, buttons, and configurations | Regular, Medium, Bold |
| **me_quran** | Quranic Arabic script text block rendering | Regular (Custom Arabic Glyphs) |
| **arabic_two** | Number overlays inside verse markers | Bold |

### Jetpack Compose Type Mapping
```kotlin
val QuranTypography = Typography(
    // System labels
    bodyLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_bold)),
        fontSize = 22.sp,
        lineHeight = 28.sp
    )
)

// Custom Arabic Text Styles
val ArabicVerseStyle = TextStyle(
    fontFamily = FontFamily(Font(R.font.me_quran)),
    fontSize = 28.sp,
    lineHeight = 44.sp,
    textAlign = TextAlign.Right
)

val ArabicVerseNumberStyle = TextStyle(
    fontFamily = FontFamily(Font(R.font.arabic_two)),
    fontSize = 14.sp
)
```

---

## 3. Dynamic Imagery & Assets

### Dynamic Prayer Time Backgrounds
Based on the active prayer time or countdown state, the dashboard screen background shifts using these local image assets:
- **Subuh:** `assets/images/subuh.jpg`
- **Sunrise:** `assets/images/sunrise.jpg`
- **Dzuhur / Ashr:** `assets/images/prayer_times.jpg`
- **Maghrib:** `assets/images/maghrib.jpg`
- **Isya:** `assets/images/isya.jpg`

### Compass Graphics
The Qibla compass combines two images overlayed in a box:
- **Base Dial (Rotation matches heading):** `assets/images/compass_base.png`
- **Arrow Needle (Points towards Mecca):** `assets/images/compass_arrow.png`
- **Center Marker:** `assets/images/compass_kakbah.png`

---

## 4. Key Custom UI Components

```
┌────────────────────────────────────────────────────────┐
│  Dashboard Header: Prayer Countdown & Hijri Date       │
├────────────────────────────────────────────────────────┤
│  Stats Overview: Days active & Completed prayers row   │
├────────────────────────────────────────────────────────┤
│  Quick Features Menu:                                 │
│  [Quran]   [Prayer Times]   [Qibla]   [Nearby Mosque]  │
└────────────────────────────────────────────────────────┘
```

### A. Quran Ayah Cell (`AyahItemCell`)
- **Structure:** A standard vertical list item containing:
  - Header Row: Ayah coordinates (e.g. `2:255`), Share/Bookmark icon actions, and Juz index.
  - Arabic Text Block: Align Right, `me_quran` font, adjustable font size.
  - Translation Text Block: Align Left, Roboto font, selected translation (EN/ID), adjustable font size.
- **Micro-interactions:** Long press to show bookmark action overlay; subtle ripple effect on tap.

### B. Prayer Tracker Checklist (`PrayerTrackerCard`)
- **Structure:** Daily checkboxes layout representing:
  - Subuh, Dzuhur, Ashr, Maghrib, Isya.
- **Behavior:** Clicking updates local SQLite prayer log history immediately and refreshes the lifetime completion rate chart.

### C. Qibla Digital Compass (`QiblaCompassView`)
- **Compose Canvas or Box Layout:**
  - Standard dial rotates dynamically using an angle animation:
    `val compassRotation by animateFloatAsState(targetValue = -headingDegrees)`
  - Kaaba indicator needle rotates based on calculated spherical trigonometry target bearing:
    `val needleRotation by animateFloatAsState(targetValue = qiblaBearing - headingDegrees)`
