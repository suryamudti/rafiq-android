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
        private const val COLOR_SURFACE_DARK = 0xFF112236.toInt()
        private const val COLOR_TEAL_BRIGHT = 0xFF4DB6AC.toInt()
        private const val COLOR_GOLD_ACCENT = 0xFFFFD54F.toInt()
        private const val COLOR_TEXT_PRIMARY = 0xFFF5F7FA.toInt()
        private const val COLOR_TEXT_SECONDARY = 0xFFB0BEC5.toInt()
        private const val COLOR_TEXT_MUTED = 0xFF78909C.toInt()

        internal fun buildTile(
            packageName: String,
            headerLabel: String,
            snapshot: WearNextPrayerSnapshot
        ): TileBuilders.Tile {
            val rootLayout = buildLayout(
                packageName = packageName,
                headerLabel = headerLabel,
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
            headerLabel: String,
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
                                        .setRadius(dp(16f))
                                        .build()
                                )
                                .build()
                        )
                        .setPadding(
                            ModifiersBuilders.Padding.Builder()
                                .setStart(dp(12f))
                                .setEnd(dp(12f))
                                .setTop(dp(8f))
                                .setBottom(dp(8f))
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
                                .setText("${snapshot.nextPrayerName} • ${snapshot.nextPrayerTime}")
                                .setFontStyle(
                                    LayoutElementBuilders.FontStyle.Builder()
                                        .setSize(sp(15f))
                                        .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                        .setColor(argb(COLOR_TEXT_PRIMARY))
                                        .build()
                                )
                                .setMaxLines(1)
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Spacer.Builder()
                                .setHeight(dp(2f))
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Text.Builder()
                                .setText("-${snapshot.countdown} • ${snapshot.cityName}")
                                .setFontStyle(
                                    LayoutElementBuilders.FontStyle.Builder()
                                        .setSize(sp(11f))
                                        .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                        .setColor(argb(COLOR_GOLD_ACCENT))
                                        .build()
                                )
                                .setMaxLines(1)
                                .build()
                        )
                        .build()
                )
                .build()

            val scheduleRow = LayoutElementBuilders.Row.Builder()
                .setWidth(wrap())
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)

            snapshot.schedule.forEachIndexed { index, item ->
                if (index > 0) {
                    scheduleRow.addContent(
                        LayoutElementBuilders.Spacer.Builder()
                            .setWidth(dp(4f))
                            .build()
                    )
                }
                scheduleRow.addContent(buildMiniPrayerPill(item))
            }

            val column = LayoutElementBuilders.Column.Builder()
                .setWidth(expand())
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(
                    LayoutElementBuilders.Text.Builder()
                        .setText(headerLabel)
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setSize(sp(10f))
                                .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                .setColor(argb(COLOR_TEAL_BRIGHT))
                                .build()
                        )
                        .setMaxLines(1)
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Spacer.Builder()
                        .setHeight(dp(6f))
                        .build()
                )
                .addContent(heroCard)
                .addContent(
                    LayoutElementBuilders.Spacer.Builder()
                        .setHeight(dp(6f))
                        .build()
                )
                .addContent(scheduleRow.build())

            if (snapshot.hijriDate.isNotBlank()) {
                column.addContent(
                    LayoutElementBuilders.Spacer.Builder()
                        .setHeight(dp(5f))
                        .build()
                )
                column.addContent(
                    LayoutElementBuilders.Text.Builder()
                        .setText(snapshot.hijriDate)
                        .setFontStyle(
                            LayoutElementBuilders.FontStyle.Builder()
                                .setSize(sp(9f))
                                .setColor(argb(COLOR_TEXT_SECONDARY))
                                .build()
                        )
                        .setMaxLines(1)
                        .build()
                )
            }

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
                                .setStart(dp(14f))
                                .setEnd(dp(14f))
                                .setTop(dp(16f))
                                .setBottom(dp(16f))
                                .build()
                        )
                        .build()
                )
                .addContent(column.build())
                .build()
        }

        private fun buildMiniPrayerPill(
            item: WearUpcomingPrayerItem
        ): LayoutElementBuilders.LayoutElement {
            val bgColor = if (item.isNext) COLOR_SURFACE_CARD else COLOR_SURFACE_DARK
            val nameColor = when {
                item.isNext -> COLOR_TEAL_BRIGHT
                item.isPassed -> COLOR_TEXT_MUTED
                else -> COLOR_TEXT_SECONDARY
            }
            val timeColor = when {
                item.isNext -> COLOR_GOLD_ACCENT
                item.isPassed -> COLOR_TEXT_MUTED
                else -> COLOR_TEXT_PRIMARY
            }
            val shortLabel = item.name.take(3).uppercase()

            return LayoutElementBuilders.Box.Builder()
                .setWidth(wrap())
                .setHeight(wrap())
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setBackground(
                            ModifiersBuilders.Background.Builder()
                                .setColor(argb(bgColor))
                                .setCorner(
                                    ModifiersBuilders.Corner.Builder()
                                        .setRadius(dp(8f))
                                        .build()
                                )
                                .build()
                        )
                        .setPadding(
                            ModifiersBuilders.Padding.Builder()
                                .setStart(dp(5f))
                                .setEnd(dp(5f))
                                .setTop(dp(4f))
                                .setBottom(dp(4f))
                                .build()
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Column.Builder()
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .addContent(
                            LayoutElementBuilders.Text.Builder()
                                .setText(shortLabel)
                                .setFontStyle(
                                    LayoutElementBuilders.FontStyle.Builder()
                                        .setSize(sp(8f))
                                        .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                        .setColor(argb(nameColor))
                                        .build()
                                )
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Text.Builder()
                                .setText(item.time)
                                .setFontStyle(
                                    LayoutElementBuilders.FontStyle.Builder()
                                        .setSize(sp(9f))
                                        .setWeight(
                                            if (item.isNext) {
                                                LayoutElementBuilders.FONT_WEIGHT_BOLD
                                            } else {
                                                LayoutElementBuilders.FONT_WEIGHT_NORMAL
                                            }
                                        )
                                        .setColor(argb(timeColor))
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        }
    }
}
