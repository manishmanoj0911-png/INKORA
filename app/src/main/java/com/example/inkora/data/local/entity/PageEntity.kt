package com.example.inkora.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"]), Index(value = ["noteId", "pageIndex"], unique = true)]
)
data class PageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val pageIndex: Int = 0,
    val bodyText: String = "",
    val checklistJson: String = "[]",
    val tableJson: String = "",
    val strokesJson: String = "[]",
    val backgroundStyleJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)
