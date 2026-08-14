package com.jukul.readspeeder.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import android.os.Trace
import com.jukul.readspeeder.R
import com.jukul.readspeeder.data.AppSettings
import com.jukul.readspeeder.data.DocumentImporter
import com.jukul.readspeeder.data.DocumentStore
import com.jukul.readspeeder.data.LibraryStorage
import com.jukul.readspeeder.data.LibraryDocument
import com.jukul.readspeeder.data.ReadDocument
import com.jukul.readspeeder.data.progressPercent
import com.jukul.readspeeder.data.toLibraryDocument
import com.jukul.readspeeder.ui.components.AddContentButton
import com.jukul.readspeeder.ui.components.CollapsedTopBarHeight
import com.jukul.readspeeder.ui.components.DocumentMenuOverlay
import com.jukul.readspeeder.ui.components.ExpandedTopBarHeight
import com.jukul.readspeeder.ui.components.NavigationMenuOverlay
import com.jukul.readspeeder.ui.components.ReadSpeederTopBar
import com.jukul.readspeeder.ui.components.SortMenuOverlay
import com.jukul.readspeeder.ui.screens.LibraryScreen
import com.jukul.readspeeder.ui.screens.InfoScreen
import com.jukul.readspeeder.ui.screens.PasteTextScreen
import com.jukul.readspeeder.ui.screens.ReaderScreen
import com.jukul.readspeeder.ui.screens.SettingsScreen
import com.jukul.readspeeder.ui.theme.ReadSpeederTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeProgressive
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
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
    Info(R.string.info, Icons.Default.Info),
}

private enum class DocumentEditField { Title, Author }

