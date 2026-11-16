package com.smiledev.rafiq_quran.wear.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.smiledev.rafiq_quran.wear.WearMainActivity
import com.smiledev.rafiq_quran.wear.navigation.WearRoutes

class WearNextPrayerComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val previewSnapshot = WearNextPrayerCalculator.defaultPreviewSnapshot()
        return buildComplicationData(
            context = this,
            type = type,
            snapshot = previewSnapshot,
            includeTapAction = false
        )
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val snapshot = WearNextPrayerDataProvider.resolveSnapshot(applicationContext)
        return buildComplicationData(
            context = applicationContext,
            type = request.complicationType,
            snapshot = snapshot,
            includeTapAction = true
        )
    }

    companion object {
        private const val REQUEST_CODE_OPEN_PRAYER_TIMES = 4101

        internal fun createLaunchPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, WearMainActivity::class.java).apply {
                putExtra("start_route", WearRoutes.PRAYER_TIMES)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                REQUEST_CODE_OPEN_PRAYER_TIMES,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        internal fun buildComplicationData(
            context: Context,
            type: ComplicationType,
            snapshot: WearNextPrayerSnapshot,
            includeTapAction: Boolean = true
        ): ComplicationData? {
            val descriptionText = PlainComplicationText.Builder(
                "${snapshot.nextPrayerName} ${snapshot.nextPrayerTime} (-${snapshot.countdown})"
            ).build()

            val monoImage = MonochromaticImage.Builder(
                Icon.createWithResource(context, snapshot.iconResId)
            ).build()

            val tapAction = if (includeTapAction) {
                runCatching { createLaunchPendingIntent(context) }.getOrNull()
            } else {
                null
            }

            return when (type) {
                ComplicationType.SHORT_TEXT -> {
                    ShortTextComplicationData.Builder(
                        text = PlainComplicationText.Builder(snapshot.nextPrayerTime).build(),
                        contentDescription = descriptionText
                    )
                        .setTitle(PlainComplicationText.Builder(snapshot.nextPrayerName).build())
                        .setMonochromaticImage(monoImage)
                        .setTapAction(tapAction)
                        .build()
                }

                ComplicationType.LONG_TEXT -> {
                    LongTextComplicationData.Builder(
                        text = PlainComplicationText.Builder(
                            "${snapshot.nextPrayerName} • ${snapshot.nextPrayerTime}"
                        ).build(),
                        contentDescription = descriptionText
                    )
                        .setTitle(
                            PlainComplicationText.Builder(
                                "-${snapshot.countdown} • ${snapshot.cityName}"
                            ).build()
                        )
                        .setMonochromaticImage(monoImage)
                        .setTapAction(tapAction)
                        .build()
                }

                else -> null
            }
        }
    }
}
