package com.juckul.readspeeder.data

import android.content.Context
import android.util.AtomicFile
import androidx.core.content.edit
import org.json.JSONArray
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

internal data class LibraryStorage(val documentCount: Int, val bytes: Long)

internal data class RestoredLibrary(
    val documents: List<LibraryDocument>,
    val failedCount: Int,
)

internal class DocumentStore(context: Context) {
    private val root = File(context.filesDir, DirectoryName)
    private val quarantine = File(root, QuarantineDirectoryName)
    private val preferences =
        context.getSharedPreferences("readspeeder_documents", Context.MODE_PRIVATE)

    fun loadSummaries(): RestoredLibrary = synchronized(fileLock) {
        require(root.exists() || root.mkdirs()) { "Unable to create document storage" }
        var failedCount = recoverAtomicBackups()
        failedCount += recoverLegacyTemporaryFiles()
        discardUncommittedFiles()

        val stored = mutableMapOf<String, StoredSummary>()
        root.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == DocumentExtension }
            .forEach { target ->
                val summary = try {
                    readSummary(target).also {
                        require(target.nameWithoutExtension == it.id.sha256()) {
                            "Document cache ID does not match its filename"
                        }
                    }
                } catch (_: Exception) {
                    quarantine(target)
                    failedCount++
                    return@forEach
                }
                stored[summary.id] = StoredSummary(summary, target.lastModified())
            }

