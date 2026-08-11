package com.jukul.readspeeder.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.jukul.readspeeder.R
import com.jukul.readspeeder.data.ThemeMode

private val GoogleSansFontFamily = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_medium, FontWeight.Medium),
    Font(R.font.google_sans_semibold, FontWeight.SemiBold),
    Font(R.font.google_sans_bold, FontWeight.Bold),
)

private val ReadSpeederTypography = with(Typography()) {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = GoogleSansFontFamily),
        displayMedium = displayMedium.copy(fontFamily = GoogleSansFontFamily),
        displaySmall = displaySmall.copy(fontFamily = GoogleSansFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = GoogleSansFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = GoogleSansFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = GoogleSansFontFamily),
        titleLarge = titleLarge.copy(fontFamily = GoogleSansFontFamily),
        titleMedium = titleMedium.copy(fontFamily = GoogleSansFontFamily),
        titleSmall = titleSmall.copy(fontFamily = GoogleSansFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = GoogleSansFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = GoogleSansFontFamily),
        bodySmall = bodySmall.copy(fontFamily = GoogleSansFontFamily),
        labelLarge = labelLarge.copy(fontFamily = GoogleSansFontFamily),
        labelMedium = labelMedium.copy(fontFamily = GoogleSansFontFamily),
        labelSmall = labelSmall.copy(fontFamily = GoogleSansFontFamily),
    )
}

private val DarkMonochromeColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF303030),
    onPrimaryContainer = Color.White,
    inversePrimary = Color.Black,
    secondary = Color(0xFFD0D0D0),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF282828),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFE0E0E0),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF343434),
    onTertiaryContainer = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFC8C8C8),
    surfaceTint = Color.Transparent,
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    error = Color.White,
    onError = Color.Black,
    errorContainer = Color(0xFF343434),
    onErrorContainer = Color.White,
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF3A3A3A),
    surfaceBright = Color(0xFF303030),
    surfaceDim = Color.Black,
    surfaceContainer = Color(0xFF181818),
    surfaceContainerHigh = Color(0xFF222222),
    surfaceContainerHighest = Color(0xFF2C2C2C),
    surfaceContainerLow = Color(0xFF101010),
    surfaceContainerLowest = Color.Black,
).copy(
    primaryFixed = Color.White,
    primaryFixedDim = Color(0xFFD0D0D0),
    onPrimaryFixed = Color.Black,
    onPrimaryFixedVariant = Color(0xFF303030),
    secondaryFixed = Color.White,
    secondaryFixedDim = Color(0xFFD0D0D0),
    onSecondaryFixed = Color.Black,
    onSecondaryFixedVariant = Color(0xFF303030),
    tertiaryFixed = Color.White,
    tertiaryFixedDim = Color(0xFFD0D0D0),
    onTertiaryFixed = Color.Black,
    onTertiaryFixedVariant = Color(0xFF303030),
)

private val LightMonochromeColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0D0D0),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF303030),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = Color.Black,
    tertiary = Color(0xFF202020),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDEDEDE),
    onTertiaryContainer = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE4E4E4),
    onSurfaceVariant = Color(0xFF383838),
    surfaceTint = Color.Transparent,
    error = Color.Black,
    onError = Color.White,
    errorContainer = Color(0xFFDEDEDE),
    onErrorContainer = Color.Black,
    outline = Color(0xFF747474),
    outlineVariant = Color(0xFFC4C4C4),
)

@Composable
internal fun ReadSpeederTheme(
    themeMode: ThemeMode = ThemeMode.System,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val view = LocalView.current
    val colorScheme = remember(context, darkTheme, dynamicColor) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
                dynamicDarkColorScheme(context)
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                dynamicLightColorScheme(context)
            !dynamicColor && darkTheme -> DarkMonochromeColorScheme
            !dynamicColor -> LightMonochromeColorScheme
            darkTheme -> darkColorScheme()
            else -> lightColorScheme()
        }
    }

    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ReadSpeederTypography,
        content = content,
    )
}
