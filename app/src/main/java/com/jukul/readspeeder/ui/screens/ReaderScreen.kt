package com.jukul.readspeeder.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Trace
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jukul.readspeeder.R
import com.jukul.readspeeder.data.AppSettings
import com.jukul.readspeeder.data.DocumentChapter
import com.jukul.readspeeder.data.formatHtmlBlocks
import com.jukul.readspeeder.data.formatPlainTextBlocks
import com.jukul.readspeeder.data.MaxWpm
import com.jukul.readspeeder.data.MinWpm
import com.jukul.readspeeder.data.ReadDocument
import com.jukul.readspeeder.data.ReaderMode
import com.jukul.readspeeder.data.ReadingAlignment
import com.jukul.readspeeder.data.ReadingFont
import com.jukul.readspeeder.data.WpmStep
import com.jukul.readspeeder.data.progressPercent
import com.jukul.readspeeder.data.progressPosition
import com.jukul.readspeeder.data.wordIndexAtProgress
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeProgressive
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val ProgressSaveIntervalMillis = 2_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderScreen(
    document: ReadDocument,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    hazeState: HazeState,
    backgroundColor: Color,
    topPadding: Dp,
    holdPaused: Boolean,
    onProgressChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val prepared by produceState<PreparedReader?>(null, document.id, document.text, settings.splitHyphenatedWords) {
        value = withContext(Dispatchers.Default) {
            traced("RSVP preparation") {
                val readerText = parseReaderText(document.text, settings.splitHyphenatedWords)
                val frequencies = buildMap {
                    readerText.words.groupingBy { it }.eachCount().forEach { (word, count) ->
                        val normalized = word.normalizedWord()
                        if (normalized.isNotEmpty()) {
                            put(normalized, getOrElse(normalized) { 0 } + count)
                        }
                    }
                }
                PreparedReader(
                    readerText = readerText,
                    wordFrequencies = frequencies,
                    chapterFlashStarts = document.chapters.map {
                        readerText.flashIndex(it.startWord)
                    },
                )
            }
        }
    }
    val reader = prepared
    if (reader == null) {
        Box(
            modifier = modifier.fillMaxSize().hazeSource(hazeState).padding(top = topPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }
    PreparedReaderScreen(
        document = document,
        settings = settings,
        onSettingsChange = onSettingsChange,
        hazeState = hazeState,
        backgroundColor = backgroundColor,
        topPadding = topPadding,
        holdPaused = holdPaused,
        onProgressChange = onProgressChange,
        prepared = reader,
        modifier = modifier,
    )
}

private data class PreparedReader(
    val readerText: ReaderText,
    val wordFrequencies: Map<String, Int>,
    val chapterFlashStarts: List<Int>,
)

private data class PreparedStandard(
    val blocks: List<AnnotatedString>,
    val blockWordStarts: List<Int>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreparedReaderScreen(
    document: ReadDocument,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    hazeState: HazeState,
    backgroundColor: Color,
    topPadding: Dp,
    holdPaused: Boolean,
    onProgressChange: (Int) -> Unit,
    prepared: PreparedReader,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val readerText = prepared.readerText
    val words = readerText.words
    val sentences = readerText.sentences
    val previewSegments = readerText.previewSegments
    val wordFrequencies = prepared.wordFrequencies
    val standardListState = rememberLazyListState()
    var wordIndex by remember(document.id, words.size) {
        val lastIndex = words.lastIndex.coerceAtLeast(0)
        mutableIntStateOf(wordIndexAtProgress(document.progressPosition, lastIndex))
    }
    val currentOnProgressChange by rememberUpdatedState(onProgressChange)
    var playing by remember(document.id) { mutableStateOf(false) }
    var standardMode by remember(document.id) {
        mutableStateOf(settings.defaultReader == ReaderMode.Standard)
    }
    var preparedStandard by remember(
        document.id,
        document.text,
        document.formattedHtml,
        settings.splitHyphenatedWords,
    ) {
        mutableStateOf<PreparedStandard?>(null)
    }
    var wpm by remember(document.id) { mutableIntStateOf(settings.defaultWpm) }
    var chapterMenuExpanded by remember(document.id) { mutableStateOf(false) }
    var countdown by remember(document.id) { mutableIntStateOf(0) }
    var controlsVisible by remember(document.id) { mutableStateOf(true) }
    val progressDescription = stringResource(R.string.reading_progress)
    val progressSliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.tertiary,
        activeTrackColor = MaterialTheme.colorScheme.tertiary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
    LaunchedEffect(standardMode, preparedStandard) {
        if (!standardMode || preparedStandard != null) return@LaunchedEffect
        preparedStandard = withContext(Dispatchers.Default) {
            traced("standard formatting") {
                val blocks = if (document.formattedHtml.isNotEmpty()) {
                    formatHtmlBlocks(document.formattedHtml)
                } else {
                    formatPlainTextBlocks(document.text)
                }
                PreparedStandard(
                    blocks = blocks,
                    blockWordStarts = buildList {
                        var wordCount = 0
                        blocks.forEach { block ->
                            add(wordCount)
                            wordCount += block.text.wordCount(settings.splitHyphenatedWords)
                        }
                    },
                )
            }
        }
    }
    val standardBlocks = preparedStandard?.blocks.orEmpty()
    val blockWordStarts = preparedStandard?.blockWordStarts.orEmpty()

    DisposableEffect(document.id, settings.keepScreenAwake) {
        val activity = context.findActivity()
        val wasKeepingScreenAwake =
            ((activity?.window?.attributes?.flags ?: 0) and
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        if (settings.keepScreenAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (!wasKeepingScreenAwake) {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1_000)
            if (countdown == 1) {
                countdown = 0
                playing = true
            } else {
                countdown--
            }
        }
    }
    LaunchedEffect(
        playing,
        holdPaused,
        settings.autoHideControls,
        controlsVisible,
        standardMode,
    ) {
        if (
            playing && !holdPaused && settings.autoHideControls &&
            controlsVisible && !standardMode
        ) {
            delay(2_000)
            controlsVisible = false
        } else if (!playing || holdPaused || !settings.autoHideControls || standardMode) {
            controlsVisible = true
        }
    }
    LaunchedEffect(
        playing,
        holdPaused,
        wpm,
        words.size,
        settings.smartPauses,
        settings.complexWordPauses,
    ) {
        while (playing && !holdPaused && words.isNotEmpty()) {
            val currentIndex = wordIndex
            val word = words[wordIndex]
            delay(
                wordDelayMillis(
                    word = word,
                    wpm = wpm,
                    smartPauses = settings.smartPauses,
                    complexWordPauses = settings.complexWordPauses,
                    occurrences = wordFrequencies[word.normalizedWord()] ?: 0,
                    totalWords = words.size,
                ),
            )
            if (!playing || holdPaused || wordIndex != currentIndex) continue
            if (wordIndex < words.lastIndex) {
                wordIndex++
            } else {
                playing = false
            }
        }
    }
    LaunchedEffect(document.id, words.lastIndex) {
        while (true) {
            delay(ProgressSaveIntervalMillis)
            currentOnProgressChange(progressPosition(wordIndex, words.lastIndex))
        }
    }
    DisposableEffect(lifecycleOwner, document.id, words.lastIndex) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                currentOnProgressChange(progressPosition(wordIndex, words.lastIndex))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            currentOnProgressChange(progressPosition(wordIndex, words.lastIndex))
        }
    }
    LaunchedEffect(standardMode, standardBlocks) {
        if (!standardMode || standardBlocks.isEmpty()) return@LaunchedEffect
        val blockIndex = blockWordStarts.indexAt(wordIndex)
        standardListState.scrollToItem(blockIndex)
        withFrameNanos {}
        val itemSize = standardListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == blockIndex }
            ?.size
            ?.coerceAtLeast(1)
            ?: 1
        val blockStart = blockWordStarts[blockIndex]
        val blockWords = (
            blockWordStarts.getOrElse(blockIndex + 1) { words.size } - blockStart
        ).coerceAtLeast(1)
        // ponytail: proportional within 50-line blocks; use per-word text bounds if exact offsets matter.
        standardListState.scrollToItem(
            blockIndex,
            (itemSize * (wordIndex - blockStart) / blockWords.toFloat()).roundToInt(),
        )
        snapshotFlow {
            Triple(
                standardListState.firstVisibleItemIndex,
                standardListState.firstVisibleItemScrollOffset,
                standardListState.isScrollInProgress,
            )
        }.distinctUntilChanged().collect { (index, offset, scrolling) ->
            if (!scrolling) return@collect
            val visible = standardListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == index }
                ?: return@collect
            val start = blockWordStarts.getOrElse(index) { 0 }
            val count = (
                blockWordStarts.getOrElse(index + 1) { words.size } - start
            ).coerceAtLeast(1)
            wordIndex = (
                start + count * offset / visible.size.coerceAtLeast(1).toFloat()
            ).roundToInt().coerceIn(0, words.lastIndex.coerceAtLeast(0))
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(controlsVisible) {
                if (!controlsVisible) {
                    awaitEachGesture {
                        awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        ).consume()
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                        controlsVisible = true
                    }
                }
            },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = if (controlsVisible || standardMode) 1f else 0f
                    }
                    .then(
                        if (controlsVisible || standardMode) Modifier
                        else Modifier.clearAndSetSemantics {},
                    )
                    .hazeEffect(state = hazeState) {
                        blurEffect {
                            this.backgroundColor = backgroundColor
                            blurRadius = 24.dp
                            progressive = HazeProgressive.verticalGradient(
                                easing = FastOutLinearInEasing,
                                startIntensity = 0f,
                                endIntensity = 1f,
                                preferPerformance = true,
                            )
                        }
                    }
                    .background(
                        Brush.verticalGradient(
                            0f to backgroundColor.copy(alpha = 0f),
                            0.75f to backgroundColor.copy(alpha = 0.7f),
                            1f to backgroundColor,
                        ),
                    )
                    .padding(top = 12.dp),
                containerColor = Color.Transparent,
            ) {
                NavigationBarItem(
                    selected = !standardMode,
                    onClick = {
                        standardMode = false
                        onSettingsChange(settings.copy(defaultReader = ReaderMode.Speed))
                    },
                    enabled = controlsVisible,
                    icon = { Icon(Icons.Default.Speed, null) },
                    label = { Text(stringResource(R.string.speed)) },
                    modifier = Modifier.offset(y = 4.dp),
                )
                NavigationBarItem(
                    selected = standardMode,
                    onClick = {
                        standardMode = true
                        playing = false
                        countdown = 0
                        onSettingsChange(settings.copy(defaultReader = ReaderMode.Standard))
                    },
                    enabled = controlsVisible,
                    icon = { Icon(Icons.Default.Description, null) },
                    label = { Text(stringResource(R.string.standard)) },
                    modifier = Modifier.offset(y = 4.dp),
                )
            }
        },
    ) { innerPadding ->
        if (standardMode) {
            val fontSize = settings.textSize.sp
            val textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = when (settings.readingFont) {
                    ReadingFont.SansSerif ->
                        MaterialTheme.typography.bodyLarge.fontFamily
                    ReadingFont.Serif -> FontFamily.Serif
                },
                fontSize = fontSize,
                lineHeight = fontSize * settings.lineSpacing,
                textAlign = when (settings.alignment) {
                    ReadingAlignment.Start -> TextAlign.Start
                    ReadingAlignment.Justified -> TextAlign.Justify
                },
            )
            if (preparedStandard == null) {
                Box(
                    Modifier.fillMaxSize().hazeSource(hazeState),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else SelectionContainer {
                LazyColumn(
                    state = standardListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(hazeState),
                    contentPadding = PaddingValues(
                        start = settings.horizontalMargin.dp,
                        top = topPadding + 24.dp,
                        end = settings.horizontalMargin.dp,
                        bottom = innerPadding.calculateBottomPadding() + 24.dp,
                    ),
                ) {
                    items(standardBlocks) { block ->
                        Text(
                            text = block,
                            style = textStyle,
                        )
                    }
                }
            }
        } else {
            val sentenceIndex = sentences.binarySearchBy(wordIndex) { it.firstWord }.let {
                if (it >= 0) it else (-it - 2).coerceAtLeast(0)
            }
            val previewSegmentIndex =
                previewSegments.binarySearchBy(wordIndex) { it.firstWord }.let {
                    if (it >= 0) it else (-it - 2).coerceAtLeast(0)
                }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .padding(top = topPadding)
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(2f),
                    contentAlignment = Alignment.Center,
                ) {
                    val word = words.getOrElse(wordIndex) { "" }
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        SentencePreview(
                            sentence = previewSegments.getOrNull(previewSegmentIndex),
                            words = words,
                            wordIndex = wordIndex,
                            wpm = wpm,
                            visible = settings.sentencePreview && countdown == 0,
                        )
                        val displayedWord = if (countdown > 0) countdown.toString() else word
                        val focus = remember(displayedWord) {
                            displayedWord.focusCharacterRange()
                        }
                        val focusStart = focus.first.coerceAtLeast(0)
                        val focusEnd = if (focus.isEmpty()) focusStart else focus.last + 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clearAndSetSemantics {
                                    contentDescription = displayedWord
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = displayedWord.take(focusStart),
                                modifier = Modifier
                                    .weight(1f)
                                    .wrapContentWidth(Alignment.End, unbounded = true),
                                style = MaterialTheme.typography.displayMedium,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .width(2.dp)
                                        .height(10.dp)
                                        .background(
                                            if (settings.focusGuides && countdown == 0) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                Color.Transparent
                                            },
                                        ),
                                )
                                Text(
                                    text = displayedWord.substring(focusStart, focusEnd),
                                    style = MaterialTheme.typography.displayMedium,
                                    maxLines = 1,
                                )
                                Box(
                                    Modifier
                                        .width(2.dp)
                                        .height(10.dp)
                                        .background(
                                            if (settings.focusGuides && countdown == 0) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                Color.Transparent
                                            },
                                        ),
                                )
                            }
                            Text(
                                text = displayedWord.drop(focusEnd),
                                modifier = Modifier
                                    .weight(1f)
                                    .wrapContentWidth(Alignment.Start, unbounded = true),
                                style = MaterialTheme.typography.displayMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                            )
                        }
                    }
                    if (document.chapters.isNotEmpty()) {
                        val chapterIndex = prepared.chapterFlashStarts.indexAt(wordIndex)
                        ChapterSelector(
                            chapters = document.chapters,
                            currentChapter = document.chapters[chapterIndex],
                            expanded = chapterMenuExpanded,
                            onExpandedChange = { chapterMenuExpanded = it },
                            onChapterSelected = {
                                playing = false
                                countdown = 0
                                wordIndex = readerText.flashIndex(it.startWord)
                                    .coerceAtMost(words.lastIndex)
                                chapterMenuExpanded = false
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .graphicsLayer {
                                    alpha = if (controlsVisible) 1f else 0f
                                }
                                .then(
                                    if (controlsVisible) Modifier
                                    else Modifier.clearAndSetSemantics {},
                                ),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer { alpha = if (controlsVisible) 1f else 0f }
                        .then(
                            if (controlsVisible) Modifier
                            else Modifier.clearAndSetSemantics {},
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Slider(
                        value = wordIndex.toFloat(),
                        onValueChange = {
                            playing = false
                            countdown = 0
                            wordIndex = it.roundToInt()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = progressDescription },
                        enabled = words.size > 1,
                        colors = progressSliderColors,
                        thumb = {
                            Box(
                                modifier = Modifier.height(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .background(
                                            MaterialTheme.colorScheme.tertiary,
                                            CircleShape,
                                        ),
                                )
                            }
                        },
                        track = {
                            SliderDefaults.Track(
                                sliderState = it,
                                modifier = Modifier.height(4.dp),
                                colors = progressSliderColors,
                                drawStopIndicator = null,
                                thumbTrackGapSize = 0.dp,
                            )
                        },
                        valueRange = 0f..maxOf(1, words.lastIndex).toFloat(),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                countdown = 0
                                wordIndex = sentences[sentenceIndex - 1].firstWord
                            },
                            modifier = Modifier.size(56.dp),
                            enabled = sentenceIndex > 0,
                        ) {
                            Icon(
                                Icons.Rounded.FastRewind,
                                stringResource(R.string.rewind),
                                Modifier.size(48.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                if (playing || countdown > 0) {
                                    playing = false
                                    countdown = 0
                                } else if (settings.playbackCountdown) {
                                    countdown = 3
                                } else {
                                    playing = true
                                }
                            },
                            modifier = Modifier.size(56.dp),
                            enabled = words.isNotEmpty(),
                        ) {
                            Icon(
                                if (playing || countdown > 0) {
                                    Icons.Rounded.Pause
                                } else {
                                    Icons.Rounded.PlayArrow
                                },
                                stringResource(
                                    if (playing || countdown > 0) R.string.pause
                                    else R.string.play,
                                ),
                                Modifier.size(56.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                countdown = 0
                                wordIndex = sentences[sentenceIndex + 1].firstWord
                            },
                            modifier = Modifier.size(56.dp),
                            enabled = sentenceIndex < sentences.lastIndex,
                        ) {
                            Icon(
                                Icons.Rounded.FastForward,
                                stringResource(R.string.forward),
                                Modifier.size(48.dp),
                            )
                        }
                    }
                    Slider(
                        value = wpm.toFloat(),
                        onValueChange = {
                            wpm = ((it / WpmStep).roundToInt() * WpmStep)
                                .coerceIn(MinWpm, MaxWpm)
                        },
                        onValueChangeFinished = {
                            onSettingsChange(settings.copy(defaultWpm = wpm))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        valueRange = MinWpm.toFloat()..MaxWpm.toFloat(),
                    )
                    Text(
                        text = stringResource(R.string.wpm, wpm),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SentencePreview(
    sentence: ReaderSegment?,
    words: List<String>,
    wordIndex: Int,
    wpm: Int,
    visible: Boolean,
) {
    val scrollState = rememberScrollState()
    var layout by remember(sentence) { mutableStateOf<TextLayoutResult?>(null) }
    var firstVisibleLine by remember(sentence) { mutableIntStateOf(0) }
    val highlight = remember { Animatable(1f) }
    val textStyle = MaterialTheme.typography.bodyLarge
    val lineHeight = with(LocalDensity.current) { textStyle.lineHeight.toDp() }
    val previewHeight = lineHeight * 3
    val wordRange = remember(sentence, wordIndex, words) {
        if (sentence == null || wordIndex !in sentence.firstWord..sentence.lastWord) {
            IntRange.EMPTY
        } else {
            val start = sentence.wordCharacterStarts[wordIndex - sentence.firstWord]
            start until start + words[wordIndex].length
        }
    }

    LaunchedEffect(wordIndex) {
        highlight.snapTo(0.55f)
        highlight.animateTo(1f, tween((45_000 / wpm).coerceIn(40, 120)))
    }
    LaunchedEffect(sentence) {
        firstVisibleLine = 0
        scrollState.scrollTo(0)
    }
    LaunchedEffect(wordIndex, sentence, layout, visible) {
        val textLayout = layout
        if (!visible || textLayout == null || wordRange.isEmpty()) return@LaunchedEffect
        val line = textLayout.getLineForOffset(wordRange.first)
        val targetLine = previewStartLine(firstVisibleLine, line)
            .coerceAtMost(textLayout.lineCount - 1)
        if (targetLine == firstVisibleLine) return@LaunchedEffect
        firstVisibleLine = targetLine
    }
    LaunchedEffect(firstVisibleLine, sentence, layout, visible) {
        val textLayout = layout
        if (!visible || textLayout == null) return@LaunchedEffect
        scrollState.animateScrollTo(
            textLayout.getLineTop(firstVisibleLine)
                .roundToInt()
                .coerceIn(0, scrollState.maxValue),
            tween(240),
        )
    }

    val preview = buildAnnotatedString {
        val text = sentence?.text.orEmpty()
        if (wordRange.isEmpty()) {
            append(text)
        } else {
            append(text.take(wordRange.first))
            withStyle(
                SpanStyle(
                    background = MaterialTheme.colorScheme.secondaryContainer.copy(
                        alpha = 0.7f * highlight.value,
                    ),
                    color = lerp(
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.onSecondaryContainer,
                        highlight.value,
                    ),
                ),
            ) {
                append(text.substring(wordRange))
            }
            append(text.drop(wordRange.last + 1))
        }
    }
    Text(
        text = preview,
        modifier = Modifier
            .fillMaxWidth()
            .height(previewHeight)
            .clipToBounds()
            .verticalScroll(scrollState, enabled = false)
            .padding(bottom = lineHeight * 2)
            .graphicsLayer { alpha = if (visible) 1f else 0f }
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics {}),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = textStyle,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Clip,
        onTextLayout = { layout = it },
    )
}

internal fun previewStartLine(currentStart: Int, highlightedLine: Int): Int =
    if (highlightedLine >= currentStart + 2 || highlightedLine < currentStart) {
        highlightedLine / 2 * 2
    } else {
        currentStart
    }

internal fun wordDelayMillis(
    word: String,
    wpm: Int,
    smartPauses: Boolean,
    complexWordPauses: Boolean = false,
    occurrences: Int = 0,
    totalWords: Int = 0,
): Long {
    val base = 60_000.0 / wpm.coerceIn(MinWpm, MaxWpm)
    val punctuation = word.trimEnd { it in "\"'”’»)]}" }.lastOrNull()
    val punctuationMultiplier = when {
        !smartPauses -> 1.0
        punctuation != null && punctuation in ".!?…。！？" -> 3.0
        punctuation != null && punctuation in ",;:，；：" -> 2.0
        else -> 1.0
    }
    val complexityExtra = if (complexWordPauses) {
        word.complexityExtra(occurrences, totalWords)
    } else {
        0.0
    }
    return (base * (punctuationMultiplier + complexityExtra)).toLong()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal data class ReaderText(
    val words: List<String>,
    val sentences: List<ReaderSegment>,
    val previewSegments: List<ReaderSegment>,
    private val sourceWordStarts: IntArray,
) {
    fun flashIndex(sourceWord: Int): Int =
        sourceWordStarts.getOrElse(sourceWord.coerceAtLeast(0)) { words.size }
            .coerceAtMost(words.lastIndex.coerceAtLeast(0))
}

internal data class ReaderSegment(
    val text: String,
    val firstWord: Int,
    val lastWord: Int,
    val wordCharacterStarts: IntArray,
)

internal fun parseReaderText(text: String, splitHyphenatedWords: Boolean): ReaderText {
    val sourceWordCount = text.sourceWordCount()
    if (sourceWordCount == 0) {
        return ReaderText(emptyList(), emptyList(), emptyList(), IntArray(0))
    }

    val words = ArrayList<String>(sourceWordCount)
    val paragraphEnds = ArrayList<Boolean>(sourceWordCount)
    val sourceWordStarts = IntArray(sourceWordCount + 1)
    var sourceWord = 0
    var start = 0
    while (start < text.length) {
        while (start < text.length && text[start].isWhitespace()) start++
        if (start == text.length) break
        var end = start + 1
        while (end < text.length && !text[end].isWhitespace()) end++
        sourceWordStarts[sourceWord++] = words.size
        text.substring(start, end).flashWords(splitHyphenatedWords).forEach {
            words += it
            paragraphEnds += false
        }
        var newlines = 0
        while (end < text.length && text[end].isWhitespace()) {
            if (text[end++] == '\n') newlines++
        }
        paragraphEnds[paragraphEnds.lastIndex] = newlines >= 2
        start = end
    }
    sourceWordStarts[sourceWordCount] = words.size
    return ReaderText(
        words = words,
        sentences = words.segments(paragraphEnds, includeClauses = false),
        previewSegments = words.segments(paragraphEnds, includeClauses = true),
        sourceWordStarts = sourceWordStarts,
    )
}

private fun String.sourceWordCount(): Int {
    var count = 0
    var index = 0
    while (index < length) {
        while (index < length && this[index].isWhitespace()) index++
        if (index == length) break
        count++
        while (index < length && !this[index].isWhitespace()) index++
    }
    return count
}

private fun List<String>.segments(
    paragraphEnds: List<Boolean>,
    includeClauses: Boolean,
): List<ReaderSegment> = buildList {
    var firstWord = 0
    this@segments.forEachIndexed { index, word ->
        if (
            index == this@segments.lastIndex ||
            word.endsSegment(includeClauses) ||
            paragraphEnds[index]
        ) {
            val segmentWords = this@segments.subList(firstWord, index + 1)
            add(
                ReaderSegment(
                    text = segmentWords.joinToString(" "),
                    firstWord = firstWord,
                    lastWord = index,
                    wordCharacterStarts = IntArray(segmentWords.size).also { starts ->
                        var character = 0
                        segmentWords.forEachIndexed { wordIndex, segmentWord ->
                            starts[wordIndex] = character
                            character += segmentWord.length + 1
                        }
                    },
                ),
            )
            firstWord = index + 1
        }
    }
}

private fun String.endsSegment(includeClauses: Boolean): Boolean {
    val punctuation = trimEnd { it in "\"'”’»)]}" }.lastOrNull()
    return punctuation != null && punctuation in if (includeClauses) {
        ".!?…。！？,;:，；："
    } else {
        ".!?…。！？"
    }
}

private fun String.flashWords(splitHyphenatedWords: Boolean): List<String> {
    if (!splitHyphenatedWords) return listOf(this)
    val words = mutableListOf<String>()
    var start = 0
    for (index in indices) {
        if (
            this[index].isWordHyphen() &&
            index > start &&
            index < lastIndex &&
            this[index - 1].isLetterOrDigit() &&
            this[index + 1].isLetterOrDigit()
        ) {
            words += substring(start, index)
            start = index + 1
        }
    }
    if (words.isEmpty()) return listOf(this)
    words += substring(start)
    return words
}

private fun Char.isWordHyphen(): Boolean =
    Character.getType(this) == Character.DASH_PUNCTUATION.toInt() ||
        this == '\u00AD' || this == '\u2212'

private fun String.normalizedWord(): String =
    lowercase().filter(Char::isLetter)

private fun String.wordCount(splitHyphenatedWords: Boolean): Int {
    var count = 0
    var start = 0
    while (start < length) {
        while (start < length && this[start].isWhitespace()) start++
        if (start == length) break
        var end = start + 1
        while (end < length && !this[end].isWhitespace()) end++
        count += substring(start, end).flashWords(splitHyphenatedWords).size
        start = end
    }
    return count
}

private fun List<Int>.indexAt(wordIndex: Int): Int {
    val result = binarySearch(wordIndex)
    return if (result >= 0) result else (-result - 2).coerceAtLeast(0)
}

private fun String.complexityExtra(occurrences: Int, totalWords: Int): Double {
    val letters = normalizedWord()
    if (letters.isEmpty()) return 0.0
    // ponytail: English vowel heuristic; replace with language-aware syllabification if needed.
    var syllables = 0
    var wasVowel = false
    letters.forEach {
        val isVowel = it in "aeiouy"
        if (isVowel && !wasVowel) syllables++
        wasVowel = isVowel
    }
    val signals = listOf(
        occurrences in 1..maxOf(1, totalWords / 10_000),
        letters.length >= 12,
        syllables >= 4 || ('-' in this && letters.length >= 8),
    ).count { it }
    return signals.coerceAtMost(3) * 0.4
}

private fun String.focusCharacterRange(): IntRange {
    val characters = buildList {
        var start = 0
        while (start < length) {
            var end = start + Character.charCount(codePointAt(start))
            while (end < length && codePointAt(end).isCombiningMark()) {
                end += Character.charCount(codePointAt(end))
            }
            add(start until end)
            start = end
        }
    }
    val readable = characters.filter { Character.isLetterOrDigit(codePointAt(it.first)) }
    val candidates = readable.ifEmpty { characters }
    if (candidates.isEmpty()) return IntRange.EMPTY

    val position = when (candidates.size) {
        1 -> 0
        in 2..5 -> 1
        in 6..9 -> 2
        in 10..13 -> 3
        else -> 4
    }
    return candidates[position.coerceAtMost(candidates.lastIndex)]
}

private fun Int.isCombiningMark(): Boolean = when (Character.getType(this)) {
    Character.NON_SPACING_MARK.toInt(),
    Character.COMBINING_SPACING_MARK.toInt(),
    Character.ENCLOSING_MARK.toInt(),
    -> true

    else -> false
}

private inline fun <T> traced(name: String, block: () -> T): T {
    Trace.beginSection(name)
    return try {
        block()
    } finally {
        Trace.endSection()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterSelector(
    chapters: List<DocumentChapter>,
    currentChapter: DocumentChapter,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onChapterSelected: (DocumentChapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    LaunchedEffect(expanded, currentChapter) {
        if (expanded) {
            listState.scrollToItem(chapters.indexOf(currentChapter).coerceAtLeast(0))
        }
    }

    Box(modifier.fillMaxWidth()) {
        TextButton(
            onClick = { onExpandedChange(true) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = currentChapter.title,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
    }
    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = { onExpandedChange(false) },
            sheetState = sheetState,
        ) {
            Text(
                text = stringResource(R.string.chapters),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(chapters) { chapter ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = chapter.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingContent = {
                            if (chapter == currentChapter) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                        ),
                        modifier = Modifier.clickable {
                            onChapterSelected(chapter)
                        },
                    )
                }
            }
        }
    }
}
