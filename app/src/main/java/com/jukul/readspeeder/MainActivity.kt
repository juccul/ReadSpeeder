package com.jukul.readspeeder

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.tooling.preview.Preview
import com.jukul.readspeeder.ui.components.ReadSpeederMenu
import com.jukul.readspeeder.ui.components.ReadSpeederTopBar
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.padding
import com.jukul.readspeeder.ui.screens.SettingsScreen
import androidx.compose.ui.res.stringResource

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
                "settings" -> SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                )
            } }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReadSpeederScreenPreview() {
    ReadSpeederTheme {
        ReadSpeederScreen()
    }
}
