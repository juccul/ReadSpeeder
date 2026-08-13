package com.jukul.readspeeder.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubArchiveTest {
    private val limits = EpubArchiveLimits(
        maxArchiveBytes = 2_048,
        maxExtractedBytes = 32,
        maxEntryBytes = 16,
        maxEntries = 2,
        maxEntryNameCharacters = 16,
    )

    @Test
    fun readsArchiveWithinEveryLimit() {
        val entries = readEpubEntries(
            zipOf("first.txt" to "first", "second.txt" to "second"),
            limits,
        )

        assertEquals(setOf("first.txt", "second.txt"), entries.keys)
        assertArrayEquals("first".toByteArray(), entries.getValue("first.txt"))
    }

    @Test
    fun rejectsTooManyEntries() {
        assertThrows(IllegalArgumentException::class.java) {
            readEpubEntries(
                zipOf("one" to "", "two" to "", "three" to ""),
                limits,
            )
        }
    }

    @Test
    fun rejectsOversizedEntryAndTotalExtraction() {
        assertThrows(IllegalArgumentException::class.java) {
            readEpubEntries(zipOf("large" to "x".repeat(17)), limits)
        }
        assertThrows(IllegalArgumentException::class.java) {
            readEpubEntries(
                zipOf("one" to "x".repeat(16), "two" to "x".repeat(16), "three" to "x"),
                limits.copy(maxEntries = 3),
            )
        }
    }

    @Test
    fun rejectsLongAndDuplicateNames() {
        assertThrows(IllegalArgumentException::class.java) {
            readEpubEntries(zipOf("x".repeat(17) to "text"), limits)
        }
        assertThrows(IllegalArgumentException::class.java) {
            readEpubEntries(zipOf("/same" to "one", "same" to "two"), limits)
        }
    }

    @Test
    fun rejectsOversizedCompressedArchive() {
        assertThrows(IllegalArgumentException::class.java) {
            readEpubEntries(
                zipOf("text" to "uncompressible-${System.nanoTime()}"),
                limits.copy(maxArchiveBytes = 10, maxEntryBytes = 64),
            )
        }
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArrayInputStream {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, value) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(value.toByteArray())
                zip.closeEntry()
            }
        }
        return ByteArrayInputStream(output.toByteArray())
    }
}
