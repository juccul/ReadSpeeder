package com.juckul.readspeeder.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class PasteTextScreenStateTest {
    @Test
    fun draftSurvivesSavedInstanceStateRestoration() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        restorationTester.setContent {
            MaterialTheme {
                PasteTextScreen(onRead = {})
            }
        }

        onNode(hasSetTextAction()).performTextInput("A draft worth keeping")
        restorationTester.emulateSaveAndRestore()

        onNode(hasSetTextAction()).assertTextContains("A draft worth keeping")
    }
}
