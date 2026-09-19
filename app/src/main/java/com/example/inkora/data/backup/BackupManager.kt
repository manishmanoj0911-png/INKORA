package com.example.inkora.data.backup

import android.content.Context
import android.os.Environment
import com.example.inkora.data.local.InkoraDatabase
import com.example.inkora.data.local.entity.FolderEntity
import com.example.inkora.data.local.entity.NoteEntity
import com.example.inkora.data.local.entity.PageEntity
import com.example.inkora.data.local.entity.TagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupFileInfo(
    val file: File,
    val name: String,
    val formattedDate: String,
    val sizeBytes: Long,
    val noteCount: Int
)

class BackupManager(private val context: Context, private val database: InkoraDatabase) {

    private val backupDir: File
        get() {
            val docDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val dir = File(docDir, "Inkora/Backups")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    suspend fun createBackup(): File = withContext(Dispatchers.IO) {
        val notes = database.noteDao().getAllNotesDirect()
        val pages = database.pageDao().getAllPagesDirect()
        val folders = database.folderDao().getAllFoldersDirect()
        val tags = database.tagDao().getAllTagsDirect()
        val attachments = database.attachmentDao().getAllAttachmentsDirect()

        val rootObj = JSONObject()
        rootObj.put("version", 1)
        rootObj.put("appName", "Inkora")
        rootObj.put("timestamp", System.currentTimeMillis())
        rootObj.put("noteCount", notes.size)

        // Notes Array
        val notesArray = JSONArray()
        notes.forEach { n ->
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("title", n.title)
            obj.put("folderId", n.folderId ?: "")
            obj.put("isPinned", n.isPinned)
            obj.put("isFavorite", n.isFavorite)
            obj.put("isLocked", n.isLocked)
            obj.put("isTrash", n.isTrash)
            obj.put("paperType", n.paperType)
            obj.put("paperColorHex", n.paperColorHex)
            obj.put("paperLineColorHex", n.paperLineColorHex)
            obj.put("paperSpacing", n.paperSpacing.toDouble())
            obj.put("fontId", n.fontId)
            obj.put("fontSizeSp", n.fontSizeSp.toDouble())
            obj.put("pageCount", n.pageCount)
            obj.put("previewSnippet", n.previewSnippet)
            obj.put("createdAt", n.createdAt)
            obj.put("modifiedAt", n.modifiedAt)
            notesArray.put(obj)
        }
        rootObj.put("notes", notesArray)

        // Pages Array
        val pagesArray = JSONArray()
        pages.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("noteId", p.noteId)
            obj.put("pageIndex", p.pageIndex)
            obj.put("bodyText", p.bodyText)
            obj.put("checklistJson", p.checklistJson)
            obj.put("tableJson", p.tableJson)
            obj.put("strokesJson", p.strokesJson)
            obj.put("createdAt", p.createdAt)
            obj.put("modifiedAt", p.modifiedAt)
            pagesArray.put(obj)
        }
        rootObj.put("pages", pagesArray)

        // Folders
        val foldersArray = JSONArray()
        folders.forEach { f ->
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("name", f.name)
            obj.put("colorHex", f.colorHex)
            obj.put("iconName", f.iconName)
            foldersArray.put(obj)
        }
        rootObj.put("folders", foldersArray)

        // Tags
        val tagsArray = JSONArray()
        tags.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("colorHex", t.colorHex)
            tagsArray.put(obj)
        }
        rootObj.put("tags", tagsArray)

        val timestampStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val backupFile = File(backupDir, "Inkora_Backup_$timestampStr.json")
        backupFile.writeText(rootObj.toString(2))
        backupFile
    }

    suspend fun getBackupList(): List<BackupFileInfo> = withContext(Dispatchers.IO) {
        val files = backupDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        files.map { file ->
            var noteCount = 0
            try {
                val json = JSONObject(file.readText())
                noteCount = json.optInt("noteCount", 0)
            } catch (_: Exception) {}

            val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
            BackupFileInfo(
                file = file,
                name = file.name,
                formattedDate = dateStr,
                sizeBytes = file.length(),
                noteCount = noteCount
            )
        }.sortedByDescending { it.file.lastModified() }
    }

    suspend fun restoreBackup(file: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val content = file.readText()
            val root = JSONObject(content)
            val notesArray = root.getJSONArray("notes")
            val pagesArray = root.getJSONArray("pages")
            val foldersArray = root.optJSONArray("folders") ?: JSONArray()
            val tagsArray = root.optJSONArray("tags") ?: JSONArray()

            // Restore folders
            for (i in 0 until foldersArray.length()) {
                val obj = foldersArray.getJSONObject(i)
                database.folderDao().insertFolder(
                    FolderEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        colorHex = obj.optString("colorHex", "#4F46E5"),
                        iconName = obj.optString("iconName", "folder")
                    )
                )
            }

            // Restore tags
            for (i in 0 until tagsArray.length()) {
                val obj = tagsArray.getJSONObject(i)
                database.tagDao().insertTag(
                    TagEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        colorHex = obj.optString("colorHex", "#0D9488")
                    )
                )
            }

            // Restore notes
            for (i in 0 until notesArray.length()) {
                val obj = notesArray.getJSONObject(i)
                val note = NoteEntity(
                    id = obj.getString("id"),
                    title = obj.optString("title", "Untitled"),
                    folderId = if (obj.optString("folderId").isNotBlank()) obj.optString("folderId") else null,
                    isPinned = obj.optBoolean("isPinned", false),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    isLocked = obj.optBoolean("isLocked", false),
                    isTrash = obj.optBoolean("isTrash", false),
                    paperType = obj.optString("paperType", "RULED"),
                    paperColorHex = obj.optString("paperColorHex", "#FFFDF9"),
                    paperLineColorHex = obj.optString("paperLineColorHex", "#E2E8F0"),
                    paperSpacing = obj.optDouble("paperSpacing", 28.0).toFloat(),
                    fontId = obj.optString("fontId", "outfit"),
                    fontSizeSp = obj.optDouble("fontSizeSp", 16.0).toFloat(),
                    pageCount = obj.optInt("pageCount", 1),
                    previewSnippet = obj.optString("previewSnippet", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    modifiedAt = obj.optLong("modifiedAt", System.currentTimeMillis())
                )
                database.noteDao().insertNote(note)
            }

            // Restore pages
            for (i in 0 until pagesArray.length()) {
                val obj = pagesArray.getJSONObject(i)
                val page = PageEntity(
                    id = obj.getString("id"),
                    noteId = obj.getString("noteId"),
                    pageIndex = obj.optInt("pageIndex", 0),
                    bodyText = obj.optString("bodyText", ""),
                    checklistJson = obj.optString("checklistJson", "[]"),
                    tableJson = obj.optString("tableJson", ""),
                    strokesJson = obj.optString("strokesJson", "[]"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    modifiedAt = obj.optLong("modifiedAt", System.currentTimeMillis())
                )
                database.pageDao().insertPage(page)
            }

            Result.success(notesArray.length())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBackup(file: File): Boolean = withContext(Dispatchers.IO) {
        file.delete()
    }
}
