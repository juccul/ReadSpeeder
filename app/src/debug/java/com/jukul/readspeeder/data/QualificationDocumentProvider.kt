package com.jukul.readspeeder.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File

class QualificationDocumentProvider : ContentProvider() {
    private val root: File
        get() = File(requireNotNull(context).cacheDir, "qualification-documents")

    override fun onCreate(): Boolean {
        root.mkdirs()
        return true
    }

    override fun getType(uri: Uri): String? = uri.getQueryParameter(MimeType)

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val file = file(uri)
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(columns).apply {
            addRow(
                columns.map { column ->
                    when (column) {
                        OpenableColumns.DISPLAY_NAME -> uri.getQueryParameter(Name)
                        OpenableColumns.SIZE -> file.length()
                        else -> null
                    }
                },
            )
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor =
        ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY)

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val safeValues = requireNotNull(values)
        val key = System.nanoTime().toString()
        File(root, key).writeBytes(requireNotNull(safeValues.getAsByteArray(Contents)))
        return RootUri.buildUpon()
            .appendPath(key)
            .appendQueryParameter(Name, requireNotNull(safeValues.getAsString(Name)))
            .appendQueryParameter(MimeType, requireNotNull(safeValues.getAsString(MimeType)))
            .build()
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val files = root.listFiles().orEmpty()
        root.deleteRecursively()
        root.mkdirs()
        return files.size
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private fun file(uri: Uri): File = File(root, requireNotNull(uri.lastPathSegment))

    companion object {
        private const val Authority = "com.jukul.readspeeder.test.documents"
        val RootUri: Uri = Uri.parse("content://$Authority")
        const val Name = "name"
        const val MimeType = "mime_type"
        const val Contents = "contents"
    }
}
