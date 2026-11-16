package com.smiledev.rafiq_quran.wear.widget

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import com.smiledev.rafiq_quran.wear.R
import com.smiledev.rafiq_quran.wear.WearMainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future

class WearNextPrayerTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> = serviceScope.future {
        val snapshot = WearNextPrayerDataProvider.resolveSnapshot(applicationContext)
        buildTile(
            packageName = packageName,
            headerLabel = getString(R.string.wear_next_prayer).uppercase(),
            snapshot = snapshot
        )
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> = serviceScope.future {
        ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val RESOURCES_VERSION = "1"
        private const val FRESHNESS_INTERVAL_MS = 15 * 60 * 1000L

        private const val COLOR_DEEP_NAVY = 0xFF08121E.toInt()
        private const val COLOR_SURFACE_CARD = 0xFF172D44.toInt()
        private const val COLOR_GOLD_ACCENT = 0xFFFFD54F.toInt()
        private const val COLOR_TEXT_PRIMARY = 0xFFF5F7FA.toInt()

        internal fun buildTile(
            packageName: String,
            @Suppress("UNUSED_PARAMETER") headerLabel: String = "",
            snapshot: WearNextPrayerSnapshot
        ): TileBuilders.Tile {
            val rootLayout = buildLayout(
                packageName = packageName,
                snapshot = snapshot
            )
            val singleEntry = TimelineBuilders.TimelineEntry.Builder()
                .setLayout(
                    LayoutElementBuilders.Layout.Builder()
                        .setRoot(rootLayout)
                        .build()
                )
                .build()

            val timeline = TimelineBuilders.Timeline.Builder()
                .addTimelineEntry(singleEntry)
                .build()

            return TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setFreshnessIntervalMillis(FRESHNESS_INTERVAL_MS)
                .setTileTimeline(timeline)
                .build()
        }

        private fun buildLayout(
            packageName: String,
            snapshot: WearNextPrayerSnapshot
        ): LayoutElementBuilders.LayoutElement {
            val launchClickable = ModifiersBuilders.Clickable.Builder()
                .setId("open_prayer_times")
                .setOnClick(
                    ActionBuilders.LaunchAction.Builder()
                        .setAndroidActivity(
                            ActionBuilders.AndroidActivity.Builder()
                                .setPackageName(packageName)
                                .setClassName(WearMainActivity::class.java.name)
                                .addKeyToExtraMapping(
                                    "start_route",
                                    ActionBuilders.stringExtra("prayer_times")
                                )
                                .build()
                        )
                        .build()
                )
                .build()

            val heroCard = LayoutElementBuilders.Box.Builder()
                .setWidth(expand())
                .setHeight(wrap())
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setClickable(launchClickable)
                        .setBackground(
                            ModifiersBuilders.Background.Builder()
                                .setColor(argb(COLOR_SURFACE_CARD))
                                .setCorner(
                                    ModifiersBuilders.Corner.Builder()
                                        .setRadius(dp(20f))
                                        .build()
                                )
                                .build()
                        )
                        .setPadding(
                            ModifiersBuilders.Padding.Builder()
                                .setStart(dp(16f))
                                .setEnd(dp(16f))
                                .setTop(dp(14f))
                                .setBottom(dp(14f))
                                .build()
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Column.Builder()
                        .setWidth(expand())
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .addContent(
                            LayoutElementBuilders.Text.Builder()
                                .setText(snapshot.nextPrayerName)
                                .setFontStyle(
                                    LayoutElementBuilders.FontStyle.Builder()
                                        .setSize(sp(15f))
                                        .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                        .setColor(argb(COLOR_GOLD_ACCENT))
                                        .build()
                                )
                                .setMaxLines(1)
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Spacer.Builder()
                                .setHeight(dp(4f))
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Text.Builder()
                                .setText(snapshot.nextPrayerTime)
                                .setFontStyle(
                                    LayoutElementBuilders.FontStyle.Builder()
                                        .setSize(sp(24f))
                                        .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                        .setColor(argb(COLOR_TEXT_PRIMARY))
                                        .build()
                                )
                                .setMaxLines(1)
                                .build()
                        )
                        .build()
                )
                .build()

            return LayoutElementBuilders.Box.Builder()
                .setWidth(expand())
                .setHeight(expand())
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setClickable(launchClickable)
                        .setBackground(
                            ModifiersBuilders.Background.Builder()
                                .setColor(argb(COLOR_DEEP_NAVY))
                                .build()
                        )
                        .setPadding(
                            ModifiersBuilders.Padding.Builder()
                                .setStart(dp(16f))
                                .setEnd(dp(16f))
                                .setTop(dp(16f))
                                .setBottom(dp(16f))
                                .build()
                        )
                        .build()
                )
                .addContent(heroCard)
                .build()
        }
    }
}
