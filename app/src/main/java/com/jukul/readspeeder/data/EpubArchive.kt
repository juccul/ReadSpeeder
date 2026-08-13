package com.jukul.readspeeder.data

import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

private const val MaxEpubEntries = 4_096
private const val MaxEpubEntryNameCharacters = 512
private const val MaxEpubEntryBytes = 16 * 1024 * 1024

internal data class EpubArchiveLimits(
    val maxArchiveBytes: Int = MaxDocumentBytes,
    val maxExtractedBytes: Int = MaxDocumentBytes,
    val maxEntryBytes: Int = MaxEpubEntryBytes,
    val maxEntries: Int = MaxEpubEntries,
    val maxEntryNameCharacters: Int = MaxEpubEntryNameCharacters,
)

internal fun readEpubEntries(
    stream: InputStream,
    limits: EpubArchiveLimits = EpubArchiveLimits(),
): Map<String, ByteArray> {
    require(limits.maxArchiveBytes > 0)
    require(limits.maxExtractedBytes > 0)
    require(limits.maxEntryBytes > 0)
    require(limits.maxEntries > 0)
    require(limits.maxEntryNameCharacters > 0)

    val entries = linkedMapOf<String, ByteArray>()
    var entryCount = 0
    var extractedBytes = 0
    ZipInputStream(LimitedArchiveInputStream(stream, limits.maxArchiveBytes)).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            if (!entry.isDirectory) {
                entryCount++
                require(entryCount <= limits.maxEntries) {
                    "EPUB contains too many files"
                }
                require(entry.name.length <= limits.maxEntryNameCharacters) {
                    "EPUB contains a file name that is too long"
                }
                require(entry.size < 0 || entry.size <= limits.maxEntryBytes) {
                    "EPUB contains a file that is too large"
                }
                val name = entry.name.replace('\\', '/').trimStart('/')
                require(name.isNotEmpty()) { "EPUB contains an invalid file name" }
                require(name !in entries) { "EPUB contains duplicate file names" }

                val remainingBytes = limits.maxExtractedBytes - extractedBytes
                require(remainingBytes >= 0) { "EPUB expands beyond the import limit" }
                val bytes = zip.readEntryLimited(minOf(limits.maxEntryBytes, remainingBytes))
                extractedBytes += bytes.size
                entries[name] = bytes
            }
            zip.closeEntry()
        }
    }
    return entries
}

private fun InputStream.readEntryLimited(limit: Int): ByteArray {
    val output = ByteArrayOutputStream(minOf(limit, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        require(total <= limit) { "EPUB contains a file that is too large" }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private class LimitedArchiveInputStream(
    stream: InputStream,
    private val limit: Int,
) : FilterInputStream(stream) {
    private var total = 0

    override fun read(): Int {
        val value = super.read()
        if (value >= 0) recordRead(1)
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val read = super.read(buffer, offset, length)
        if (read > 0) recordRead(read)
        return read
    }

    private fun recordRead(count: Int) {
        total += count
        require(total <= limit) { "EPUB archive is larger than 50 MB" }
    }
}
