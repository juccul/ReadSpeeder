package com.jukul.readspeeder.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun SettingsScreen(modifier: Modifier = Modifier) =
    Spacer(modifier.fillMaxSize().verticalScroll(rememberScrollState()))
