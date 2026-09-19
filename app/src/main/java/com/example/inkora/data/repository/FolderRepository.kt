package com.example.inkora.data.repository

import com.example.inkora.data.local.InkoraDatabase
import com.example.inkora.data.local.entity.FolderEntity
import com.example.inkora.data.local.entity.NoteTagCrossRef
import com.example.inkora.data.local.entity.PageTemplateEntity
import com.example.inkora.data.local.entity.TagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FolderRepository(database: InkoraDatabase) {
    private val folderDao = database.folderDao()
    private val tagDao = database.tagDao()
    private val templateDao = database.pageTemplateDao()

    fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAllFolders()
    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()
    fun getAllTemplates(): Flow<List<PageTemplateEntity>> = templateDao.getAllTemplates()

    suspend fun createFolder(name: String, colorHex: String = "#4F46E5", iconName: String = "folder") = withContext(Dispatchers.IO) {
        val folder = FolderEntity(name = name, colorHex = colorHex, iconName = iconName)
        folderDao.insertFolder(folder)
    }

    suspend fun updateFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        folderDao.updateFolder(folder)
    }

    suspend fun deleteFolder(id: String) = withContext(Dispatchers.IO) {
        folderDao.deleteFolderById(id)
    }

    suspend fun createTag(name: String, colorHex: String = "#0D9488") = withContext(Dispatchers.IO) {
        val tag = TagEntity(name = name, colorHex = colorHex)
        tagDao.insertTag(tag)
    }

    suspend fun deleteTag(tag: TagEntity) = withContext(Dispatchers.IO) {
        tagDao.deleteTag(tag)
    }

    suspend fun addTagToNote(noteId: String, tagId: String) = withContext(Dispatchers.IO) {
        tagDao.insertCrossRef(NoteTagCrossRef(noteId, tagId))
    }

    suspend fun removeTagFromNote(noteId: String, tagId: String) = withContext(Dispatchers.IO) {
        tagDao.deleteCrossRef(noteId, tagId)
    }

    suspend fun createTemplate(template: PageTemplateEntity) = withContext(Dispatchers.IO) {
        if (template.isDefault) {
            templateDao.clearDefaultFlags()
        }
        templateDao.insertTemplate(template)
    }

    suspend fun deleteTemplate(template: PageTemplateEntity) = withContext(Dispatchers.IO) {
        templateDao.deleteTemplate(template)
    }
}
