package com.example.inkora.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.inkora.data.local.entity.PageTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageTemplateDao {
    @Query("SELECT * FROM page_templates ORDER BY isDefault DESC, name ASC")
    fun getAllTemplates(): Flow<List<PageTemplateEntity>>

    @Query("SELECT * FROM page_templates WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultTemplate(): PageTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: PageTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: PageTemplateEntity)

    @Query("UPDATE page_templates SET isDefault = 0")
    suspend fun clearDefaultFlags()
}
