package com.jukul.readspeeder.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import dev.chrisbanes.haze.HazeState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InfoScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsProjectLinksVersionAndBundledLicenses() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            MaterialTheme {
                InfoScreen(
                    contentPadding = PaddingValues(),
                    hazeState = remember { HazeState() },
                )
            }
        }

        composeRule.onNodeWithText("Free and open-source software").assertExists()
        composeRule.onNodeWithText(appVersionName(context)).assertExists()
        composeRule.onNodeWithText("Source code").assertHasClickAction()
        composeRule.onNodeWithText("Support development").assertHasClickAction()

        composeRule.onNodeWithText("AndroidX, Jetpack Compose, and Material Components").assertDoesNotExist()
        composeRule.onNodeWithText("Haze by Chris Banes").assertDoesNotExist()
        composeRule.onNodeWithText("Kotlin by JetBrains").assertDoesNotExist()

        composeRule.onNodeWithText("Google Sans").performScrollTo().performClick()
        composeRule.onNodeWithText(
            "SIL OPEN FONT LICENSE Version 1.1",
            substring = true,
        ).assertExists()
    }

    @Test
    fun infoIsTheThirdMenuDestinationAfterSettings() {
        assertEquals(
            listOf("Library", "Settings", "Info"),
            com.jukul.readspeeder.ui.AppDestination.entries.map { it.name },
        )
    }
}
