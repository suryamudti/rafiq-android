package com.smiledev.rafiq_quran

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import com.smiledev.rafiq_quran.data.preferences.PreferencesManager
import com.smiledev.rafiq_quran.theme.RafiqAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var preferencesManager: PreferencesManager

  private val activeTabState = androidx.compose.runtime.mutableStateOf<com.smiledev.rafiq_quran.ui.RafiqAppTab?>(null)

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(newBase)
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    intent.getStringExtra("TARGET_TAB")?.let { tabStr ->
      val tab = runCatching { com.smiledev.rafiq_quran.ui.RafiqAppTab.valueOf(tabStr) }.getOrNull()
      if (tab != null) activeTabState.value = tab
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    intent?.getStringExtra("TARGET_TAB")?.let { tabStr ->
      val tab = runCatching { com.smiledev.rafiq_quran.ui.RafiqAppTab.valueOf(tabStr) }.getOrNull()
      if (tab != null) activeTabState.value = tab
    }

    enableEdgeToEdge()
    setContent {
      val themeMode by preferencesManager.themeMode.collectAsState(initial = "system")
      val translationLanguage by preferencesManager.translationLanguage.collectAsState(initial = "system")

      val context = LocalContext.current
      val localizedContext = remember(translationLanguage, context) {
          context.wrapLocale(translationLanguage)
      }
      val localizedConfiguration = remember(translationLanguage, localizedContext) {
          localizedContext.resources.configuration
      }

      CompositionLocalProvider(
          LocalContext provides localizedContext,
          LocalConfiguration provides localizedConfiguration
      ) {
          RafiqAppTheme(themeMode = themeMode) {
              Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                  val activeTab by activeTabState
                  if (activeTab != null) {
                      androidx.compose.runtime.key(activeTab) {
                          com.smiledev.rafiq_quran.ui.RafiqAppRoot(initialTab = activeTab!!)
                      }
                  } else {
                      MainNavigation()
                  }
              }
          }
      }
    }
  }
}