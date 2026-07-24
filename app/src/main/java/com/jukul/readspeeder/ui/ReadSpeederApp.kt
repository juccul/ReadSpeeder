package com.jukul.readspeeder.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.jukul.readspeeder.R
import com.jukul.readspeeder.ui.components.AddContentButton
import com.jukul.readspeeder.ui.components.CollapsedTopBarHeight
import com.jukul.readspeeder.ui.components.ExpandedTopBarHeight
import com.jukul.readspeeder.ui.components.NavigationMenuOverlay
import com.jukul.readspeeder.ui.components.ReadSpeederTopBar
import com.jukul.readspeeder.ui.screens.LibraryScreen
import com.jukul.readspeeder.ui.screens.SettingsScreen
import com.jukul.readspeeder.ui.theme.ReadSpeederTheme
import dev.chrisbanes.haze.blur.HazeProgressive
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

internal enum class AppDestination(
    val titleRes: Int,
    val icon: ImageVector,
) {
    Library(R.string.library, Icons.Default.Home),
    Settings(R.string.settings, Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadSpeederApp() {
    val hazeBackgroundColor = MaterialTheme.colorScheme.surface
    val hazeState = rememberHazeState()
    val libraryGridState = rememberLazyGridState()
    var currentDestination by remember { mutableStateOf(AppDestination.Library) }
    var navigationMenuExpanded by remember { mutableStateOf(false) }
    var addContentMenuExpanded by remember { mutableStateOf(false) }
    var navigationMenuBounds by remember { mutableStateOf(Rect.Zero) }
    var addContentMenuBounds by remember { mutableStateOf(Rect.Zero) }
    val allowTopBarExpansion = remember { mutableStateOf(false) }
    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        canScroll = {
            currentDestination == AppDestination.Library &&
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

    BackHandler(enabled = navigationMenuExpanded || addContentMenuExpanded) {
        if (addContentMenuExpanded) {
            addContentMenuExpanded = false
        } else {
            navigationMenuExpanded = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    if (navigationMenuExpanded && down.position !in navigationMenuBounds) {
                        navigationMenuExpanded = false
                    }
                    if (addContentMenuExpanded && down.position !in addContentMenuBounds) {
                        addContentMenuExpanded = false
                    }
                }
            },
    ) {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val currentTopBarHeight = statusBarHeight + lerp(
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
                            currentDestination == AppDestination.Library &&
                                !libraryGridState.canScrollBackward
                        waitForUpOrCancellation()
                    }
                }
                .nestedScroll(topBarScrollConnection),
            topBar = {
                ReadSpeederTopBar(
                    scrollBehavior = topBarScrollBehavior,
                    title = stringResource(currentDestination.titleRes),
                    showActions = currentDestination == AppDestination.Library,
                    onNavigationClick = {
                        navigationMenuExpanded = !navigationMenuExpanded
                    },
                    onSearchClick = { },
                    onFilterClick = { },
                    modifier = Modifier
                        .hazeEffect(state = hazeState) {
                            blurEffect {
                                backgroundColor = hazeBackgroundColor
                                blurRadius = 24.dp
                                progressive = HazeProgressive.verticalGradient(
                                    startIntensity = 1f,
                                    endIntensity = 0f,
                                    preferPerformance = true,
                                )
                            }
                        }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    hazeBackgroundColor,
                                    hazeBackgroundColor.copy(alpha = 0f),
                                ),
                            ),
                        ),
                )
            },
        ) { innerPadding ->
            when (currentDestination) {
                AppDestination.Library -> LibraryScreen(
                    state = libraryGridState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = innerPadding.calculateTopPadding() + 16.dp,
                        end = 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 88.dp,
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = hazeState),
                )

                AppDestination.Settings -> SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        if (currentDestination == AppDestination.Library && !navigationMenuExpanded) {
            AddContentButton(
                expanded = addContentMenuExpanded,
                hazeState = hazeState,
                backgroundColor = hazeBackgroundColor,
                onExpandedChange = { addContentMenuExpanded = it },
                onBoundsChanged = { addContentMenuBounds = it },
                onPasteText = { },
                onAddDocument = { },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
            )
        }

        NavigationMenuOverlay(
            expanded = navigationMenuExpanded,
            currentDestination = currentDestination,
            topOffset = currentTopBarHeight,
            hazeState = hazeState,
            backgroundColor = hazeBackgroundColor,
            onBoundsChanged = { navigationMenuBounds = it },
            onDestinationSelected = { destination ->
                currentDestination = destination
                topBarScrollBehavior.state.heightOffset = 0f
                navigationMenuExpanded = false
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReadSpeederAppPreview() {
    ReadSpeederTheme {
        ReadSpeederApp()
    }
}
