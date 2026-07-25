package com.jukul.readspeeder.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.jukul.readspeeder.R
import com.jukul.readspeeder.data.DocumentImporter
import com.jukul.readspeeder.data.ReadDocument
import com.jukul.readspeeder.ui.components.AddContentButton
import com.jukul.readspeeder.ui.components.CollapsedTopBarHeight
import com.jukul.readspeeder.ui.components.ExpandedTopBarHeight
import com.jukul.readspeeder.ui.components.NavigationMenuOverlay
import com.jukul.readspeeder.ui.components.ReadSpeederTopBar
import com.jukul.readspeeder.ui.screens.LibraryScreen
import com.jukul.readspeeder.ui.screens.PasteTextScreen
import com.jukul.readspeeder.ui.screens.ReaderScreen
import com.jukul.readspeeder.ui.screens.SettingsScreen
import com.jukul.readspeeder.ui.theme.ReadSpeederTheme
import dev.chrisbanes.haze.blur.HazeProgressive
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private val SupportedDocumentTypes = arrayOf(
    "text/plain",
    "application/pdf",
    "application/epub+zip",
)

internal enum class AppDestination(val titleRes: Int, val icon: ImageVector) {
    Library(R.string.library, Icons.Default.Home),
    Settings(R.string.settings, Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadSpeederApp() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val hazeBackgroundColor = MaterialTheme.colorScheme.surface
    val hazeState = rememberHazeState()
    val snackbarHostState = remember { SnackbarHostState() }
    val libraryGridState = rememberLazyGridState()
    val documents = remember { mutableStateListOf<ReadDocument>() }
    var currentDestination by remember { mutableStateOf(AppDestination.Library) }
    var openedDocument by remember { mutableStateOf<ReadDocument?>(null) }
    var pastingText by remember { mutableStateOf(false) }
    var navigationMenuExpanded by remember { mutableStateOf(false) }
    var addContentMenuExpanded by remember { mutableStateOf(false) }
    var navigationMenuBounds by remember { mutableStateOf(Rect.Zero) }
    var addContentMenuBounds by remember { mutableStateOf(Rect.Zero) }
    val pageState = Triple(currentDestination, openedDocument?.id, pastingText)
    val libraryAtTop by remember {
        derivedStateOf {
            libraryGridState.firstVisibleItemIndex == 0 &&
                libraryGridState.firstVisibleItemScrollOffset == 0
        }
    }
    val documentPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        val document = withContext(Dispatchers.IO) {
                            DocumentImporter.import(context.contentResolver, uri)
                        }
                        documents.removeAll { it.id == document.id }
                        documents.add(0, document)
                        snackbarHostState.showSnackbar(
                            resources.getString(R.string.document_imported, document.title),
                        )
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        snackbarHostState.showSnackbar(
                            error.message ?: resources.getString(R.string.document_import_failed),
                        )
                    }
                }
            }
        }
    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        canScroll = {
            currentDestination == AppDestination.Library &&
                openedDocument == null &&
                !pastingText
        },
    )
    val topBarScrollConnection = remember(topBarScrollBehavior) {
        val delegate = topBarScrollBehavior.nestedScrollConnection
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ) = delegate.onPreScroll(available, source)

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset =
                if (
                    available.y > 0f &&
                    (
                        libraryGridState.firstVisibleItemIndex != 0 ||
                            libraryGridState.firstVisibleItemScrollOffset != 0
                    )
                ) {
                    Offset.Zero
                } else {
                    delegate.onPostScroll(consumed, available, source)
                }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity =
                if (
                    available.y > 0f &&
                    (
                        libraryGridState.firstVisibleItemIndex != 0 ||
                            libraryGridState.firstVisibleItemScrollOffset != 0
                    )
                ) {
                    Velocity.Zero
                } else {
                    delegate.onPostFling(consumed, available)
                }
        }
    }
    LaunchedEffect(pageState) {
        if (
            pageState.first == AppDestination.Library &&
            pageState.second == null &&
            !pageState.third &&
            !libraryAtTop
        ) {
            topBarScrollBehavior.state.heightOffset =
                topBarScrollBehavior.state.heightOffsetLimit
        }
    }

    BackHandler(
        enabled =
            navigationMenuExpanded || addContentMenuExpanded ||
                openedDocument != null || pastingText ||
                currentDestination != AppDestination.Library,
    ) {
        if (addContentMenuExpanded) {
            addContentMenuExpanded = false
        } else if (navigationMenuExpanded) {
            navigationMenuExpanded = false
        } else {
            openedDocument = null
            pastingText = false
            currentDestination = AppDestination.Library
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
                    val dismissNavigation =
                        navigationMenuExpanded && down.position !in navigationMenuBounds
                    val dismissAddContent =
                        addContentMenuExpanded && down.position !in addContentMenuBounds
                    if (dismissNavigation) {
                        navigationMenuExpanded = false
                    }
                    if (dismissAddContent) {
                        addContentMenuExpanded = false
                    }
                    if (dismissNavigation || dismissAddContent) {
                        down.consume()
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }
            },
    ) {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val currentTopBarHeight = statusBarHeight + lerp(
            ExpandedTopBarHeight,
            CollapsedTopBarHeight,
            if (
                openedDocument != null || pastingText ||
                currentDestination == AppDestination.Settings
            ) 1f else topBarScrollBehavior.state.collapsedFraction,
        )

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(topBarScrollConnection),
            topBar = {
                Box(
                    modifier = Modifier
                        .height(currentTopBarHeight)
                        .clipToBounds(),
                ) {
                    AnimatedContent(
                        targetState = pageState,
                        transitionSpec = {
                            fadeIn(tween(220))
                                .togetherWith(fadeOut(tween(140)))
                                .using(null)
                        },
                        label = "top bar",
                    ) { (destination, documentId, showPasteText) ->
                        val document = documents.firstOrNull { it.id == documentId }
                        ReadSpeederTopBar(
                            scrollBehavior = topBarScrollBehavior,
                            title = document?.title
                                ?: stringResource(
                                    if (showPasteText) R.string.paste_text
                                    else destination.titleRes,
                                ),
                            subtitle = document?.author,
                            showActions =
                                destination == AppDestination.Library &&
                                    documentId == null && !showPasteText,
                            showBackNavigation =
                                documentId != null || showPasteText ||
                                    destination == AppDestination.Settings,
                            onNavigationClick = {
                                when {
                                    openedDocument != null -> openedDocument = null
                                    pastingText -> pastingText = false
                                    currentDestination == AppDestination.Settings ->
                                        currentDestination = AppDestination.Library
                                    else -> navigationMenuExpanded = !navigationMenuExpanded
                                }
                            },
                            onSearchClick = { },
                            onFilterClick = { },
                            modifier = Modifier
                                .hazeEffect(state = hazeState) {
                                    blurEffect {
                                        backgroundColor = hazeBackgroundColor
                                        blurRadius = 24.dp
                                        progressive = HazeProgressive.verticalGradient(
                                            easing = FastOutLinearInEasing,
                                            startIntensity = 1f,
                                            endIntensity = 0f,
                                            preferPerformance = true,
                                        )
                                    }
                                }
                                .background(
                                    Brush.verticalGradient(
                                        0f to hazeBackgroundColor,
                                        0.25f to hazeBackgroundColor.copy(alpha = 0.7f),
                                        1f to hazeBackgroundColor.copy(alpha = 0f),
                                    ),
                                ),
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            AnimatedContent(
                targetState = pageState,
                transitionSpec = {
                    val direction = when {
                        targetState.second != null || targetState.third -> 1
                        initialState.second != null || initialState.third -> -1
                        targetState.first.ordinal > initialState.first.ordinal -> 1
                        else -> -1
                    }
                    (
                        slideInHorizontally { direction * it / 8 } + fadeIn()
                    ).togetherWith(
                        slideOutHorizontally { -direction * it / 8 } + fadeOut(),
                    )
                },
                label = "page",
            ) { (destination, documentId, showPasteText) ->
                if (documentId != null) {
                    documents.firstOrNull { it.id == documentId }?.let { document ->
                        ReaderScreen(
                            document = document,
                            hazeState = hazeState,
                            backgroundColor = hazeBackgroundColor,
                            topPadding = innerPadding.calculateTopPadding(),
                            onProgressChange = { progress ->
                                val index = documents.indexOfFirst { it.id == document.id }
                                if (index >= 0 && documents[index].progress != progress) {
                                    documents[index] = documents[index].copy(progress = progress)
                                }
                            },
                        )
                    }
                } else if (showPasteText) {
                    PasteTextScreen(
                        onRead = { text ->
                            val document = ReadDocument(
                                id = UUID.randomUUID().toString(),
                                title = resources.getString(R.string.paste_text),
                                author = null,
                                text = text,
                            )
                            documents.add(0, document)
                            pastingText = false
                            openedDocument = document
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                } else when (destination) {
                    AppDestination.Library -> LibraryScreen(
                        state = libraryGridState,
                        documents = documents,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = innerPadding.calculateTopPadding() + 16.dp,
                            end = 16.dp,
                            bottom = innerPadding.calculateBottomPadding() + 88.dp,
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(state = hazeState),
                        onDocumentClick = {
                            openedDocument = it
                        },
                    )

                    AppDestination.Settings -> SettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }

        if (
            currentDestination == AppDestination.Library &&
            openedDocument == null &&
            !pastingText &&
            !navigationMenuExpanded
        ) {
            AddContentButton(
                expanded = addContentMenuExpanded,
                hazeState = hazeState,
                backgroundColor = hazeBackgroundColor,
                onExpandedChange = { addContentMenuExpanded = it },
                onBoundsChanged = { addContentMenuBounds = it },
                onPasteText = {
                    pastingText = true
                },
                onAddDocument = { documentPicker.launch(SupportedDocumentTypes) },
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
                openedDocument = null
                pastingText = false
                navigationMenuExpanded = false
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReadSpeederAppPreview() = ReadSpeederTheme { ReadSpeederApp() }
