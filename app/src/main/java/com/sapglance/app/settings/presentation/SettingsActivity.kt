package com.sapglance.app.settings.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sapglance.app.SapGlanceApp
import com.sapglance.app.settings.presentation.theme.SapGlanceTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as SapGlanceApp).container
        setContent {
            SapGlanceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        settingsRepository = container.settingsRepository,
                        tipHistoryRepository = container.tipHistoryRepository,
                        tipEngine = container.tipEngine,
                    )
                }
            }
        }
    }
}
