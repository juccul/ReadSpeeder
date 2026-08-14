package com.juckul.readspeeder.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTimingTest {
    @Test
    fun previewPagesByTwoLinesAtTheThirdLine() {
        assertEquals(0, previewStartLine(0, 0))
        assertEquals(0, previewStartLine(0, 1))
        assertEquals(2, previewStartLine(0, 2))
        assertEquals(2, previewStartLine(2, 3))
        assertEquals(4, previewStartLine(2, 4))
        assertEquals(0, previewStartLine(4, 1))
    }

    @Test
    fun smartPausesApplyPunctuationMultipliers() {
        assertEquals(200, wordDelayMillis("word", 300, false))
        assertEquals(200, wordDelayMillis("word", 300, true))
        assertEquals(400, wordDelayMillis("word,”", 300, true))
        assertEquals(600, wordDelayMillis("word!”", 300, true))
    }

    @Test
    fun complexWordsAddDelayFromDocumentFrequencyAndShape() {
        assertEquals(
            200,
            wordDelayMillis("common", 300, false, true, 100, 10_000),
        )
        assertEquals(
            440,
            wordDelayMillis("internationalization", 300, false, true, 1, 10_000),
        )
    }

    @Test
    fun previewUsesClausesWithoutChangingSentenceNavigation() {
        val text = parseReaderText(
            "Wait, this is: still one; sentence. Next.",
            splitHyphenatedWords = true,
        )

        assertEquals(
            listOf("Wait,", "this is:", "still one;", "sentence.", "Next."),
            text.previewSegments.map { it.text },
        )
        assertEquals(
            listOf("Wait, this is: still one; sentence.", "Next."),
            text.sentences.map { it.text },
        )
    }

    @Test
    fun hyphenatedWordsBecomeOrderedFlashes() {
        val text = parseReaderText(
            "A state-of-the-art reader—friendly design.",
            splitHyphenatedWords = true,
        )

        assertEquals(
            listOf("A", "state", "of", "the", "art", "reader", "friendly", "design."),
            text.words,
        )
        assertEquals(5, text.flashIndex(2))
        assertEquals(7, text.flashIndex(3))
        assertEquals(
            listOf("A", "state-of-the-art", "reader—friendly", "design."),
            parseReaderText(
                "A state-of-the-art reader—friendly design.",
                splitHyphenatedWords = false,
            ).words,
        )
    }
}
