package com.example.inkora.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val type: String, // IMAGE, AUDIO, PDF, DRAWING
    val localPath: String,
    val remoteUrl: String? = null,
    val name: String,
    val sizeBytes: Long = 0L,
    val durationSeconds: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "page_templates")
data class PageTemplateEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val paperType: String = "RULED",
    val paperColorHex: String = "#FFFDF9",
    val paperLineColorHex: String = "#E2E8F0",
    val paperSpacing: Float = 28f,
    val fontId: String = "outfit",
    val fontSizeSp: Float = 16f,
    val isDefault: Boolean = false
)
