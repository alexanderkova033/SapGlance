package com.healthwidget.app.settings.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.healthwidget.app.HealthWidgetApp
import com.healthwidget.app.settings.presentation.theme.HealthWidgetTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as HealthWidgetApp).container
        setContent {
            HealthWidgetTheme {
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
