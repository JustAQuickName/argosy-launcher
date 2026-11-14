package com.nendo.argosy.data.localpc

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalPcDocumentService @Inject constructor(
    private val contentResolver: ContentResolver,
    private val repository: LocalPcRepository
) {
    suspend fun previewMarker(uri: Uri): LocalPcImportPreview = withContext(Dispatchers.IO) {
        val name = runCatching { queryName(uri) }.getOrNull() ?: uri.lastPathSegment.orEmpty()
        val draft = runCatching { readText(uri) }
            .mapCatching { LocalPcMarkerParser.parseGameNative(name, it) }
            .getOrNull()
        preview(listOf(name to draft), if (draft == null) listOf(name) else emptyList())
    }

    suspend fun previewTree(treeUri: Uri): LocalPcImportPreview = withContext(Dispatchers.IO) {
        val parsed = mutableListOf<Pair<String, LocalPcEntryDraft?>>()
        val unreadable = mutableListOf<String>()
        walkTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri), parsed, unreadable)
        preview(parsed, unreadable)
    }

    suspend fun import(preview: LocalPcImportPreview): LocalPcImportResult =
        repository.import(preview.items.mapNotNull { it.draft })

    suspend fun readBackup(uri: Uri): LocalPcBackup = withContext(Dispatchers.IO) {
        LocalPcBackupCodec.decode(readText(uri))
    }

    suspend fun writeBackup(uri: Uri) = withContext(Dispatchers.IO) {
        val text = LocalPcBackupCodec.encode(repository.exportBackup())
        contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) }
            ?: throw IOException("Unable to open backup destination")
    }

    private suspend fun preview(
        sources: List<Pair<String, LocalPcEntryDraft?>>,
        unreadable: List<String>
    ): LocalPcImportPreview {
        val existing = repository.getAll()
        val items = sources.map { (name, draft) ->
            if (draft == null) {
                LocalPcImportPreviewItem(null, name, LocalPcImportDisposition.REJECTED)
            } else {
                val fingerprint = draft.sourceFingerprint ?: LocalPcMarkerParser.fingerprint(draft.target)
                val row = existing.firstOrNull { it.entry.sourceFingerprint == fingerprint }
                val sameTarget = row?.targets?.any {
                    it.runnerType == draft.target.runnerType.token &&
                        it.runnerGameId == draft.target.runnerGameId &&
                        it.packageName == draft.target.packageName
                } == true
                val disposition = when {
                    row == null -> LocalPcImportDisposition.ADD
                    sameTarget -> LocalPcImportDisposition.UNCHANGED
                    else -> LocalPcImportDisposition.ADD_TARGET
                }
                LocalPcImportPreviewItem(draft, name, disposition, row?.entry?.gameId)
            }
        }
        return LocalPcImportPreview(items, unreadable.distinct())
    }

    private fun walkTree(
        treeUri: Uri,
        parentId: String,
        parsed: MutableList<Pair<String, LocalPcEntryDraft?>>,
        unreadable: MutableList<String>
    ) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        val cursor = runCatching { contentResolver.query(children, projection, null, null, null) }.getOrNull()
        if (cursor == null) {
            unreadable += parentId
            return
        }
        cursor.use { rows ->
            while (rows.moveToNext()) {
                val id = rows.getString(0)
                val name = rows.getString(1) ?: id
                val mime = rows.getString(2)
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    walkTree(treeUri, id, parsed, unreadable)
                } else if (name.endsWith(".pcgame", ignoreCase = true)) {
                    val document = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    val draft = runCatching { readText(document) }
                        .mapCatching { LocalPcMarkerParser.parseGameNative(name, it) }
                        .getOrNull()
                    parsed += name to draft
                    if (draft == null) unreadable += name
                }
            }
        }
    }

    private fun readText(uri: Uri): String =
        contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IOException("Unable to open document")

    private fun queryName(uri: Uri): String? =
        contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
}
