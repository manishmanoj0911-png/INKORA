package com.example.inkora.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.inkora.data.auth.AuthManager
import com.example.inkora.data.local.InkoraDatabase
import com.example.inkora.data.local.entity.NoteEntity
import com.example.inkora.data.local.entity.PageEntity
import com.example.inkora.model.SyncStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class SyncEngineState(
    val status: SyncStatus = SyncStatus.SYNCED,
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val message: String = "All notes saved locally"
)

class FirebaseSyncEngine(
    private val context: Context,
    private val database: InkoraDatabase,
    private val authManager: AuthManager
) {
    private val noteDao = database.noteDao()
    private val pageDao = database.pageDao()

    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (_: Exception) {
        null
    }

    private val _syncState = MutableStateFlow(SyncEngineState())
    val syncState: StateFlow<SyncEngineState> = _syncState.asStateFlow()

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun triggerSync(force: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        val user = authManager.currentUser.value
        val unsyncedNotes = noteDao.getUnsyncedNotes()
        val count = unsyncedNotes.size

        if (!isOnline()) {
            _syncState.value = _syncState.value.copy(
                status = if (count > 0) SyncStatus.PENDING_UPLOAD else SyncStatus.SYNCED,
                isSyncing = false,
                pendingCount = count,
                message = if (count > 0) "Offline: $count changes pending sync" else "Offline: All notes saved locally"
            )
            return@withContext Result.success(Unit)
        }

        if (firestore == null || user == null) {
            _syncState.value = _syncState.value.copy(
                status = if (count > 0) SyncStatus.PENDING_UPLOAD else SyncStatus.SYNCED,
                isSyncing = false,
                pendingCount = count,
                message = if (count > 0) "$count local changes ready to sync" else "Local storage active"
            )
            return@withContext Result.success(Unit)
        }

        _syncState.value = _syncState.value.copy(
            isSyncing = true,
            status = SyncStatus.PENDING_UPLOAD,
            message = "Syncing with cloud..."
        )

        try {
            val userNotesCollection = firestore.collection("users").document(user.uid).collection("notes")

            // 1. Upload local pending notes
            for (note in unsyncedNotes) {
                if (note.isTrash && note.syncState == SyncStatus.PENDING_DELETE.name) {
                    try {
                        userNotesCollection.document(note.id).delete().await()
                    } catch (_: Exception) {}
                    noteDao.deleteNoteById(note.id)
                } else {
                    val pages = pageDao.getPagesForNoteDirect(note.id)
                    val noteMap = hashMapOf(
                        "id" to note.id,
                        "title" to note.title,
                        "folderId" to (note.folderId ?: ""),
                        "isPinned" to note.isPinned,
                        "isFavorite" to note.isFavorite,
                        "isLocked" to note.isLocked,
                        "isTrash" to note.isTrash,
                        "paperType" to note.paperType,
                        "paperColorHex" to note.paperColorHex,
                        "paperLineColorHex" to note.paperLineColorHex,
                        "paperSpacing" to note.paperSpacing,
                        "fontId" to note.fontId,
                        "fontSizeSp" to note.fontSizeSp,
                        "previewSnippet" to note.previewSnippet,
                        "createdAt" to note.createdAt,
                        "modifiedAt" to note.modifiedAt,
                        "pages" to pages.map { p ->
                            hashMapOf(
                                "id" to p.id,
                                "pageIndex" to p.pageIndex,
                                "bodyText" to p.bodyText,
                                "checklistJson" to p.checklistJson,
                                "tableJson" to p.tableJson,
                                "strokesJson" to p.strokesJson
                            )
                        }
                    )

                    userNotesCollection.document(note.id).set(noteMap, SetOptions.merge()).await()
                    noteDao.updateSyncState(note.id, SyncStatus.SYNCED.name, System.currentTimeMillis())
                }
            }

            // 2. Check remote updates
            val remoteDocs = userNotesCollection.get().await()
            for (doc in remoteDocs.documents) {
                val remoteModifiedAt = doc.getLong("modifiedAt") ?: 0L
                val localNote = noteDao.getNoteByIdDirect(doc.id)

                if (localNote == null) {
                    // Pull remote note to local Room
                    val newNote = NoteEntity(
                        id = doc.id,
                        title = doc.getString("title") ?: "Untitled Note",
                        folderId = doc.getString("folderId")?.takeIf { it.isNotBlank() },
                        isPinned = doc.getBoolean("isPinned") ?: false,
                        isFavorite = doc.getBoolean("isFavorite") ?: false,
                        isLocked = doc.getBoolean("isLocked") ?: false,
                        isTrash = doc.getBoolean("isTrash") ?: false,
                        paperType = doc.getString("paperType") ?: "RULED",
                        paperColorHex = doc.getString("paperColorHex") ?: "#FFFDF9",
                        paperLineColorHex = doc.getString("paperLineColorHex") ?: "#E2E8F0",
                        paperSpacing = (doc.getDouble("paperSpacing") ?: 28.0).toFloat(),
                        fontId = doc.getString("fontId") ?: "outfit",
                        fontSizeSp = (doc.getDouble("fontSizeSp") ?: 16.0).toFloat(),
                        previewSnippet = doc.getString("previewSnippet") ?: "",
                        syncState = SyncStatus.SYNCED.name,
                        syncTimestamp = System.currentTimeMillis(),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        modifiedAt = remoteModifiedAt
                    )
                    noteDao.insertNote(newNote)

                    @Suppress("UNCHECKED_CAST")
                    val remotePages = doc.get("pages") as? List<Map<String, Any>>
                    remotePages?.forEach { pMap ->
                        pageDao.insertPage(
                            PageEntity(
                                id = pMap["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                                noteId = doc.id,
                                pageIndex = (pMap["pageIndex"] as? Long)?.toInt() ?: 0,
                                bodyText = pMap["bodyText"] as? String ?: "",
                                checklistJson = pMap["checklistJson"] as? String ?: "[]",
                                tableJson = pMap["tableJson"] as? String ?: "",
                                strokesJson = pMap["strokesJson"] as? String ?: "[]",
                                createdAt = remoteModifiedAt,
                                modifiedAt = remoteModifiedAt
                            )
                        )
                    }
                } else if (remoteModifiedAt > localNote.modifiedAt && localNote.syncState == SyncStatus.SYNCED.name) {
                    // Update from remote if remote is newer and no local pending edits
                    val updatedNote = localNote.copy(
                        title = doc.getString("title") ?: localNote.title,
                        isPinned = doc.getBoolean("isPinned") ?: localNote.isPinned,
                        isFavorite = doc.getBoolean("isFavorite") ?: localNote.isFavorite,
                        paperType = doc.getString("paperType") ?: localNote.paperType,
                        previewSnippet = doc.getString("previewSnippet") ?: localNote.previewSnippet,
                        modifiedAt = remoteModifiedAt,
                        syncState = SyncStatus.SYNCED.name,
                        syncTimestamp = System.currentTimeMillis()
                    )
                    noteDao.insertNote(updatedNote)
                }
            }

            val remaining = noteDao.getUnsyncedNotes().size
            _syncState.value = SyncEngineState(
                status = if (remaining == 0) SyncStatus.SYNCED else SyncStatus.PENDING_UPLOAD,
                isSyncing = false,
                pendingCount = remaining,
                lastSyncTime = System.currentTimeMillis(),
                message = if (remaining == 0) "All notes synced with cloud" else "$remaining items pending"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            val remaining = noteDao.getUnsyncedNotes().size
            _syncState.value = SyncEngineState(
                status = SyncStatus.ERROR,
                isSyncing = false,
                pendingCount = remaining,
                lastSyncTime = _syncState.value.lastSyncTime,
                message = "Cloud sync paused. Notes safely preserved locally."
            )
            Result.failure(e)
        }
    }
}
