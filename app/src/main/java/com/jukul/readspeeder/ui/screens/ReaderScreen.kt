package com.jukul.readspeeder.ui.screens

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jukul.readspeeder.R
import com.jukul.readspeeder.data.DocumentChapter
import com.jukul.readspeeder.data.ReadDocument
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeProgressive
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val DefaultWpm = 300
private const val MinWpm = 50
private const val MaxWpm = 1_000
private const val WpmStep = 5
private const val WordJump = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderScreen(
    document: ReadDocument,
    hazeState: HazeState,
    backgroundColor: Color,
    topPadding: Dp,
    onProgressChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val words = remember(document.id, document.text) {
        document.text.split(Regex("\\s+")).filter(String::isNotBlank)
    }
    var wordIndex by remember(document.id, words.size) {
        val lastIndex = words.lastIndex.coerceAtLeast(0)
        mutableIntStateOf((lastIndex * document.progress / 100f).roundToInt())
    }
    var playing by remember(document.id) { mutableStateOf(false) }
    var standardMode by remember(document.id) { mutableStateOf(false) }
    var wpm by remember(document.id) { mutableIntStateOf(DefaultWpm) }
    var chapterMenuExpanded by remember(document.id) { mutableStateOf(false) }
    val progressDescription = stringResource(R.string.reading_progress)
    val progressSliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.tertiary,
        activeTrackColor = MaterialTheme.colorScheme.tertiary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    )

    LaunchedEffect(playing, wpm, wordIndex, words.size) {
        if (playing && words.isNotEmpty()) {
            delay(60_000L / wpm)
            if (wordIndex < words.lastIndex) {
                wordIndex++
            } else {
                playing = false
            }
        }
    }
    LaunchedEffect(wordIndex, words.lastIndex) {
        onProgressChange(
            (wordIndex * 100f / maxOf(1, words.lastIndex)).roundToInt(),
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
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
                    onClick = { standardMode = false },
                    icon = { Icon(Icons.Default.Speed, null) },
                    label = { Text(stringResource(R.string.speed)) },
                    modifier = Modifier.offset(y = 4.dp),
                )
                NavigationBarItem(
                    selected = standardMode,
                    onClick = {
                        standardMode = true
                        playing = false
                    },
                    icon = { Icon(Icons.Default.Description, null) },
                    label = { Text(stringResource(R.string.standard)) },
                    modifier = Modifier.offset(y = 4.dp),
                )
            }
        },
    ) { innerPadding ->
        if (standardMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 24.dp,
                        top = topPadding + 24.dp,
                        end = 24.dp,
                        bottom = innerPadding.calculateBottomPadding() + 24.dp,
                    ),
            ) {
                SelectionContainer {
                    Text(
                        text = document.text,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        } else {
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
                    Text(
                        text = words.getOrElse(wordIndex) { "" },
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (document.chapters.isNotEmpty()) {
                        ChapterSelector(
                            chapters = document.chapters,
                            currentChapter = document.chapters
                                .lastOrNull { it.startWord <= wordIndex }
                                ?: document.chapters.first(),
                            expanded = chapterMenuExpanded,
                            onExpandedChange = { chapterMenuExpanded = it },
                            onChapterSelected = {
                                playing = false
                                wordIndex = it.startWord.coerceAtMost(words.lastIndex)
                                chapterMenuExpanded = false
                            },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Slider(
                        value = wordIndex.toFloat(),
                        onValueChange = {
                            playing = false
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
                            onClick = { wordIndex = (wordIndex - WordJump).coerceAtLeast(0) },
                            modifier = Modifier.size(72.dp),
                            enabled = words.isNotEmpty(),
                        ) {
                            Icon(
                                Icons.Rounded.FastRewind,
                                stringResource(R.string.rewind),
                                Modifier.size(48.dp),
                            )
                        }
                        IconButton(
                            onClick = { playing = !playing },
                            modifier = Modifier.size(72.dp),
                            enabled = words.isNotEmpty(),
                        ) {
                            Icon(
                                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                stringResource(if (playing) R.string.pause else R.string.play),
                                Modifier.size(56.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                wordIndex = (wordIndex + WordJump).coerceAtMost(words.lastIndex)
                            },
                            modifier = Modifier.size(72.dp),
                            enabled = words.isNotEmpty(),
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
                        modifier = Modifier.fillMaxWidth(),
                        valueRange = MinWpm.toFloat()..MaxWpm.toFloat(),
                    )
                    Text(
                        text = stringResource(R.string.wpm, wpm),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterSelector(
    chapters: List<DocumentChapter>,
    currentChapter: DocumentChapter,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onChapterSelected: (DocumentChapter) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            chapters.forEach { chapter ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = chapter.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = { onChapterSelected(chapter) },
                )
            }
        }
    }
}
