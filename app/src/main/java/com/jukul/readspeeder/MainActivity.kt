package com.jukul.readspeeder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jukul.readspeeder.ui.ReadSpeederApp
import com.jukul.readspeeder.ui.theme.ReadSpeederTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            ReadSpeederTheme { ReadSpeederApp() }
        }
    }
}
