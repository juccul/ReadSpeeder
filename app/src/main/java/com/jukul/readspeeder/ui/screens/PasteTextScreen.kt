package com.jukul.readspeeder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jukul.readspeeder.R

internal const val MaxPasteTextCharacters = 100_000

@Composable
internal fun PasteTextScreen(
    onRead: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(MaxPasteTextCharacters) },
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text(stringResource(R.string.paste_text)) },
            placeholder = { Text(stringResource(R.string.paste_text_hint)) },
            supportingText = {
                Text(stringResource(R.string.paste_text_limit, MaxPasteTextCharacters))
            },
        )
        Button(
            onClick = { onRead(text.trim()) },
            modifier = Modifier.fillMaxWidth(),
            enabled = text.isNotBlank(),
        ) {
            Text(stringResource(R.string.start_reading))
        }
    }
}
