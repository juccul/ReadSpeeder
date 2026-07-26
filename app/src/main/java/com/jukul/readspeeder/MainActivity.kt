package com.jukul.readspeeder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jukul.readspeeder.data.SettingsStore
import com.jukul.readspeeder.ui.ReadSpeederApp
import com.jukul.readspeeder.ui.theme.ReadSpeederTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val settingsStore = remember { SettingsStore(applicationContext) }
            var settings by remember { mutableStateOf(settingsStore.load()) }
            ReadSpeederTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
            ) {
                ReadSpeederApp(
                    settings = settings,
                    onSettingsChange = {
                        settings = it.validated()
                        settingsStore.save(settings)
                    },
                )
            }
        }
    }
}