private data class AppPageState(
    val destination: AppDestination,
    val documentId: String?,
    val showPasteText: Boolean,
    val hazeState: HazeState,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadSpeederApp(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val hazeBackgroundColor = MaterialTheme.colorScheme.surface
    val snackbarHostState = remember { SnackbarHostState() }
    val libraryGridState = rememberLazyGridState()
    val documentStore = remember { DocumentStore(context.applicationContext) }
    val documents = remember { mutableStateListOf<LibraryDocument>() }
    var openedDocument by remember { mutableStateOf<ReadDocument?>(null) }
    var libraryLoading by remember { mutableStateOf(true) }
    var libraryStorage by remember { mutableStateOf(LibraryStorage(0, 0)) }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.Library) }
    var openedDocumentId by rememberSaveable {
        mutableStateOf(documentStore.activeDocumentId())
    }
    var pastingText by rememberSaveable { mutableStateOf(false) }
    var librarySearchQuery by rememberSaveable { mutableStateOf<String?>(null) }
    var readerHeld by remember { mutableStateOf(false) }
    var navigationMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var documentMenuExpanded by remember { mutableStateOf(false) }
    var documentMenuId by remember { mutableStateOf<String?>(null) }
    var documentMenuAnchor by remember { mutableStateOf(Offset.Zero) }
    var editingDocumentField by remember { mutableStateOf<DocumentEditField?>(null) }
    var editDocumentValue by remember { mutableStateOf("") }
    var confirmDocumentDelete by remember { mutableStateOf(false) }
    var addContentMenuExpanded by remember { mutableStateOf(false) }
    var navigationMenuBounds by remember { mutableStateOf(Rect.Zero) }
    var sortMenuBounds by remember { mutableStateOf(Rect.Zero) }
    var documentMenuBounds by remember { mutableStateOf(Rect.Zero) }
    var addContentMenuBounds by remember { mutableStateOf(Rect.Zero) }
    val pageState = remember(currentDestination, openedDocumentId, pastingText) {
        AppPageState(
            destination = currentDestination,
            documentId = openedDocumentId,
            showPasteText = pastingText,
            hazeState = HazeState(),
        )
    }
    val libraryAtTop by remember {
        derivedStateOf {
            libraryGridState.firstVisibleItemIndex == 0 &&
                libraryGridState.firstVisibleItemScrollOffset == 0
        }
    }
    val librarySearchActive =
        currentDestination == AppDestination.Library &&
            openedDocumentId == null && !pastingText && librarySearchQuery != null
    LaunchedEffect(Unit) {
        val restored = withContext(Dispatchers.IO) {
            traced("summary restoration") { documentStore.loadSummaries() }
        }
        documents.addAll(restored.documents)
        if (openedDocumentId != null && restored.documents.none { it.id == openedDocumentId }) {
            documentStore.setActiveDocument(null)
            openedDocumentId = null
        }
        libraryStorage = withContext(Dispatchers.IO) { documentStore.storage() }
        libraryLoading = false
        if (restored.failedCount > 0) {
            snackbarHostState.showSnackbar(
                resources.getString(R.string.library_restore_failed),
            )
        }
    }
    LaunchedEffect(openedDocumentId) {
        val id = openedDocumentId
        if (id == null) {
            openedDocument = null
        } else {
            openedDocument = null
            try {
                openedDocument = withContext(Dispatchers.IO) {
                    traced("full document loading") { documentStore.load(id) }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                documentStore.setActiveDocument(null)
                openedDocumentId = null
                snackbarHostState.showSnackbar(
                    error.message ?: resources.getString(R.string.document_import_failed),
                )
            }
        }
    }
    val documentPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        val document = withContext(Dispatchers.IO) {
                            DocumentImporter.import(context.contentResolver, uri).also(
                                documentStore::save,
                            )
                        }
                        documents.removeAll { it.id == document.id }
                        documents.add(0, document.toLibraryDocument())
                        libraryStorage = withContext(Dispatchers.IO) {
                            documentStore.storage()
                        }
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
        canScroll = { openedDocumentId == null && !pastingText },
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
                    currentDestination == AppDestination.Library &&
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
                    currentDestination == AppDestination.Library &&
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
            pageState.destination == AppDestination.Library &&
            pageState.documentId == null &&
            !pageState.showPasteText &&
            !libraryAtTop
        ) {
            topBarScrollBehavior.state.heightOffset =
                topBarScrollBehavior.state.heightOffsetLimit
        }
    }

    BackHandler(
        enabled =
            navigationMenuExpanded || sortMenuExpanded || documentMenuExpanded ||
                addContentMenuExpanded ||
                librarySearchActive ||
                openedDocumentId != null || pastingText ||
                currentDestination != AppDestination.Library,
    ) {
        if (addContentMenuExpanded) {
            addContentMenuExpanded = false
        } else if (documentMenuExpanded) {
            documentMenuExpanded = false
        } else if (sortMenuExpanded) {
            sortMenuExpanded = false
        } else if (navigationMenuExpanded) {
            navigationMenuExpanded = false
        } else if (librarySearchActive) {
            librarySearchQuery = null
        } else {
            documentStore.setActiveDocument(null)
            openedDocumentId = null
            pastingText = false
            currentDestination = AppDestination.Library
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(openedDocumentId) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    val dismissNavigation =
                        navigationMenuExpanded && down.position !in navigationMenuBounds
                    val dismissSort = sortMenuExpanded && down.position !in sortMenuBounds
                    val dismissDocumentMenu =
                        documentMenuExpanded && down.position !in documentMenuBounds
                    val dismissAddContent =
                        addContentMenuExpanded && down.position !in addContentMenuBounds
                    val holdingReader = openedDocumentId != null
                    if (holdingReader) readerHeld = true
                    if (dismissNavigation) {
                        navigationMenuExpanded = false
                    }
                    if (dismissAddContent) {
                        addContentMenuExpanded = false
                    }
                    if (dismissSort) {
                        sortMenuExpanded = false
                    }
                    if (dismissDocumentMenu) {
                        documentMenuExpanded = false
                    }
                    if (
                        dismissNavigation || dismissSort || dismissDocumentMenu ||
                        dismissAddContent
                    ) {
                        down.consume()
                    }
                    try {
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (
                                dismissNavigation || dismissSort || dismissDocumentMenu ||
                                dismissAddContent
                            ) {
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    } finally {
                        if (holdingReader) readerHeld = false
                    }
                }
            },
    ) {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val libraryTopBarHeight = statusBarHeight + lerp(
            ExpandedTopBarHeight,
            CollapsedTopBarHeight,
            topBarScrollBehavior.state.collapsedFraction,
        )
        val collapsedTopBarHeight = statusBarHeight + CollapsedTopBarHeight

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(topBarScrollConnection),
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            AnimatedContent(
                targetState = pageState,
                transitionSpec = {
                    val direction = when {
                        targetState.documentId != null || targetState.showPasteText -> 1
                        initialState.documentId != null || initialState.showPasteText -> -1
                        targetState.destination.ordinal > initialState.destination.ordinal -> 1
                        else -> -1
                    }
                    val exit =
                        if (
                            initialState.documentId != null &&
                            targetState.documentId == null &&
                            targetState.destination == AppDestination.Library
                        ) {
                            ExitTransition.None
                        } else {
                            slideOutHorizontally { -direction * it / 8 } + fadeOut()
                        }
                    (
                        slideInHorizontally { direction * it / 8 } + fadeIn()
                    ).togetherWith(exit).using(null)
                },
                label = "page",
            ) { state ->
                val destination = state.destination
                val documentId = state.documentId
                val showPasteText = state.showPasteText
                val pageTopPadding =
                    if (documentId != null || showPasteText) {
                        collapsedTopBarHeight
                    } else {
                        libraryTopBarHeight
                    }

                if (documentId != null) {
                    val document = openedDocument?.takeIf { it.id == documentId }
                    if (document != null) {
                        ReaderScreen(
                            document = document,
                            settings = settings,
                            onSettingsChange = onSettingsChange,
                            hazeState = state.hazeState,
                            backgroundColor = hazeBackgroundColor,
                            topPadding = pageTopPadding,
                            holdPaused = readerHeld,
                            onProgressChange = { progressPosition ->
                                val progress = progressPercent(progressPosition)
                                val index = documents.indexOfFirst { it.id == document.id }
                                if (index >= 0 && documents[index].progress != progress) {
                                    documents[index] = documents[index].copy(progress = progress)
                                }
                                val positionChanged =
                                    openedDocument?.progressPosition != progressPosition
                                if (openedDocumentId == document.id && positionChanged) {
                                    openedDocument = document.copy(
                                        progress = progress,
                                        progressPosition = progressPosition,
                                    )
                                }
                                if (positionChanged) {
                                    documentStore.updateProgress(
                                        document.id,
                                        progress,
                                        progressPosition,
                                    )
                                }
                            },
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
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
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        documentStore.save(document)
                                    }
                                    documents.add(0, document.toLibraryDocument())
                                    libraryStorage = withContext(Dispatchers.IO) {
                                        documentStore.storage()
                                    }
                                    pastingText = false
                                    documentStore.setActiveDocument(document.id)
                                    openedDocumentId = document.id
                                } catch (error: Exception) {
                                    if (error is CancellationException) {
                                        throw error
                                    }
                                    snackbarHostState.showSnackbar(
                                        error.message
                                            ?: resources.getString(
                                                R.string.document_import_failed,
                                            ),
                                    )
                                }
                            }
                        },
                        modifier = Modifier.padding(
                            top = pageTopPadding,
                            bottom = innerPadding.calculateBottomPadding(),
                        ),
                    )
                } else when (destination) {
                    AppDestination.Library -> Box(Modifier.fillMaxSize()) {
                        LibraryScreen(
                            state = libraryGridState,
                            documents = documents,
                            searchQuery = librarySearchQuery.orEmpty(),
                            sort = settings.librarySort,
                            loadCover = documentStore::loadCover,
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = pageTopPadding + 16.dp,
                                end = 16.dp,
                                bottom = innerPadding.calculateBottomPadding() + 88.dp,
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(state = state.hazeState),
                            onDocumentClick = {
                                val index = documents.indexOfFirst { document ->
                                    document.id == it.id
                                }
                                if (index > 0) documents.add(0, documents.removeAt(index))
                                documentStore.setActiveDocument(it.id)
                                openedDocumentId = it.id
                            },
                            onDocumentLongClick = { document, position ->
                                documentMenuId = document.id
                                documentMenuAnchor = position
                                documentMenuExpanded = true
                            },
                        )
                        AddContentButton(
                            expanded = addContentMenuExpanded,
                            hazeState = state.hazeState,
                            backgroundColor = hazeBackgroundColor,
                            onExpandedChange = { addContentMenuExpanded = it },
                            onBoundsChanged = { addContentMenuBounds = it },
                            onPasteText = { pastingText = true },
                            onAddDocument = {
                                documentPicker.launch(SupportedDocumentTypes)
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(16.dp),
                        )
                        if (libraryLoading) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    }

                    AppDestination.Settings -> SettingsScreen(
                        settings = settings,
                        storage = libraryStorage,
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            top = pageTopPadding + 8.dp,
                            end = 8.dp,
                            bottom = innerPadding.calculateBottomPadding() + 16.dp,
                        ),
                        hazeState = state.hazeState,
                        onSettingsChange = onSettingsChange,
                        onClearLibrary = {
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) { documentStore.clear() }
                                    documents.clear()
                                    documentStore.setActiveDocument(null)
                                    openedDocumentId = null
                                    libraryStorage = LibraryStorage(0, 0)
                                } catch (error: Exception) {
                                    if (error is CancellationException) throw error
                                    snackbarHostState.showSnackbar(
                                        resources.getString(R.string.library_clear_failed),
                                    )
                                }
                            }
                        },
                    )

                    AppDestination.Info -> InfoScreen(
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            top = pageTopPadding + 8.dp,
                            end = 8.dp,
                            bottom = innerPadding.calculateBottomPadding() + 16.dp,
                        ),
                        hazeState = state.hazeState,
                    )
                }
            }
        }

        AnimatedContent(
            targetState = pageState,
            transitionSpec = {
                val direction = when {
                    targetState.documentId != null || targetState.showPasteText -> 1
                    initialState.documentId != null || initialState.showPasteText -> -1
                    targetState.destination.ordinal > initialState.destination.ordinal -> 1
                    else -> -1
                }
                (
                    slideInHorizontally { direction * it / 8 } + fadeIn()
                ).togetherWith(
                    slideOutHorizontally { -direction * it / 8 } + fadeOut(),
                ).using(null)
            },
            label = "top bar",
        ) { state ->
            val destination = state.destination
            val documentId = state.documentId
            val showPasteText = state.showPasteText
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
                        destination != AppDestination.Library,
                forceCollapsed = documentId != null || showPasteText,
                searchQuery = librarySearchQuery,
                onNavigationClick = {
                    when {
                        openedDocumentId != null -> {
                            documentStore.setActiveDocument(null)
                            openedDocumentId = null
                        }
                        pastingText -> pastingText = false
                        currentDestination != AppDestination.Library ->
                            currentDestination = AppDestination.Library
                        else -> navigationMenuExpanded = !navigationMenuExpanded
                    }
                },
                onSearchQueryChange = { librarySearchQuery = it },
                onSortClick = { sortMenuExpanded = !sortMenuExpanded },
                modifier = Modifier
                    .hazeEffect(state = state.hazeState) {
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

        NavigationMenuOverlay(
            expanded = navigationMenuExpanded,
            currentDestination = currentDestination,
            topOffset = libraryTopBarHeight,
            hazeState = pageState.hazeState,
            backgroundColor = hazeBackgroundColor,
            onBoundsChanged = { navigationMenuBounds = it },
            onDestinationSelected = { destination ->
                currentDestination = destination
                documentStore.setActiveDocument(null)
                openedDocumentId = null
                pastingText = false
                navigationMenuExpanded = false
            },
        )

        SortMenuOverlay(
            expanded = sortMenuExpanded,
            currentSort = settings.librarySort,
            topOffset = libraryTopBarHeight,
            hazeState = pageState.hazeState,
            backgroundColor = hazeBackgroundColor,
            onBoundsChanged = { sortMenuBounds = it },
            onSortSelected = {
                onSettingsChange(settings.copy(librarySort = it))
                sortMenuExpanded = false
            },
        )

        DocumentMenuOverlay(
            expanded = documentMenuExpanded,
            anchor = documentMenuAnchor,
            hazeState = pageState.hazeState,
            backgroundColor = hazeBackgroundColor,
            onBoundsChanged = { documentMenuBounds = it },
            onChangeTitle = {
                val document = documents.firstOrNull { it.id == documentMenuId }
                if (document != null) {
                    editDocumentValue = document.title
                    editingDocumentField = DocumentEditField.Title
                }
                documentMenuExpanded = false
            },
            onChangeAuthor = {
                val document = documents.firstOrNull { it.id == documentMenuId }
                if (document != null) {
                    editDocumentValue = document.author.orEmpty()
                    editingDocumentField = DocumentEditField.Author
                }
                documentMenuExpanded = false
            },
            onDelete = {
                documentMenuExpanded = false
                confirmDocumentDelete = true
            },
        )

        val documentBeingEdited =
            documents.firstOrNull { it.id == documentMenuId }
        val editField = editingDocumentField
        if (documentBeingEdited != null && editField != null) {
            AlertDialog(
                onDismissRequest = { editingDocumentField = null },
                title = {
                    Text(
                        stringResource(
                            if (editField == DocumentEditField.Title) {
                                R.string.change_title
                            } else {
                                R.string.change_author
                            },
                        ),
                    )
                },
                text = {
                    OutlinedTextField(
                        value = editDocumentValue,
                        onValueChange = { editDocumentValue = it },
                        label = {
                            Text(
                                stringResource(
                                    if (editField == DocumentEditField.Title) {
                                        R.string.title
                                    } else {
                                        R.string.author
                                    },
                                ),
                            )
                        },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled =
                            editField == DocumentEditField.Author ||
                                editDocumentValue.isNotBlank(),
                        onClick = {
                            val value = editDocumentValue.trim()
                            val updatedSummary = when (editField) {
                                DocumentEditField.Title ->
                                    documentBeingEdited.copy(title = value)
                                DocumentEditField.Author ->
                                    documentBeingEdited.copy(author = value.ifEmpty { null })
                            }
                            editingDocumentField = null
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        val stored = documentStore.load(documentBeingEdited.id)
                                        val updated = when (editField) {
                                            DocumentEditField.Title -> stored.copy(title = value)
                                            DocumentEditField.Author ->
                                                stored.copy(author = value.ifEmpty { null })
                                        }
                                        documentStore.save(updated, makeRecent = false)
                                    }
                                    val index =
                                        documents.indexOfFirst { it.id == updatedSummary.id }
                                    if (index >= 0) documents[index] = updatedSummary
                                    if (openedDocument?.id == updatedSummary.id) {
                                        openedDocument = when (editField) {
                                            DocumentEditField.Title ->
                                                openedDocument?.copy(title = value)
                                            DocumentEditField.Author ->
                                                openedDocument?.copy(
                                                    author = value.ifEmpty { null },
                                                )
                                        }
                                    }
                                    libraryStorage = withContext(Dispatchers.IO) {
                                        documentStore.storage()
                                    }
                                } catch (error: Exception) {
                                    if (error is CancellationException) throw error
                                    snackbarHostState.showSnackbar(
                                        resources.getString(R.string.document_update_failed),
                                    )
                                }
                            }
                        },
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingDocumentField = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }

        if (confirmDocumentDelete && documentBeingEdited != null) {
            AlertDialog(
                onDismissRequest = { confirmDocumentDelete = false },
                title = { Text(stringResource(R.string.delete_document)) },
                text = {
                    Text(
                        stringResource(
                            R.string.delete_document_confirmation,
                            documentBeingEdited.title,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val id = documentBeingEdited.id
                            confirmDocumentDelete = false
                            documentMenuId = null
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        documentStore.delete(id)
                                    }
                                    documents.removeAll { it.id == id }
                                    libraryStorage = withContext(Dispatchers.IO) {
                                        documentStore.storage()
                                    }
                                } catch (error: Exception) {
                                    if (error is CancellationException) {
                                        throw error
                                    }
                                    snackbarHostState.showSnackbar(
                                        resources.getString(
                                            R.string.document_delete_failed,
                                        ),
                                    )
                                }
                            }
                        },
                    ) {
                        Text(
                            stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDocumentDelete = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReadSpeederAppPreview() = ReadSpeederTheme {
    ReadSpeederApp(AppSettings(), {})
}

private inline fun <T> traced(name: String, block: () -> T): T {
    Trace.beginSection(name)
    return try {
        block()
    } finally {
        Trace.endSection()
    }
}
