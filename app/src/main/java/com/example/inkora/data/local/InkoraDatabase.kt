package com.example.inkora.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.inkora.data.local.dao.AttachmentDao
import com.example.inkora.data.local.dao.FolderDao
import com.example.inkora.data.local.dao.NoteDao
import com.example.inkora.data.local.dao.PageDao
import com.example.inkora.data.local.dao.PageTemplateDao
import com.example.inkora.data.local.dao.TagDao
import com.example.inkora.data.local.entity.AttachmentEntity
import com.example.inkora.data.local.entity.FolderEntity
import com.example.inkora.data.local.entity.NoteEntity
import com.example.inkora.data.local.entity.NoteTagCrossRef
import com.example.inkora.data.local.entity.PageEntity
import com.example.inkora.data.local.entity.PageTemplateEntity
import com.example.inkora.data.local.entity.TagEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        NoteEntity::class,
        PageEntity::class,
        FolderEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        AttachmentEntity::class,
        PageTemplateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class InkoraDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun pageDao(): PageDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun pageTemplateDao(): PageTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: InkoraDatabase? = null

        fun getInstance(context: Context): InkoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InkoraDatabase::class.java,
                    "inkora_notes.db"
                )
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: InkoraDatabase) {
                val folderDao = database.folderDao()
                val tagDao = database.tagDao()
                val noteDao = database.noteDao()
                val pageDao = database.pageDao()
                val templateDao = database.pageTemplateDao()

                val workFolder = FolderEntity(name = "Work & Projects", colorHex = "#3B82F6", iconName = "work")
                val personalFolder = FolderEntity(name = "Personal", colorHex = "#10B981", iconName = "person")
                val ideasFolder = FolderEntity(name = "Creative Ideas", colorHex = "#F59E0B", iconName = "lightbulb")
                val studyFolder = FolderEntity(name = "Study & Research", colorHex = "#8B5CF6", iconName = "school")

                folderDao.insertFolder(workFolder)
                folderDao.insertFolder(personalFolder)
                folderDao.insertFolder(ideasFolder)
                folderDao.insertFolder(studyFolder)

                val tagUrgent = TagEntity(name = "Important", colorHex = "#EF4444")
                val tagMeeting = TagEntity(name = "Meeting", colorHex = "#3B82F6")
                val tagDraft = TagEntity(name = "Draft", colorHex = "#6B7280")
                val tagJournal = TagEntity(name = "Journal", colorHex = "#8B5CF6")

                tagDao.insertTag(tagUrgent)
                tagDao.insertTag(tagMeeting)
                tagDao.insertTag(tagDraft)
                tagDao.insertTag(tagJournal)

                val templateRuled = PageTemplateEntity(
                    name = "Classic Ruled",
                    paperType = "RULED",
                    paperColorHex = "#FFFDF9",
                    paperLineColorHex = "#E2E8F0",
                    paperSpacing = 28f,
                    fontId = "outfit",
                    fontSizeSp = 16f,
                    isDefault = true
                )
                val templateCornell = PageTemplateEntity(
                    name = "Cornell Study",
                    paperType = "CORNELL",
                    paperColorHex = "#FFFDF7",
                    paperLineColorHex = "#CBD5E1",
                    paperSpacing = 26f,
                    fontId = "lora",
                    fontSizeSp = 16f,
                    isDefault = false
                )
                val templateGrid = PageTemplateEntity(
                    name = "Engineering Grid",
                    paperType = "GRID",
                    paperColorHex = "#FFFFFF",
                    paperLineColorHex = "#E2E8F0",
                    paperSpacing = 24f,
                    fontId = "fira_code",
                    fontSizeSp = 15f,
                    isDefault = false
                )
                val templateDark = PageTemplateEntity(
                    name = "Midnight Slate",
                    paperType = "DOTTED",
                    paperColorHex = "#131720",
                    paperLineColorHex = "#283042",
                    paperSpacing = 24f,
                    fontId = "outfit",
                    fontSizeSp = 16f,
                    isDefault = false
                )

                templateDao.insertTemplate(templateRuled)
                templateDao.insertTemplate(templateCornell)
                templateDao.insertTemplate(templateGrid)
                templateDao.insertTemplate(templateDark)

                // Welcome note to demonstrate full capabilities immediately
                val welcomeNote = NoteEntity(
                    title = "Welcome to Inkora ✨",
                    folderId = ideasFolder.id,
                    isPinned = true,
                    isFavorite = true,
                    isLocked = false,
                    colorHex = "#FFFDF9",
                    paperType = "RULED",
                    paperColorHex = "#FFFDF9",
                    paperLineColorHex = "#E2E8F0",
                    paperSpacing = 28f,
                    fontId = "outfit",
                    fontSizeSp = 16f,
                    syncState = "SYNCED",
                    pageCount = 1,
                    previewSnippet = "Welcome to Inkora — Write. Create. Remember. Smooth handwriting, rich text, and paper styles.",
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis()
                )
                noteDao.insertNote(welcomeNote)
                tagDao.insertCrossRef(NoteTagCrossRef(welcomeNote.id, tagJournal.id))

                val welcomePage = PageEntity(
                    noteId = welcomeNote.id,
                    pageIndex = 0,
                    bodyText = "Welcome to Inkora!\n\nTagline: Write. Create. Remember.\n\nInkora is an offline-first notes application designed for fluid thinking, stylus handwriting, rich text editing, and beautiful paper styles.\n\nKey Highlights:\n• Offline-First: Works completely offline with zero latency.\n• Stylus & Touch: Pen, Pencil, Highlighter, Eraser with pressure response and stroke smoothing.\n• Custom Paper: Ruled, Grid, Dotted, Cornell Notes, Graph, and customizable paper colors.\n• Handwriting to Text: Lasso-select strokes and tap Convert to Text.\n• Rich Media: Tables, checklists, voice memos, and image attachments.\n• Cloud Sync: Background sync with Firebase when connected.",
                    checklistJson = """[{"id":"c1","text":"Explore paper styles and background colors","isChecked":true},{"id":"c2","text":"Try the stylus pen and highlighter","isChecked":false},{"id":"c3","text":"Test handwriting-to-text recognition","isChecked":false},{"id":"c4","text":"Record a voice memo in note","isChecked":false}]""",
                    tableJson = """{"rows":[["Feature","Offline Ready?","Details"],["Rich Text Editor","Yes","Multiple fonts, sizing, colors"],["Handwriting & Shapes","Yes","Stylus smoothing, lasso selection"],["Paper Backgrounds","Yes","Ruled, Grid, Dotted, Cornell, Custom"],["Voice Memo Recording","Yes","Local audio capture with playback"],["Firebase Cloud Sync","Yes","Automatic delta sync when online"]]}""",
                    strokesJson = "[]"
                )
                pageDao.insertPage(welcomePage)
            }
        }
    }
}
