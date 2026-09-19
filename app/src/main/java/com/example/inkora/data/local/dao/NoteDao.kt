package com.example.inkora.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.inkora.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isTrash = 0 ORDER BY isPinned DESC, modifiedAt DESC")
    fun getAllActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrash = 0 AND isPinned = 1 ORDER BY modifiedAt DESC")
    fun getPinnedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrash = 0 AND isFavorite = 1 ORDER BY modifiedAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrash = 0 AND folderId = :folderId ORDER BY isPinned DESC, modifiedAt DESC")
    fun getNotesByFolder(folderId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrash = 1 ORDER BY modifiedAt DESC")
    fun getTrashNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdDirect(id: String): NoteEntity?

    @Query("""
        SELECT * FROM notes 
        WHERE isTrash = 0 AND (
            title LIKE '%' || :query || '%' 
            OR previewSnippet LIKE '%' || :query || '%'
        )
        ORDER BY modifiedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE syncState != 'SYNCED'")
    suspend fun getUnsyncedNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesDirect(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET isTrash = :isTrash, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun setTrashStatus(id: String, isTrash: Boolean, modifiedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isFavorite = :isFavorite, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun setFavoriteStatus(id: String, isFavorite: Boolean, modifiedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isPinned = :isPinned, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun setPinnedStatus(id: String, isPinned: Boolean, modifiedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isLocked = :isLocked, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun setLockedStatus(id: String, isLocked: Boolean, modifiedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET syncState = :syncState, syncTimestamp = :syncTime WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String, syncTime: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("DELETE FROM notes WHERE isTrash = 1")
    suspend fun emptyTrash()
}
