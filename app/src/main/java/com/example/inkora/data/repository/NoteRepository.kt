package com.example.inkora.data.repository

import com.example.inkora.data.local.InkoraDatabase
import com.example.inkora.data.local.entity.AttachmentEntity
import com.example.inkora.data.local.entity.NoteEntity
import com.example.inkora.data.local.entity.PageEntity
import com.example.inkora.model.PaperBackground
import com.example.inkora.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class NoteRepository(private val database: InkoraDatabase) {
    private val noteDao = database.noteDao()
    private val pageDao = database.pageDao()
    private val attachmentDao = database.attachmentDao()
    private val tagDao = database.tagDao()

    fun getActiveNotes(): Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()
    fun getPinnedNotes(): Flow<List<NoteEntity>> = noteDao.getPinnedNotes()
    fun getFavoriteNotes(): Flow<List<NoteEntity>> = noteDao.getFavoriteNotes()
    fun getTrashNotes(): Flow<List<NoteEntity>> = noteDao.getTrashNotes()
    fun getNotesByFolder(folderId: String): Flow<List<NoteEntity>> = noteDao.getNotesByFolder(folderId)
    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)
    fun getNoteById(id: String): Flow<NoteEntity?> = noteDao.getNoteById(id)
    fun getPagesForNote(noteId: String): Flow<List<PageEntity>> = pageDao.getPagesForNote(noteId)
    fun getAttachmentsForNote(noteId: String): Flow<List<AttachmentEntity>> = attachmentDao.getAttachmentsForNote(noteId)
    fun getTagsForNote(noteId: String) = tagDao.getTagsForNote(noteId)

    suspend fun getNoteDirect(id: String): NoteEntity? = withContext(Dispatchers.IO) {
        noteDao.getNoteByIdDirect(id)
    }

    suspend fun getPagesDirect(noteId: String): List<PageEntity> = withContext(Dispatchers.IO) {
        pageDao.getPagesForNoteDirect(noteId)
    }

    suspend fun createNote(
        title: String = "Untitled Note",
        folderId: String? = null,
        paperBackground: PaperBackground = PaperBackground.ClassicCream,
        fontId: String = "outfit",
        fontSizeSp: Float = 16f,
        initialText: String = "",
        initialChecklists: String = "[]",
        initialTable: String = ""
    ): String = withContext(Dispatchers.IO) {
        val noteId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val snippet = if (initialText.isNotBlank()) {
            initialText.take(120).replace("\n", " ")
        } else {
            "No additional text"
        }

        val note = NoteEntity(
            id = noteId,
            title = title.ifBlank { "Untitled Note" },
            folderId = folderId,
            isPinned = false,
            isFavorite = false,
            isLocked = false,
            isTrash = false,
            colorHex = paperBackground.backgroundColorHex,
            paperType = paperBackground.type.name,
            paperColorHex = paperBackground.backgroundColorHex,
            paperLineColorHex = paperBackground.lineColorHex,
            paperSpacing = paperBackground.spacingDp,
            fontId = fontId,
            fontSizeSp = fontSizeSp,
            syncState = SyncStatus.PENDING_UPLOAD.name,
            pageCount = 1,
            previewSnippet = snippet,
            createdAt = now,
            modifiedAt = now
        )
        noteDao.insertNote(note)

        val firstPage = PageEntity(
            noteId = noteId,
            pageIndex = 0,
            bodyText = initialText,
            checklistJson = initialChecklists,
            tableJson = initialTable,
            strokesJson = "[]",
            createdAt = now,
            modifiedAt = now
        )
        pageDao.insertPage(firstPage)

        noteId
    }

    suspend fun saveNote(note: NoteEntity, pages: List<PageEntity>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val updatedNote = note.copy(
            modifiedAt = now,
            syncState = SyncStatus.PENDING_UPLOAD.name,
            pageCount = pages.size
        )
        noteDao.insertNote(updatedNote)
        pages.forEach { page ->
            pageDao.insertPage(page.copy(modifiedAt = now))
        }
    }

    suspend fun updateNotePaperStyle(noteId: String, paper: PaperBackground) = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteByIdDirect(noteId) ?: return@withContext
        val updated = note.copy(
            paperType = paper.type.name,
            paperColorHex = paper.backgroundColorHex,
            paperLineColorHex = paper.lineColorHex,
            paperSpacing = paper.spacingDp,
            modifiedAt = System.currentTimeMillis(),
            syncState = SyncStatus.PENDING_UPLOAD.name
        )
        noteDao.insertNote(updated)
    }

    suspend fun updateNoteFont(noteId: String, fontId: String, fontSizeSp: Float) = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteByIdDirect(noteId) ?: return@withContext
        val updated = note.copy(
            fontId = fontId,
            fontSizeSp = fontSizeSp,
            modifiedAt = System.currentTimeMillis(),
            syncState = SyncStatus.PENDING_UPLOAD.name
        )
        noteDao.insertNote(updated)
    }

    suspend fun setTrash(noteId: String, isTrash: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setTrashStatus(noteId, isTrash)
        noteDao.updateSyncState(noteId, SyncStatus.PENDING_UPLOAD.name)
    }

    suspend fun setFavorite(noteId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setFavoriteStatus(noteId, isFavorite)
        noteDao.updateSyncState(noteId, SyncStatus.PENDING_UPLOAD.name)
    }

    suspend fun setPinned(noteId: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setPinnedStatus(noteId, isPinned)
        noteDao.updateSyncState(noteId, SyncStatus.PENDING_UPLOAD.name)
    }

    suspend fun setLocked(noteId: String, isLocked: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setLockedStatus(noteId, isLocked)
    }

    suspend fun deletePermanently(noteId: String) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(noteId)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        noteDao.emptyTrash()
    }

    suspend fun addPage(noteId: String, afterIndex: Int): PageEntity = withContext(Dispatchers.IO) {
        val existing = pageDao.getPagesForNoteDirect(noteId)
        val newIndex = afterIndex + 1

        // Shift indices if needed
        existing.filter { it.pageIndex >= newIndex }.forEach { page ->
            pageDao.insertPage(page.copy(pageIndex = page.pageIndex + 1))
        }

        val newPage = PageEntity(
            noteId = noteId,
            pageIndex = newIndex,
            bodyText = "",
            checklistJson = "[]",
            tableJson = "",
            strokesJson = "[]",
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )
        pageDao.insertPage(newPage)

        val note = noteDao.getNoteByIdDirect(noteId)
        if (note != null) {
            noteDao.insertNote(note.copy(pageCount = existing.size + 1, modifiedAt = System.currentTimeMillis()))
        }
        newPage
    }

    suspend fun deletePage(noteId: String, pageId: String): Boolean = withContext(Dispatchers.IO) {
        val existing = pageDao.getPagesForNoteDirect(noteId)
        if (existing.size <= 1) return@withContext false // cannot delete the only page

        pageDao.deletePageById(pageId)
        val remaining = pageDao.getPagesForNoteDirect(noteId).sortedBy { it.pageIndex }
        // Re-index pages
        remaining.forEachIndexed { idx, page ->
            if (page.pageIndex != idx) {
                pageDao.insertPage(page.copy(pageIndex = idx))
            }
        }

        val note = noteDao.getNoteByIdDirect(noteId)
        if (note != null) {
            noteDao.insertNote(note.copy(pageCount = remaining.size, modifiedAt = System.currentTimeMillis()))
        }
        true
    }

    suspend fun addAttachment(attachment: AttachmentEntity) = withContext(Dispatchers.IO) {
        attachmentDao.insertAttachment(attachment)
    }

    suspend fun deleteAttachment(id: String) = withContext(Dispatchers.IO) {
        attachmentDao.deleteAttachmentById(id)
    }
}
