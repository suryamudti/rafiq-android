# Keep Retrofit / Gson remote DTOs and domain models consumed across modules
-keep class com.smiledev.rafiq_quran.data.remote.** { *; }
-keep class com.smiledev.rafiq_quran.domain.model.** { *; }

# Keep private/internal Gson model classes used via TypeToken reflection (asset JSON parsing)
-keep class com.smiledev.rafiq_quran.data.repository.ReciterRaw { *; }
-keep class com.smiledev.rafiq_quran.data.repository.AsmaulHusnaRaw { *; }
-keep class com.smiledev.rafiq_quran.data.repository.PrayerGuidanceRaw { *; }
-keep class com.smiledev.rafiq_quran.data.repository.PrayerStepRaw { *; }
-keep class com.smiledev.rafiq_quran.data.repository.SunnahGuidanceRaw { *; }
-keep class com.smiledev.rafiq_quran.data.repository.SunnahStepRaw { *; }
-keep class com.smiledev.rafiq_quran.data.repository.TasbihItemRaw { *; }
-keep class com.smiledev.rafiq_quran.data.repository.TasbihResponse { *; }
