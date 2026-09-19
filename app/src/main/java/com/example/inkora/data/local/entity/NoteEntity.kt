package com.example.inkora.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Note",
    val folderId: String? = null,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val isTrash: Boolean = false,
    val colorHex: String = "#FFFDF9",
    val paperType: String = "RULED",
    val paperColorHex: String = "#FFFDF9",
    val paperLineColorHex: String = "#E2E8F0",
    val paperSpacing: Float = 28f,
    val fontId: String = "outfit",
    val fontSizeSp: Float = 16f,
    val syncState: String = "PENDING_UPLOAD",
    val syncTimestamp: Long = 0L,
    val pageCount: Int = 1,
    val previewSnippet: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)
