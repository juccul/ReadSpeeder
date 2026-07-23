package com.jukul.readspeeder

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jukul.readspeeder.ui.components.DocumentCard
import com.jukul.readspeeder.ui.components.ReadSpeederMenu
import com.jukul.readspeeder.ui.components.ReadSpeederTopBar
import com.jukul.readspeeder.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

private val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val ReadSpeederTypography = with(Typography()) {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = InterFontFamily),
        displayMedium = displayMedium.copy(fontFamily = InterFontFamily),
        displaySmall = displaySmall.copy(fontFamily = InterFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = InterFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = InterFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = InterFontFamily),
        titleLarge = titleLarge.copy(fontFamily = InterFontFamily),
        titleMedium = titleMedium.copy(fontFamily = InterFontFamily),
        titleSmall = titleSmall.copy(fontFamily = InterFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = InterFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = InterFontFamily),
        bodySmall = bodySmall.copy(fontFamily = InterFontFamily),
        labelLarge = labelLarge.copy(fontFamily = InterFontFamily),
        labelMedium = labelMedium.copy(fontFamily = InterFontFamily),
        labelSmall = labelSmall.copy(fontFamily = InterFontFamily),
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReadSpeederTheme {
                ReadSpeederScreen()
            }
        }
    }
}

@Composable
private fun ReadSpeederTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ReadSpeederTypography,
        content = content,
    )
}

@Composable
private fun ReadSpeederScreen() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentDestination by remember { mutableStateOf("home") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ReadSpeederMenu(
                currentDestination = currentDestination,
                onDestinationClick = { destination ->
                    currentDestination = destination
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                ReadSpeederTopBar(
                    title = stringResource(
                        if (currentDestination == "settings") {
                            R.string.settings
                        } else {
                            R.string.home
                        },
                    ),
                    showActions = currentDestination == "home",
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    onSearchClick = { },
                    onFilterClick = { },
                )
            },
        ) {innerPadding ->
            when (currentDestination) {
                "home" -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(12) { index ->
                        DocumentCard(
                            title = "Sample Document ${index + 1}",
                            author = "Author ${index + 1}",
                            progress = index * 9,
                            onClick = { },
                        )
                    }
                }
                "settings" -> SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReadSpeederScreenPreview() {
    ReadSpeederTheme {
        ReadSpeederScreen()
    }
}
