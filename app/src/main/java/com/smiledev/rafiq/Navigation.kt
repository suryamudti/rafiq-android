package com.smiledev.rafiq

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.smiledev.rafiq.ui.calendar.IslamicCalendarScreen
import com.smiledev.rafiq.ui.dashboard.DashboardScreen
import com.smiledev.rafiq.ui.quran.QuranScreen
import com.smiledev.rafiq.ui.quran.AyahScreen
import com.smiledev.rafiq.ui.prayertimes.PrayerTimesScreen
import com.smiledev.rafiq.ui.qibla.QiblaScreen
import com.smiledev.rafiq.ui.mosques.MosquesScreen
import com.smiledev.rafiq.ui.prophets.ProphetsScreen
import com.smiledev.rafiq.ui.prophets.ProphetDetailScreen
import com.smiledev.rafiq.ui.recitation.RecitationScreen
import com.smiledev.rafiq.ui.zakat.ZakatCalculatorScreen
import com.smiledev.rafiq.ui.asmaulhusna.AsmaulHusnaScreen
import com.smiledev.rafiq.ui.tasbih.TasbihScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Dashboard)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Dashboard> {
          DashboardScreen(
            onNavigate = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<Quran> {
          QuranScreen(
            onSurahClick = { num, name -> backStack.add(Ayah(num, name)) },
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<Ayah> { key ->
          AyahScreen(
            suraNumber = key.suraNumber,
            suraName = key.suraName,
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
      },
  )
}
