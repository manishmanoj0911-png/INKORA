package com.example.inkora.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.inkora.data.local.entity.NoteTagCrossRef
import com.example.inkora.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsDirect(): List<TagEntity>

    @Query("""
        SELECT t.* FROM tags t 
        INNER JOIN note_tag_cross_ref cr ON t.id = cr.tagId 
        WHERE cr.noteId = :noteId
    """)
    fun getTagsForNote(noteId: String): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId AND tagId = :tagId")
    suspend fun deleteCrossRef(noteId: String, tagId: String)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun clearTagsForNote(noteId: String)

    @Delete
    suspend fun deleteTag(tag: TagEntity)
}
