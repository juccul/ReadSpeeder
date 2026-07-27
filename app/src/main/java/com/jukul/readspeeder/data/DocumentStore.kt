package com.jukul.readspeeder.data

import android.content.Context
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
    private val preferences =
        context.getSharedPreferences("readspeeder_documents", Context.MODE_PRIVATE)

    fun loadSummaries(): RestoredLibrary {
        var failed = 0
        val documents = documentIds().mapNotNull { id ->
            try {
                val stored = file(id).inputStream().buffered().use(DocumentCodec::decodeSummary)
                stored.copy(
                    progress = preferences.getInt(progressKey(id), stored.progress)
                        .coerceIn(0, 100),
                )
            } catch (_: Exception) {
                failed++
                null
            }
        }
        return RestoredLibrary(documents, failed)
    }

    fun load(id: String): ReadDocument {
        val stored = file(id).inputStream().buffered().use(DocumentCodec::decode)
        return stored.copy(
            progress = preferences.getInt(progressKey(id), stored.progress).coerceIn(0, 100),
        )
    }

    fun save(document: ReadDocument, makeRecent: Boolean = true) {
        root.mkdirs()
        val target = file(document.id)
        val temporary = File(root, "${target.name}.tmp")
        val backup = File(root, "${target.name}.bak")
        temporary.outputStream().buffered().use { DocumentCodec.encode(document, it) }
        backup.delete()
        if (target.exists() && !target.renameTo(backup)) {
            temporary.delete()
            error("Unable to update document cache")
        }
        if (!temporary.renameTo(target)) {
            backup.renameTo(target)
            error("Unable to save document cache")
        }
        backup.delete()
        val ids = documentIds().toMutableList().apply {
            if (makeRecent || document.id !in this) {
                remove(document.id)
                add(0, document.id)
            }
        }
        preferences.edit {
            putString(IndexKey, JSONArray(ids).toString())
            putInt(progressKey(document.id), document.progress)
        }
    }

    fun updateProgress(id: String, progress: Int) {
        preferences.edit { putInt(progressKey(id), progress.coerceIn(0, 100)) }
    }

    fun delete(id: String) {
        val target = file(id)
        check(target.parentFile?.canonicalFile == root.canonicalFile)
        if (target.exists() && !target.delete()) error("Unable to delete document")
        preferences.edit {
            putString(
                IndexKey,
                JSONArray(documentIds().filterNot { it == id }).toString(),
            )
            remove(progressKey(id))
            if (activeDocumentId() == id) remove(ActiveDocumentKey)
        }
    }

    fun activeDocumentId(): String? = preferences.getString(ActiveDocumentKey, null)

    fun setActiveDocument(id: String?) {
        preferences.edit {
            if (id == null) {
                remove(ActiveDocumentKey)
            } else {
                putString(ActiveDocumentKey, id)
                val ids = documentIds()
                if (id in ids) {
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
    }

    fun storage(): LibraryStorage = LibraryStorage(
        documentCount = documentIds().size,
        bytes = root.walkTopDown().filter(File::isFile).sumOf(File::length),
    )

    fun clear() {
        check(root.canonicalFile == File(root.parentFile, DirectoryName).canonicalFile)
        if (root.exists() && !root.deleteRecursively()) error("Unable to clear library")
        preferences.edit { clear() }
    }

    private fun documentIds(): List<String> = try {
        val values = JSONArray(preferences.getString(IndexKey, "[]"))
        buildList(values.length()) {
            repeat(values.length()) { add(values.getString(it)) }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun file(id: String) = File(root, "${id.sha256()}.bin")
    private fun progressKey(id: String) = "progress_${id.sha256()}"

    private companion object {
        const val DirectoryName = "documents"
        const val IndexKey = "document_index"
        const val ActiveDocumentKey = "active_document"
    }
}

internal object DocumentCodec {
    private const val Magic = 0x52535044
    private const val Version = 1
    private const val MaxStringBytes = 50 * 1024 * 1024
    private const val MaxItems = 10_000
    private const val MaxCoverBytes = 20 * 1024 * 1024

    fun encode(document: ReadDocument, stream: OutputStream) {
        DataOutputStream(stream).use { output ->
            output.writeInt(Magic)
            output.writeInt(Version)
            output.writeString(document.id)
            output.writeString(document.title)
            output.writeBoolean(document.author != null)
            document.author?.let { output.writeString(it) }
            output.writeString(document.text)
            output.writeInt(document.formattedHtml.size)
            document.formattedHtml.forEach { output.writeString(it) }
            output.writeInt(document.chapters.size)
            document.chapters.forEach {
                output.writeString(it.title)
                output.writeInt(it.startWord)
            }
            output.writeBoolean(document.cover != null)
            document.cover?.let {
                output.writeInt(it.size)
                output.write(it)
            }
            output.writeInt(document.progress.coerceIn(0, 100))
        }
    }

    fun encode(document: ReadDocument): ByteArray =
        ByteArrayOutputStream().use { bytes ->
            encode(document, bytes)
            bytes.toByteArray()
        }

    fun decode(stream: InputStream): ReadDocument =
        DataInputStream(stream).use { input ->
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
            val cover = if (input.readBoolean()) {
                val size = input.readInt()
                require(size in 0..MaxCoverBytes) { "Invalid cover cache" }
                ByteArray(size).also(input::readFully)
            } else {
                null
            }
            val progress = input.readInt().coerceIn(0, 100)
            require(input.read() == -1) { "Unexpected document cache data" }
            ReadDocument(
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

    fun decodeSummary(stream: InputStream): LibraryDocument =
        DataInputStream(stream).use { input ->
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
            val cover = if (input.readBoolean()) {
                val size = input.readInt()
                require(size in 0..MaxCoverBytes) { "Invalid cover cache" }
                ByteArray(size).also(input::readFully)
            } else {
                null
            }
            val progress = input.readInt().coerceIn(0, 100)
            require(input.read() == -1) { "Unexpected document cache data" }
            LibraryDocument(id, title, author, cover, progress)
        }

    fun decodeSummary(bytes: ByteArray): LibraryDocument =
        ByteArrayInputStream(bytes).use(::decodeSummary)

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
        var remaining = readInt()
        require(remaining in 0..MaxStringBytes) { "Invalid document cache field" }
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
