package com.juckul.readspeeder.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test

class DocumentCardAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun actionableCardAnnouncesDocumentIdentityAndProgress() {
        composeRule.setContent {
            MaterialTheme {
                DocumentCard(
                    documentId = "dune",
                    title = "Dune",
                    author = "Frank Herbert",
                    hasCover = false,
                    loadCover = { null },
                    progress = 37,
                    onClick = {},
                    onLongClick = { _: Offset -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Dune, by Frank Herbert")
            .assertHasClickAction()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "37% complete",
                ),
            )
    }
}