        val documents = synchronized(indexLock) {
            val indexedIds = documentIdsLocked().distinct()
            val indexedSet = indexedIds.toHashSet()
            val recoveredIds = stored.values
                .filter { it.document.id !in indexedSet }
                .sortedByDescending(StoredSummary::lastModified)
                .map { it.document.id }
            val restoredIds = recoveredIds + indexedIds.filter(stored::containsKey)
            val validIds = restoredIds.toHashSet()
            val validProgressKeys = validIds.flatMapTo(mutableSetOf()) { id ->
                listOf(progressKey(id), progressPositionKey(id))
            }
            preferences.edit(commit = true) {
                putString(IndexKey, JSONArray(restoredIds).toString())
                preferences.all.keys
                    .filter { it.startsWith(ProgressKeyPrefix) && it !in validProgressKeys }
                    .forEach(::remove)
                if (preferences.getString(ActiveDocumentKey, null) !in validIds) {
                    remove(ActiveDocumentKey)
                }
            }

            restoredIds.map { id ->
                val summary = checkNotNull(stored[id]).document
                summary.copy(
                    progress = preferences.getInt(progressKey(id), summary.progress)
                        .coerceIn(0, 100),
                )
            }
        }
        RestoredLibrary(documents, failedCount)
    }

    fun load(id: String): ReadDocument = synchronized(fileLock) {
        val stored = readDocument(file(id))
        synchronized(indexLock) {
            val progress = preferences.getInt(progressKey(id), stored.progress).coerceIn(0, 100)
            stored.copy(
                progress = progress,
                progressPosition = preferences.getInt(
                    progressPositionKey(id),
                    progress * (ProgressPositionScale / 100),
                ).coerceIn(0, ProgressPositionScale),
            )
        }
    }

    fun loadCover(id: String): ByteArray? = synchronized(fileLock) {
        try {
            AtomicFile(file(id)).openRead().buffered().use(DocumentCodec::decodeCover)
        } catch (_: Exception) {
            null
        }
    }

    fun save(document: ReadDocument, makeRecent: Boolean = true) = synchronized(fileLock) {
        require(root.exists() || root.mkdirs()) { "Unable to create document storage" }
        val target = file(document.id)
        val atomicFile = AtomicFile(target)
        val output = atomicFile.startWrite()
        try {
            DocumentCodec.encode(document, output)
            atomicFile.finishWrite(output)
        } catch (error: Exception) {
            atomicFile.failWrite(output)
            throw error
        }
        File("${target.path}.tmp").delete()
        synchronized(indexLock) {
            val ids = documentIdsLocked().toMutableList().apply {
                if (makeRecent || document.id !in this) {
                    remove(document.id)
                    add(0, document.id)
                }
            }
            preferences.edit {
                putString(IndexKey, JSONArray(ids).toString())
                putInt(progressKey(document.id), document.progress.coerceIn(0, 100))
                putInt(
                    progressPositionKey(document.id),
                    document.progressPosition.coerceIn(0, ProgressPositionScale),
                )
            }
        }
    }

    fun updateProgress(id: String, progress: Int, progressPosition: Int) =
        synchronized(indexLock) {
            if (id !in documentIdsLocked()) return@synchronized
            preferences.edit {
                putInt(progressKey(id), progress.coerceIn(0, 100))
                putInt(
                    progressPositionKey(id),
                    progressPosition.coerceIn(0, ProgressPositionScale),
                )
            }
        }

    fun delete(id: String) = synchronized(fileLock) {
        val target = file(id)
        check(target.parentFile?.canonicalFile == root.canonicalFile)
        AtomicFile(target).delete()
        File("${target.path}.tmp").delete()
        synchronized(indexLock) {
            preferences.edit {
                putString(
                    IndexKey,
                    JSONArray(documentIdsLocked().filterNot { it == id }).toString(),
                )
                remove(progressKey(id))
                remove(progressPositionKey(id))
                if (activeDocumentIdLocked() == id) remove(ActiveDocumentKey)
            }
        }
    }

    fun activeDocumentId(): String? = synchronized(indexLock) { activeDocumentIdLocked() }

    fun setActiveDocument(id: String?) = synchronized(indexLock) {
        val ids = documentIdsLocked()
        preferences.edit {
            if (id == null || id !in ids) {
                remove(ActiveDocumentKey)
            } else {
                putString(ActiveDocumentKey, id)
                putString(
                    IndexKey,
                    JSONArray(
                        ids.toMutableList().apply {
                            remove(id)
                            add(0, id)
                        },
                    ).toString(),
                )
            }
        }
    }

    fun storage(): LibraryStorage = synchronized(fileLock) {
        LibraryStorage(
            documentCount = synchronized(indexLock) { documentIdsLocked().size },
            bytes = root.walkTopDown().filter(File::isFile).sumOf(File::length),
        )
    }

    fun clear() = synchronized(fileLock) {
        check(root.canonicalFile == File(root.parentFile, DirectoryName).canonicalFile)
        if (root.exists() && !root.deleteRecursively()) error("Unable to clear library")
        synchronized(indexLock) {
            preferences.edit { clear() }
        }
    }

    private fun recoverAtomicBackups(): Int {
        var failedCount = 0
        root.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.endsWith(".$DocumentExtension.bak") }
            .forEach { backup ->
                val target = File(backup.path.removeSuffix(".bak"))
                if (isValidDocument(backup)) {
                    AtomicFile(target).openRead().close()
                } else {
                    quarantine(backup)
                    failedCount++
                }
            }
        return failedCount
    }

    private fun recoverLegacyTemporaryFiles(): Int {
        var failedCount = 0
        root.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.endsWith(".$DocumentExtension.tmp") }
            .forEach { temporary ->
                val target = File(temporary.path.removeSuffix(".tmp"))
                if (target.exists() && isValidDocument(target)) {
                    temporary.delete()
                    return@forEach
                }
                if (!isValidDocument(temporary)) {
                    quarantine(temporary)
                    return@forEach
                }
                if (target.exists()) {
                    quarantine(target)
                    failedCount++
                }
                if (!temporary.renameTo(target)) {
                    error("Unable to recover document cache")
                }
            }
        return failedCount
    }

    private fun discardUncommittedFiles() {
        root.listFiles()
            .orEmpty()
            .filter { it.isFile && it.name.endsWith(".$DocumentExtension.new") }
            .forEach(File::delete)
    }

    private fun isValidDocument(target: File): Boolean = try {
        target.inputStream().buffered().use(DocumentCodec::decodeSummary).id.sha256() == target.name
            .removeSuffix(".bak")
            .removeSuffix(".tmp")
            .removeSuffix(".$DocumentExtension")
    } catch (_: Exception) {
        false
    }

    private fun quarantine(target: File) {
        if (!target.exists()) return
        require(quarantine.exists() || quarantine.mkdirs()) {
            "Unable to create document quarantine"
        }
        var destination = File(
            quarantine,
            "${target.name}.${System.currentTimeMillis()}.corrupt",
        )
        var suffix = 0
        while (destination.exists()) {
            destination = File(quarantine, "${target.name}.${System.currentTimeMillis()}.${suffix++}")
        }
        if (!target.renameTo(destination)) error("Unable to quarantine document cache")
    }

    private fun readDocument(target: File): ReadDocument =
        AtomicFile(target).openRead().buffered().use(DocumentCodec::decode)

    private fun readSummary(target: File): LibraryDocument =
        AtomicFile(target).openRead().buffered().use(DocumentCodec::decodeSummary)

    private fun documentIdsLocked(): List<String> = try {
        val values = JSONArray(preferences.getString(IndexKey, "[]"))
        buildList(values.length()) {
            repeat(values.length()) { add(values.getString(it)) }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun activeDocumentIdLocked(): String? =
        preferences.getString(ActiveDocumentKey, null)

    private fun file(id: String) = File(root, "${id.sha256()}.$DocumentExtension")
    private fun progressKey(id: String) = "progress_${id.sha256()}"
    private fun progressPositionKey(id: String) = "progress_position_${id.sha256()}"

    private data class StoredSummary(
        val document: LibraryDocument,
        val lastModified: Long,
    )

    private companion object {
        // Process-wide locks; split by document only if storage throughput becomes visible.
        val fileLock = Any()
        val indexLock = Any()
        const val DirectoryName = "documents"
        const val QuarantineDirectoryName = "quarantine"
        const val DocumentExtension = "bin"
        const val IndexKey = "document_index"
        const val ActiveDocumentKey = "active_document"
        const val ProgressKeyPrefix = "progress_"
    }
}

internal object DocumentCodec {
    private const val Magic = 0x52535044
    private const val Version = 1
    private const val MaxStringBytes = 50 * 1024 * 1024
    private const val MaxItems = 10_000
    private const val MaxCoverBytes = 20 * 1024 * 1024

    fun encode(document: ReadDocument, stream: OutputStream) {
        require(document.formattedHtml.size <= MaxItems) {
            "Too many formatted document blocks"
        }
        require(document.chapters.size <= MaxItems) { "Too many document chapters" }
        require(document.cover == null || document.cover.size <= MaxCoverBytes) {
            "Document cover is too large"
        }

        DataOutputStream(stream).apply {
            writeInt(Magic)
            writeInt(Version)
            writeString(document.id)
            writeString(document.title)
            writeBoolean(document.author != null)
            document.author?.let { writeString(it) }
            writeString(document.text)
            writeInt(document.formattedHtml.size)
            document.formattedHtml.forEach { writeString(it) }
            writeInt(document.chapters.size)
            document.chapters.forEach {
                writeString(it.title)
                writeInt(it.startWord)
            }
            writeBoolean(document.cover != null)
            document.cover?.let {
                writeInt(it.size)
                write(it)
            }
            writeInt(document.progress.coerceIn(0, 100))
            flush()
        }
    }

    fun encode(document: ReadDocument): ByteArray =
        ByteArrayOutputStream().use { bytes ->
            encode(document, bytes)
            bytes.toByteArray()
        }

    fun decode(stream: InputStream): ReadDocument {
        val input = DataInputStream(stream)
        input.readHeader()
        val id = input.readString()
        val title = input.readString()
        val author = if (input.readBoolean()) input.readString() else null
        val text = input.readString()
        val htmlCount = input.readCount()
        val formattedHtml = List(htmlCount) { input.readString() }
        val chapterCount = input.readCount()
        val chapters = List(chapterCount) {
            DocumentChapter(input.readString(), input.readInt().coerceAtLeast(0))
        }
        val cover = input.readCover()
        val progress = input.readInt().coerceIn(0, 100)
        require(input.read() == -1) { "Unexpected document cache data" }
        return ReadDocument(
            id = id,
            title = title,
            author = author,
            text = text,
            formattedHtml = formattedHtml,
            cover = cover,
            chapters = chapters,
            progress = progress,
        )
    }

    fun decode(bytes: ByteArray): ReadDocument =
        ByteArrayInputStream(bytes).use(::decode)

    fun decodeSummary(stream: InputStream): LibraryDocument {
        val input = DataInputStream(stream)
        input.readHeader()
        val id = input.readString()
        val title = input.readString()
        val author = if (input.readBoolean()) input.readString() else null
        input.skipString()
        repeat(input.readCount()) { input.skipString() }
        repeat(input.readCount()) {
            input.skipString()
            input.readInt()
        }
        val hasCover = input.skipCover()
        val progress = input.readInt().coerceIn(0, 100)
        require(input.read() == -1) { "Unexpected document cache data" }
        return LibraryDocument(id, title, author, hasCover, progress)
    }

    fun decodeSummary(bytes: ByteArray): LibraryDocument =
        ByteArrayInputStream(bytes).use(::decodeSummary)

    fun decodeCover(stream: InputStream): ByteArray? {
        val input = DataInputStream(stream)
        input.readHeader()
        input.skipString()
        input.skipString()
        if (input.readBoolean()) input.skipString()
        input.skipString()
        repeat(input.readCount()) { input.skipString() }
        repeat(input.readCount()) {
            input.skipString()
            input.readInt()
        }
        val cover = input.readCover()
        input.readInt()
        require(input.read() == -1) { "Unexpected document cache data" }
        return cover
    }

    fun decodeCover(bytes: ByteArray): ByteArray? =
        ByteArrayInputStream(bytes).use(::decodeCover)

    private fun DataInputStream.readHeader() {
        require(readInt() == Magic) { "Invalid document cache" }
        require(readInt() == Version) { "Unsupported document cache" }
    }

    private fun DataOutputStream.writeString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MaxStringBytes) { "Document field is too large" }
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readString(): String {
        val size = readInt()
        require(size in 0..MaxStringBytes) { "Invalid document cache field" }
        return ByteArray(size).also(::readFully).toString(Charsets.UTF_8)
    }

    private fun DataInputStream.skipString() {
        val size = readInt()
        require(size in 0..MaxStringBytes) { "Invalid document cache field" }
        skipFully(size)
    }

    private fun DataInputStream.readCover(): ByteArray? =
        if (readBoolean()) {
            val size = readInt()
            require(size in 0..MaxCoverBytes) { "Invalid cover cache" }
            ByteArray(size).also(::readFully)
        } else {
            null
        }

    private fun DataInputStream.skipCover(): Boolean {
        val hasCover = readBoolean()
        if (hasCover) {
            val size = readInt()
            require(size in 0..MaxCoverBytes) { "Invalid cover cache" }
            skipFully(size)
        }
        return hasCover
    }

    private fun DataInputStream.skipFully(size: Int) {
        var remaining = size
        while (remaining > 0) {
            val skipped = skipBytes(remaining)
            if (skipped == 0) {
                if (read() == -1) error("Truncated document cache")
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }

    private fun DataInputStream.readCount(): Int =
        readInt().also { require(it in 0..MaxItems) { "Invalid document cache count" } }
}

private fun String.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
