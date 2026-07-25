package com.jukul.readspeeder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.jukul.readspeeder.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeProgressive
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlin.math.roundToInt

private const val DefaultWpm = 300
private const val MinWpm = 50
private const val MaxWpm = 1_000
private const val WpmStep = 5

@Composable
internal fun ReaderScreen(
    initialProgress: Int,
    hazeState: HazeState,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    var progress by remember(initialProgress) {
        mutableIntStateOf(initialProgress.coerceIn(0, 100))
    }
    var playing by remember(initialProgress) { mutableStateOf(false) }
    var standardMode by remember(initialProgress) { mutableStateOf(false) }
    var wpm by remember(initialProgress) { mutableIntStateOf(DefaultWpm) }

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
                                startIntensity = 0f,
                                endIntensity = 1f,
                                preferPerformance = true,
                            )
                        }
                    }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                backgroundColor.copy(alpha = 0f),
                                backgroundColor,
                            ),
                        ),
                    ),
                containerColor = Color.Transparent,
            ) {
                NavigationBarItem(
                    selected = !standardMode,
                    onClick = { standardMode = false },
                    icon = { Icon(Icons.Default.Speed, null) },
                    label = { Text(stringResource(R.string.speed)) },
                )
                NavigationBarItem(
                    selected = standardMode,
                    onClick = { standardMode = true },
                    icon = { Icon(Icons.Default.Description, null) },
                    label = { Text(stringResource(R.string.standard)) },
                )
            }
        },
    ) { innerPadding ->
        if (standardMode) {
            Box(
                modifier = Modifier.fillMaxSize().hazeSource(hazeState).padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.standard_reading_mode))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(2f))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { progress = (progress - 10).coerceAtLeast(0) },
                            modifier = Modifier.size(72.dp),
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
                        ) {
                            Icon(
                                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                stringResource(if (playing) R.string.pause else R.string.play),
                                Modifier.size(56.dp),
                            )
                        }
                        IconButton(
                            onClick = { progress = (progress + 10).coerceAtMost(100) },
                            modifier = Modifier.size(72.dp),
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
