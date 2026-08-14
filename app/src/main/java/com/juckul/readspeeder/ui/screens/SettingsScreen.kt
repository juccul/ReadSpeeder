package com.juckul.readspeeder.ui.screens

import android.os.Build
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.juckul.readspeeder.R
import com.juckul.readspeeder.data.AppSettings
import com.juckul.readspeeder.data.LibraryStorage
import com.juckul.readspeeder.data.MaxWpm
import com.juckul.readspeeder.data.MinWpm
import com.juckul.readspeeder.data.ReaderMode
import com.juckul.readspeeder.data.ReadingAlignment
import com.juckul.readspeeder.data.ReadingFont
import com.juckul.readspeeder.data.ThemeMode
import com.juckul.readspeeder.data.WpmStep
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.roundToInt

@Composable
internal fun SettingsScreen(
    settings: AppSettings,
    storage: LibraryStorage,
    contentPadding: PaddingValues,
    hazeState: HazeState,
    onSettingsChange: (AppSettings) -> Unit,
    onClearLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmClear by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize().hazeSource(hazeState),
        contentPadding = contentPadding,
    ) {
        item { SectionTitle(stringResource(R.string.appearance)) }
        item {
            ChoiceSetting(
                title = stringResource(R.string.theme),
                selected = settings.themeMode,
                options = listOf(
                    ThemeMode.System to stringResource(R.string.system_default),
                    ThemeMode.Light to stringResource(R.string.light),
                    ThemeMode.Dark to stringResource(R.string.dark),
                ),
                onSelected = { onSettingsChange(settings.copy(themeMode = it)) },
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item {
                SwitchSetting(
                    title = stringResource(R.string.dynamic_color),
                    checked = settings.dynamicColor,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(dynamicColor = it))
                    },
                )
            }
        }

        item { SectionTitle(stringResource(R.string.reading)) }
        item {
            ChoiceSetting(
                title = stringResource(R.string.default_reader),
                selected = settings.defaultReader,
                options = listOf(
                    ReaderMode.Speed to stringResource(R.string.speed),
                    ReaderMode.Standard to stringResource(R.string.standard),
                ),
                onSelected = { onSettingsChange(settings.copy(defaultReader = it)) },
            )
        }
        item {
            SwitchSetting(
                title = stringResource(R.string.keep_screen_awake),
                checked = settings.keepScreenAwake,
                onCheckedChange = {
                    onSettingsChange(settings.copy(keepScreenAwake = it))
                },
            )
        }
        item { SectionTitle(stringResource(R.string.speed_reading)) }
        item {
            SliderSetting(
                title = stringResource(R.string.default_speed),
                valueLabel = stringResource(R.string.wpm, settings.defaultWpm),
                value = settings.defaultWpm.toFloat(),
                range = MinWpm.toFloat()..MaxWpm.toFloat(),
                steps = (MaxWpm - MinWpm) / WpmStep - 1,
                onValueChange = {
                    onSettingsChange(
                        settings.copy(
                            defaultWpm = (it / WpmStep).roundToInt() * WpmStep,
                        ),
                    )
                },
            )
        }
        item {
            SwitchSetting(
                stringResource(R.string.smart_pauses),
                settings.smartPauses,
            ) { onSettingsChange(settings.copy(smartPauses = it)) }
        }
        item {
            SwitchSetting(
                stringResource(R.string.complex_word_pauses),
                settings.complexWordPauses,
            ) { onSettingsChange(settings.copy(complexWordPauses = it)) }
        }
        item {
            SwitchSetting(
                stringResource(R.string.split_hyphenated_words),
                settings.splitHyphenatedWords,
            ) { onSettingsChange(settings.copy(splitHyphenatedWords = it)) }
        }
        item {
            SwitchSetting(
                stringResource(R.string.playback_countdown),
                settings.playbackCountdown,
            ) { onSettingsChange(settings.copy(playbackCountdown = it)) }
        }
        item {
            SwitchSetting(
                stringResource(R.string.sentence_preview),
                settings.sentencePreview,
            ) { onSettingsChange(settings.copy(sentencePreview = it)) }
        }
        item {
            SwitchSetting(
                stringResource(R.string.focus_guides),
                settings.focusGuides,
            ) { onSettingsChange(settings.copy(focusGuides = it)) }
        }
        item {
            SwitchSetting(
                stringResource(R.string.auto_hide_controls),
                settings.autoHideControls,
            ) { onSettingsChange(settings.copy(autoHideControls = it)) }
        }

        item { SectionTitle(stringResource(R.string.standard_reading)) }
        item {
            ChoiceSetting(
                title = stringResource(R.string.font),
                selected = settings.readingFont,
                options = listOf(
                    ReadingFont.SansSerif to stringResource(R.string.sans_serif),
                    ReadingFont.Serif to stringResource(R.string.serif),
                ),
                onSelected = { onSettingsChange(settings.copy(readingFont = it)) },
            )
        }
        item {
            SliderSetting(
                title = stringResource(R.string.text_size),
                valueLabel = stringResource(R.string.sp_value, settings.textSize),
                value = settings.textSize.toFloat(),
                range = 12f..32f,
                steps = 19,
                onValueChange = {
                    onSettingsChange(settings.copy(textSize = it.roundToInt()))
                },
            )
        }
        item {
            SliderSetting(
                title = stringResource(R.string.line_spacing),
                valueLabel = stringResource(R.string.multiplier, settings.lineSpacing),
                value = settings.lineSpacing,
                range = 1f..2f,
                steps = 9,
                onValueChange = {
                    onSettingsChange(
                        settings.copy(lineSpacing = (it * 10).roundToInt() / 10f),
                    )
                },
            )
        }
        item {
            SliderSetting(
                title = stringResource(R.string.horizontal_margins),
                valueLabel = stringResource(R.string.dp_value, settings.horizontalMargin),
                value = settings.horizontalMargin.toFloat(),
                range = 8f..48f,
                steps = 9,
                onValueChange = {
                    onSettingsChange(
                        settings.copy(horizontalMargin = (it / 4).roundToInt() * 4),
                    )
                },
            )
        }
        item {
            ChoiceSetting(
                title = stringResource(R.string.alignment),
                selected = settings.alignment,
                options = listOf(
                    ReadingAlignment.Start to stringResource(R.string.start),
                    ReadingAlignment.Justified to stringResource(R.string.justified),
                ),
                onSelected = { onSettingsChange(settings.copy(alignment = it)) },
            )
        }

        item { SectionTitle(stringResource(R.string.data)) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.library_storage)) },
                supportingContent = {
                    Text(
                        stringResource(
                            R.string.library_storage_summary,
                            pluralStringResource(
                                R.plurals.library_document_count,
                                storage.documentCount,
                                storage.documentCount,
                            ),
                            Formatter.formatShortFileSize(context, storage.bytes),
                        ),
                    )
                },
            )
        }
        item {
            TextButton(
                onClick = { confirmClear = true },
                enabled = storage.documentCount > 0,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Text(stringResource(R.string.clear_library))
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_library)) },
            text = { Text(stringResource(R.string.clear_library_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearLibrary()
                    },
                ) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column {
        HorizontalDivider()
        Text(
            text = text,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) = ListItem(
    headlineContent = { Text(title) },
    trailingContent = {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    },
    modifier = Modifier.clickable { onCheckedChange(!checked) },
)

@Composable
private fun <T> ChoiceSetting(
    title: String,
    selected: T,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(options.first { it.first == selected }.second)
        },
        modifier = Modifier.clickable { expanded = true },
    )
    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expanded = false
                                    onSelected(value)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = value == selected,
                                onClick = null,
                            )
                            Text(label, Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun SliderSetting(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title)
            Text(valueLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
        )
    }
}
