package com.crochetquest.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.crochetquest.app.data.repository.AppSettings
import com.crochetquest.app.data.repository.ThemeMode
import com.crochetquest.app.ui.navigation.CrochetQuestRoot
import com.crochetquest.app.ui.theme.CozyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = (application as CrochetQuestApp).container.settingsRepository
        setContent {
            val appSettings by settings.settings.collectAsState(initial = AppSettings())
            val systemDark = isSystemInDarkTheme()
            val dark = when (appSettings.themeMode) {
                ThemeMode.NIGHT -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }
            // Keep the status and navigation bar icons readable on both themes.
            DisposableEffect(dark) {
                val bars = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                onDispose { }
            }
            CozyTheme(darkTheme = dark) {
                CrochetQuestRoot()
            }
        }
    }
}
