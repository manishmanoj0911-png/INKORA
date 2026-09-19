package com.example.inkora.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.inkora.data.local.entity.PageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE noteId = :noteId ORDER BY pageIndex ASC")
    fun getPagesForNote(noteId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE noteId = :noteId ORDER BY pageIndex ASC")
    suspend fun getPagesForNoteDirect(noteId: String): List<PageEntity>

    @Query("SELECT * FROM pages WHERE noteId = :noteId AND pageIndex = :index LIMIT 1")
    suspend fun getPageByIndex(noteId: String, index: Int): PageEntity?

    @Query("SELECT * FROM pages WHERE id = :pageId LIMIT 1")
    suspend fun getPageById(pageId: String): PageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>)

    @Update
    suspend fun updatePage(page: PageEntity)

    @Delete
    suspend fun deletePage(page: PageEntity)

    @Query("DELETE FROM pages WHERE noteId = :noteId")
    suspend fun deleteAllPagesForNote(noteId: String)

    @Query("DELETE FROM pages WHERE id = :pageId")
    suspend fun deletePageById(pageId: String)

    @Query("SELECT * FROM pages")
    suspend fun getAllPagesDirect(): List<PageEntity>
}
