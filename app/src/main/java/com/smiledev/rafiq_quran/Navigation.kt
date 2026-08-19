package com.smiledev.rafiq_quran

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.smiledev.rafiq_quran.ui.calendar.IslamicCalendarScreen
import com.smiledev.rafiq_quran.ui.dashboard.DashboardScreen
import com.smiledev.rafiq_quran.ui.quran.QuranScreen
import com.smiledev.rafiq_quran.ui.quran.AyahScreen
import com.smiledev.rafiq_quran.ui.prayertimes.PrayerTimesScreen
import com.smiledev.rafiq_quran.ui.qibla.QiblaScreen
import com.smiledev.rafiq_quran.ui.mosques.MosquesScreen
import com.smiledev.rafiq_quran.ui.prophets.ProphetsScreen
import com.smiledev.rafiq_quran.ui.prophets.ProphetDetailScreen
import com.smiledev.rafiq_quran.ui.recitation.RecitationScreen
import com.smiledev.rafiq_quran.ui.hadith.HadithBooksScreen
import com.smiledev.rafiq_quran.ui.hadith.HadithSearchScreen
import com.smiledev.rafiq_quran.ui.hadith.HadithListScreen
import com.smiledev.rafiq_quran.ui.hadith.HadithDetailScreen
import com.smiledev.rafiq_quran.ui.zakat.ZakatCalculatorScreen
import com.smiledev.rafiq_quran.ui.asmaulhusna.AsmaulHusnaScreen
import com.smiledev.rafiq_quran.ui.tasbih.TasbihScreen
import com.smiledev.rafiq_quran.ui.bookmarks.BookmarkListFullScreen
import com.smiledev.rafiq_quran.ui.prayerlog.PrayerLogScreen

import com.smiledev.rafiq_quran.ui.settings.SettingsScreen
import com.smiledev.rafiq_quran.ui.sources.SourcesScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Dashboard)

  NavDisplay(
    backStack = backStack,
    onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Dashboard> {
          DashboardScreen(
            onNavigate = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<Quran> { key ->
          QuranScreen(
            initialTab = key.initialTab,
            onSurahClick = { num, name -> backStack.add(Ayah(num, name)) },
            onBookmarkClick = { sura, name, aya ->
              backStack.add(Ayah(suraNumber = sura, suraName = name, scrollToAya = aya))
            },
            onSearchResultClick = { sura, name, aya ->
              backStack.add(Ayah(suraNumber = sura, suraName = name, scrollToAya = aya))
            },
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<Ayah> { key ->
          AyahScreen(
            suraNumber = key.suraNumber,
            suraName = key.suraName,
            scrollToAya = key.scrollToAya,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<PrayerTimes> {
          PrayerTimesScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<Qibla> {
          QiblaScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<Mosques> {
          MosquesScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<Prophets> {
          ProphetsScreen(
            onProphetClick = { id -> backStack.add(ProphetDetail(id)) },
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<ProphetDetail> { key ->
          ProphetDetailScreen(
            prophetId = key.prophetId,
            onBack = { backStack.removeLastOrNull() },
            onVerseRefClick = { surah, surahName, ayaStart ->
              backStack.add(Ayah(suraNumber = surah, suraName = surahName, scrollToAya = ayaStart))
            },
            onProphetNavigate = { id ->
              backStack.removeLastOrNull()
              backStack.add(ProphetDetail(id))
            },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<HadithBooks> {
          HadithBooksScreen(
            onHadithBookClick = { bookId -> backStack.add(HadithList(bookId)) },
            onSearch = { backStack.add(HadithSearch) },
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<HadithSearch> {
          HadithSearchScreen(
            onHadithClick = { id -> backStack.add(HadithDetail(id)) },
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<HadithList> { key ->
          HadithListScreen(
            bookId = key.bookId,
            onHadithClick = { id -> backStack.add(HadithDetail(id)) },
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<HadithDetail> { key ->
          HadithDetailScreen(
            hadithId = key.hadithId,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<Recitation> {
          RecitationScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<IslamicCalendar> {
          IslamicCalendarScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<ZakatCalculator> {
          ZakatCalculatorScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<AsmaulHusna> {
          AsmaulHusnaScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<Tasbih> {
          TasbihScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<BookmarkList> {
          BookmarkListFullScreen(
            onBack = { backStack.removeLastOrNull() },
            onBookmarkClick = { sura, name, aya ->
              backStack.add(Ayah(suraNumber = sura, suraName = name, scrollToAya = aya))
            },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<PrayerLog> {
          PrayerLogScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }

        entry<Settings> {
          SettingsScreen(
            onBack = { backStack.removeLastOrNull() },
            onNavigate = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<Sources> {
          SourcesScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
      },
  )
}
