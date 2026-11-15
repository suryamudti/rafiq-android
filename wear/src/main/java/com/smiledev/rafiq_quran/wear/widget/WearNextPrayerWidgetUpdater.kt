package com.smiledev.rafiq_quran.wear.widget

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

object WearNextPrayerWidgetUpdater {

    @JvmStatic
    fun requestUpdate(context: Context) {
        val appContext = context.applicationContext ?: context
        runCatching {
            val componentName = ComponentName(appContext, WearNextPrayerComplicationService::class.java)
            ComplicationDataSourceUpdateRequester
                .create(appContext, componentName)
                .requestUpdateAll()
        }
        runCatching {
            TileService.getUpdater(appContext)
                .requestUpdate(WearNextPrayerTileService::class.java)
        }
    }
}
