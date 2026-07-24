package com.jukul.readspeeder

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jukul.readspeeder.ui.components.CollapsedTopBarHeight
import com.jukul.readspeeder.ui.components.DocumentCard
import com.jukul.readspeeder.ui.components.ExpandedTopBarHeight
import com.jukul.readspeeder.ui.components.ReadSpeederMenu
import com.jukul.readspeeder.ui.components.ReadSpeederTopBar
import com.jukul.readspeeder.ui.screens.SettingsScreen
import dev.chrisbanes.haze.blur.HazeProgressive
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadSpeederScreen() {
    val hazeBackgroundColor = MaterialTheme.colorScheme.surface
    val hazeState = rememberHazeState()
    val libraryGridState = rememberLazyGridState()
    val closeNavigationDescription = stringResource(R.string.close_navigation)
    var currentDestination by remember { mutableStateOf("home") }
    var menuExpanded by remember { mutableStateOf(false) }
    val allowTopBarExpansion = remember { mutableStateOf(false) }
    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        canScroll = {
            currentDestination == "home" &&
                (libraryGridState.canScrollForward || libraryGridState.canScrollBackward)
        },
    )
    val topBarScrollConnection = remember(topBarScrollBehavior) {
        val delegate = topBarScrollBehavior.nestedScrollConnection
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f) allowTopBarExpansion.value = false
                return delegate.onPreScroll(available, source)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset =
                if (available.y > 0f && !allowTopBarExpansion.value) {
                    Offset.Zero
                } else {
                    delegate.onPostScroll(consumed, available, source)
                }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity =
                if (available.y > 0f && !allowTopBarExpansion.value) {
                    Velocity.Zero
                } else {
                    delegate.onPostFling(consumed, available)
                }
        }
    }

    BackHandler(enabled = menuExpanded) {
        menuExpanded = false
    }

    Box(Modifier.fillMaxSize()) {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val currentTopBarHeight = statusBarHeight + lerpDp(
            ExpandedTopBarHeight,
            CollapsedTopBarHeight,
            topBarScrollBehavior.state.collapsedFraction,
        )
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentDestination, libraryGridState) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        allowTopBarExpansion.value =
                            currentDestination == "home" &&
                                !libraryGridState.canScrollBackward
                        waitForUpOrCancellation()
                    }
                }
                .nestedScroll(topBarScrollConnection),
            topBar = {
                ReadSpeederTopBar(
                    modifier = Modifier.hazeEffect(state = hazeState) {
                        blurEffect {
                            backgroundColor = hazeBackgroundColor
                            blurRadius = 24.dp
                            progressive = HazeProgressive.verticalGradient(
                                startIntensity = 1f,
                                endIntensity = 0f,
                                preferPerformance = true,
                            )
                        }
                    }.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                hazeBackgroundColor,
                                hazeBackgroundColor.copy(alpha = 0f),
                            ),
                        ),
                    ),
                    scrollBehavior = topBarScrollBehavior,
                    title = stringResource(
                        if (currentDestination == "settings") {
                            R.string.settings
                        } else {
                            R.string.home
                        },
                    ),
                    showActions = currentDestination == "home",
                    onNavigationClick = { menuExpanded = !menuExpanded },
                    onSearchClick = { },
                    onFilterClick = { },
                )
            },
        ) {innerPadding ->
            when (currentDestination) {
                "home" -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = libraryGridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = hazeState),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = innerPadding.calculateTopPadding() + 16.dp,
                        end = 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 16.dp,
                    ),
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

        AnimatedVisibility(
            visible = menuExpanded,
            modifier = Modifier.align(Alignment.TopStart),
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(160)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { menuExpanded = false },
                    )
                    .semantics {
                        contentDescription = closeNavigationDescription
                    },
            )
        }

        AnimatedVisibility(
            visible = menuExpanded,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = 8.dp,
                    y = currentTopBarHeight,
                ),
            enter = EnterTransition.None,
            exit = ExitTransition.None,
        ) {
            val menuExpansion by transition.animateFloat(
                transitionSpec = { tween(280, easing = FastOutSlowInEasing) },
                label = "menu expansion",
            ) { state ->
                if (state == EnterExitState.Visible) 1f else 0f
            }
            val cornerRadius by transition.animateDp(
                transitionSpec = { tween(280, easing = FastOutSlowInEasing) },
                label = "menu corners",
            ) { state ->
                if (state == EnterExitState.Visible) 28.dp else 24.dp
            }
            val surfaceAlpha by transition.animateFloat(
                transitionSpec = { tween(120) },
                label = "menu surface",
            ) { state ->
                if (state == EnterExitState.Visible) 1f else 0f
            }
            val contentAlpha by transition.animateFloat(
                transitionSpec = {
                    if (targetState == EnterExitState.Visible) {
                        tween(durationMillis = 120, delayMillis = 140)
                    } else {
                        tween(durationMillis = 80)
                    }
                },
                label = "menu content",
            ) { state ->
                if (state == EnterExitState.Visible) 1f else 0f
            }
            val menuShape = RoundedCornerShape(cornerRadius)

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = surfaceAlpha
                    }
                    .clip(menuShape)
                    .hazeEffect(state = hazeState) {
                        blurEffect {
                            backgroundColor = hazeBackgroundColor
                            blurRadius = 28.dp
                        }
                    }
                    .background(hazeBackgroundColor.copy(alpha = 0.68f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        shape = menuShape,
                    )
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val collapsedSize = 48.dp.roundToPx()
                        layout(
                            width = lerp(collapsedSize, placeable.width, menuExpansion),
                            height = lerp(collapsedSize, placeable.height, menuExpansion),
                        ) {
                            placeable.placeRelative(0, 0)
                        }
                    },
            ) {
                ReadSpeederMenu(
                    currentDestination = currentDestination,
                    onDestinationClick = { destination ->
                        currentDestination = destination
                        topBarScrollBehavior.state.heightOffset = 0f
                        menuExpanded = false
                    },
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = contentAlpha
                        },
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
